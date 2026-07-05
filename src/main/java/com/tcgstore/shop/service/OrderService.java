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
		Order managed = orders.findById(order.getId()).orElseThrow();
		if (managed.getStatus() != OrderStatus.PENDING) {
			return;
		}
		managed.setStatus(OrderStatus.PAID);
		managed.setPaidAt(Instant.now());
		if (paymentRef != null) {
			managed.setPaymentRef(paymentRef);
		}
		orders.save(managed);
		log.info("Order {} marked PAID ({})", managed.getOrderNumber(), paymentRef);
	}

	@Transactional
	public void markShipped(Order order) {
		Order managed = orders.findById(order.getId()).orElseThrow();
		if (managed.getStatus() != OrderStatus.PAID) {
			return;
		}
		managed.setStatus(OrderStatus.SHIPPED);
		managed.setShippedAt(Instant.now());
		orders.save(managed);
	}

	/** Cancels and puts the reserved items back in stock. */
	@Transactional
	public void cancel(Order order) {
		// re-load inside this transaction so the lazy lines collection is readable
		// no matter where the caller got the order instance from
		Order managed = orders.findById(order.getId()).orElseThrow();
		if (managed.getStatus() == OrderStatus.CANCELLED || managed.getStatus() == OrderStatus.SHIPPED) {
			return;
		}
		for (OrderLine line : managed.getLines()) {
			if (line.getVariantId() != null) {
				variants.releaseStock(line.getVariantId(), line.getQty());
			}
		}
		managed.setStatus(OrderStatus.CANCELLED);
		managed.setCancelledAt(Instant.now());
		orders.save(managed);
		log.info("Order {} cancelled, stock released", managed.getOrderNumber());
	}

	/** Frees stock held by checkouts that never completed payment. */
	@Transactional
	public int releaseExpiredPending(Duration maxAge) {
		List<Order> expired = orders.findByStatusAndCreatedAtBefore(OrderStatus.PENDING, Instant.now().minus(maxAge));
		expired.forEach(this::cancel);
		return expired.size();
	}
}
