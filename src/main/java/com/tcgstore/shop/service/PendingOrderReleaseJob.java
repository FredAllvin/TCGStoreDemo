package com.tcgstore.shop.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** Cancels unpaid orders after an hour so reserved cards go back on sale. */
@Component
public class PendingOrderReleaseJob {

	private static final Logger log = LoggerFactory.getLogger(PendingOrderReleaseJob.class);

	// Must stay well above StripeCheckoutProvider.SESSION_LIFETIME (30 min):
	// the payment session has to be dead before we cancel and restock.
	private static final Duration MAX_PENDING_AGE = Duration.ofMinutes(60);

	private final OrderService orderService;

	public PendingOrderReleaseJob(OrderService orderService) {
		this.orderService = orderService;
	}

	@Scheduled(fixedDelay = 300_000, initialDelay = 120_000)
	public void releaseExpiredOrders() {
		int released = orderService.releaseExpiredPending(MAX_PENDING_AGE);
		if (released > 0) {
			log.info("Released {} expired pending order(s)", released);
		}
	}
}
