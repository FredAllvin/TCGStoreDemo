package com.tcgstore.shop.service.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Picks the active payment provider from the payments.provider property. */
@Service
public class PaymentService {

	private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

	private final Map<String, PaymentProvider> providers;
	private final String configured;

	public PaymentService(List<PaymentProvider> providerList, @Value("${payments.provider}") String configured) {
		this.providers = providerList.stream()
				.collect(Collectors.toMap(PaymentProvider::name, Function.identity()));
		this.configured = configured;
	}

	public PaymentProvider active() {
		PaymentProvider provider = providers.get(configured);
		if (provider == null) {
			log.warn("Unknown payments.provider '{}', falling back to mock", configured);
			provider = providers.get("mock");
		}
		return provider;
	}
}
