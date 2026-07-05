package com.tcgstore.shop.service.payment;

import com.tcgstore.shop.domain.Order;

/**
 * A way to collect payment for a freshly created PENDING order.
 * Implementations return the URL the customer should be redirected to.
 */
public interface PaymentProvider {

	String name();

	String startPayment(Order order);
}
