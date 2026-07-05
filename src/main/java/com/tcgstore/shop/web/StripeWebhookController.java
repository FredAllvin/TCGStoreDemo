package com.tcgstore.shop.web;

import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.tcgstore.shop.repo.OrderRepository;
import com.tcgstore.shop.repo.ProcessedWebhookEventRepository;
import com.tcgstore.shop.domain.ProcessedWebhookEvent;
import com.tcgstore.shop.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives Stripe webhooks. Authenticity comes from the signature check
 * (this endpoint is excluded from CSRF), and every event id is recorded so
 * Stripe's retries and replays are no-ops. Local testing:
 * stripe listen --forward-to localhost:8080/webhooks/stripe
 */
@RestController
public class StripeWebhookController {

	private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

	private final String webhookSecret;
	private final OrderRepository orders;
	private final OrderService orderService;
	private final ProcessedWebhookEventRepository processedEvents;

	public StripeWebhookController(@Value("${stripe.webhook-secret}") String webhookSecret,
			OrderRepository orders, OrderService orderService, ProcessedWebhookEventRepository processedEvents) {
		this.webhookSecret = webhookSecret == null ? "" : webhookSecret.trim();
		this.orders = orders;
		this.orderService = orderService;
		this.processedEvents = processedEvents;
	}

	@PostMapping("/webhooks/stripe")
	@Transactional
	public ResponseEntity<String> handle(@RequestBody String payload,
			@RequestHeader(value = "Stripe-Signature", required = false) String signature) {
		if (webhookSecret.isBlank()) {
			return ResponseEntity.status(503).body("webhook secret not configured");
		}
		if (signature == null) {
			return ResponseEntity.badRequest().body("missing signature");
		}

		Event event;
		try {
			event = Webhook.constructEvent(payload, signature, webhookSecret);
		}
		catch (SignatureVerificationException e) {
			log.warn("Stripe webhook with invalid signature rejected");
			return ResponseEntity.badRequest().body("invalid signature");
		}

		if (processedEvents.existsByEventId(event.getId())) {
			return ResponseEntity.ok("already processed");
		}

		switch (event.getType()) {
			case "checkout.session.completed", "checkout.session.async_payment_succeeded" -> {
				Session session = deserializeSession(event);
				// async payment methods (e.g. Klarna) send "completed" with
				// payment_status=unpaid first — only mark paid when it is paid
				if (session != null && "paid".equals(session.getPaymentStatus())) {
					orders.findByOrderNumber(session.getClientReferenceId()).ifPresent(order ->
							orderService.markPaid(order,
									session.getPaymentIntent() != null ? session.getPaymentIntent() : session.getId()));
				}
			}
			case "checkout.session.expired", "checkout.session.async_payment_failed" -> {
				Session session = deserializeSession(event);
				if (session != null) {
					orders.findByOrderNumber(session.getClientReferenceId()).ifPresent(orderService::cancel);
				}
			}
			default -> log.debug("Ignoring Stripe event type {}", event.getType());
		}

		// Unique index on event_id; a concurrent duplicate delivery makes this
		// commit fail with a 500 and Stripe simply retries into the guard above.
		processedEvents.save(new ProcessedWebhookEvent(event.getId()));
		return ResponseEntity.ok("ok");
	}

	private Session deserializeSession(Event event) {
		var deserializer = event.getDataObjectDeserializer();
		StripeObject object = deserializer.getObject().orElse(null);
		if (object == null) {
			try {
				object = deserializer.deserializeUnsafe();
			}
			catch (EventDataObjectDeserializationException e) {
				log.error("Could not deserialize Stripe event {}: {}", event.getId(), e.getMessage());
				return null;
			}
		}
		return object instanceof Session session ? session : null;
	}
}
