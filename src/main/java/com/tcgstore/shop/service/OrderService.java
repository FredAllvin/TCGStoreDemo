package com.tcgstore.shop.service;

import com.tcgstore.shop.domain.Order;
import com.tcgstore.shop.domain.OrderLine;
import com.tcgstore.shop.domain.OrderStatus;
import com.tcgstore.shop.repo.OrderRepository;
import com.tcgstore.shop.repo.ProductVariantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class OrderService {

	private static final Logger log = LoggerFactory.getLogger(OrderService.class);

	private final OrderRepository orders;
	private final ProductVariantRepository variants;

	public OrderService(OrderRepository orders, ProductVariantRepository variants) {
		this.orders = orders;
		this.variants = variants;
	}

	/** Idempotent: marking an already-paid order paid again is a no-op. */
	@Transactional
	public void markPaid(Order order, String paymentRef) {
		if (order.getStatus() != OrderStatus.PENDING) {
			return;
		}
		order.setStatus(OrderStatus.PAID);
		order.setPaidAt(Instant.now());
		if (paymentRef != null) {
			order.setPaymentRef(paymentRef);
		}
		orders.save(order);
		log.info("Order {} marked PAID ({})", order.getOrderNumber(), paymentRef);
	}

	@Transactional
	public void markShipped(Order order) {
		if (order.getStatus() != OrderStatus.PAID) {
			return;
		}
		order.setStatus(OrderStatus.SHIPPED);
		order.setShippedAt(Instant.now());
		orders.save(order);
	}

	/** Cancels and puts the reserved items back in stock. */
	@Transactional
	public void cancel(Order order) {
		if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.SHIPPED) {
			return;
		}
		for (OrderLine line : order.getLines()) {
			if (line.getVariantId() != null) {
				variants.releaseStock(line.getVariantId(), line.getQty());
			}
		}
		order.setStatus(OrderStatus.CANCELLED);
		order.setCancelledAt(Instant.now());
		orders.save(order);
		log.info("Order {} cancelled, stock released", order.getOrderNumber());
	}

	/** Frees stock held by checkouts that never completed payment. */
	@Transactional
	public int releaseExpiredPending(Duration maxAge) {
		List<Order> expired = orders.findByStatusAndCreatedAtBefore(OrderStatus.PENDING, Instant.now().minus(maxAge));
		expired.forEach(this::cancel);
		return expired.size();
	}
}
