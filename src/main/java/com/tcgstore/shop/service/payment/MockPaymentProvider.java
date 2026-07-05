package com.tcgstore.shop.service.payment;

import com.tcgstore.shop.domain.Order;
import org.springframework.stereotype.Component;

/** Fake payment page for demos — looks like a checkout, charges nothing. */
@Component
public class MockPaymentProvider implements PaymentProvider {

	@Override
	public String name() {
		return "mock";
	}

	@Override
	public String startPayment(Order order) {
		return "/pay/mock/" + order.getOrderNumber() + "?t=" + order.getAccessToken();
	}
}
