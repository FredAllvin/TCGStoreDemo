package com.tcgstore.shop.service;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Session-scoped cart. Deliberately stores only variant ids and quantities —
 * prices, names and stock are always re-read from the database so nothing the
 * browser sends can influence what is charged.
 */
@Component
@SessionScope
public class Cart implements Serializable {

	private final LinkedHashMap<Long, Integer> items = new LinkedHashMap<>();

	public void add(long variantId, int qty) {
		items.merge(variantId, Math.max(1, qty), Integer::sum);
	}

	public void setQty(long variantId, int qty) {
		if (qty <= 0) {
			items.remove(variantId);
		}
		else {
			items.put(variantId, qty);
		}
	}

	public void remove(long variantId) {
		items.remove(variantId);
	}

	public void clear() {
		items.clear();
	}

	public Map<Long, Integer> getItems() {
		return Collections.unmodifiableMap(items);
	}

	public int itemCount() {
		return items.values().stream().mapToInt(Integer::intValue).sum();
	}

	public boolean isEmpty() {
		return items.isEmpty();
	}
}
