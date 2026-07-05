package com.tcgstore.shop.service.payment;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.tcgstore.shop.config.AppProperties;
import com.tcgstore.shop.domain.Order;
import com.tcgstore.shop.domain.OrderLine;
import com.tcgstore.shop.repo.OrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Stripe Checkout: creates a hosted checkout session and redirects the
 * customer to Stripe. Card data never touches this server. The order is
 * marked paid by the webhook, not by the success redirect.
 *
 * Test mode: use sk_test_… keys and card 4242 4242 4242 4242.
 */
@Component
public class StripeCheckoutProvider implements PaymentProvider {

	private final AppProperties props;
	private final OrderRepository orders;
	private final String secretKey;

	public StripeCheckoutProvider(AppProperties props, OrderRepository orders,
			@Value("${stripe.secret-key}") String secretKey) {
		this.props = props;
		this.orders = orders;
		this.secretKey = secretKey == null ? "" : secretKey.trim();
		if (!this.secretKey.isBlank()) {
			Stripe.apiKey = this.secretKey;
		}
	}

	@Override
	public String name() {
		return "stripe";
	}

	@Override
	public String startPayment(Order order) {
		if (secretKey.isBlank()) {
			throw new PaymentException("payments.provider=stripe but STRIPE_SECRET_KEY is not set");
		}
		String confirmationUrl = props.baseUrl() + "/order/" + order.getOrderNumber() + "?t=" + order.getAccessToken();
		SessionCreateParams.Builder params = SessionCreateParams.builder()
				.setMode(SessionCreateParams.Mode.PAYMENT)
				.setClientReferenceId(order.getOrderNumber())
				.setCustomerEmail(order.getEmail())
				.setSuccessUrl(confirmationUrl)
				.setCancelUrl(confirmationUrl);

		String currency = order.getCurrency().toLowerCase(Locale.ROOT);
		for (OrderLine line : order.getLines()) {
			String name = line.getVariantLabel().isEmpty()
					? line.getProductName()
					: line.getProductName() + " (" + line.getVariantLabel() + ")";
			params.addLineItem(SessionCreateParams.LineItem.builder()
					.setQuantity((long) line.getQty())
					.setPriceData(SessionCreateParams.LineItem.PriceData.builder()
							.setCurrency(currency)
							.setUnitAmount(line.getUnitPriceMinor())
							.setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
									.setName(name)
									.build())
							.build())
					.build());
		}
		if (order.getShippingMinor() > 0) {
			params.addLineItem(SessionCreateParams.LineItem.builder()
					.setQuantity(1L)
					.setPriceData(SessionCreateParams.LineItem.PriceData.builder()
							.setCurrency(currency)
							.setUnitAmount(order.getShippingMinor())
							.setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
									.setName("Frakt – " + order.getShippingName())
									.build())
							.build())
					.build());
		}

		try {
			Session session = Session.create(params.build());
			order.setPaymentRef(session.getId());
			orders.save(order);
			return session.getUrl();
		}
		catch (StripeException e) {
			throw new PaymentException("Could not create Stripe checkout session", e);
		}
	}
}
