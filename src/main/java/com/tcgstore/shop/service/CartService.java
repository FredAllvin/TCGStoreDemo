package com.tcgstore.shop.service;

import com.tcgstore.shop.domain.Product;
import com.tcgstore.shop.domain.ProductVariant;
import com.tcgstore.shop.repo.ProductVariantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns the id/qty pairs in the session {@link Cart} into a priced view,
 * clamping quantities to current stock. Prices always come from the database.
 */
@Service
public class CartService {

	public record CartLine(ProductVariant variant, Product product, int qty, long lineTotal, boolean adjusted) {
	}

	public record CartView(List<CartLine> lines, long subtotal, int itemCount, boolean adjusted) {

		public boolean isEmpty() {
			return lines.isEmpty();
		}
	}

	private final Cart cart;
	private final ProductVariantRepository variants;

	public CartService(Cart cart, ProductVariantRepository variants) {
		this.cart = cart;
		this.variants = variants;
	}

	@Transactional(readOnly = true)
	public CartView view() {
		List<CartLine> lines = new ArrayList<>();
		boolean adjusted = false;
		Map<Long, Integer> snapshot = new LinkedHashMap<>(cart.getItems());
		for (Map.Entry<Long, Integer> entry : snapshot.entrySet()) {
			ProductVariant variant = variants.findById(entry.getKey()).orElse(null);
			if (variant == null || !variant.isActive() || variant.getStockQty() <= 0) {
				cart.remove(entry.getKey());
				adjusted = true;
				continue;
			}
			int wanted = entry.getValue();
			int qty = Math.min(wanted, variant.getStockQty());
			if (qty != wanted) {
				cart.setQty(entry.getKey(), qty);
				adjusted = true;
			}
			lines.add(new CartLine(variant, variant.getProduct(), qty, variant.getPriceMinor() * qty, qty != wanted));
		}
		long subtotal = lines.stream().mapToLong(CartLine::lineTotal).sum();
		int count = lines.stream().mapToInt(CartLine::qty).sum();
		return new CartView(lines, subtotal, count, adjusted);
	}

	@Transactional(readOnly = true)
	public boolean add(long variantId, int qty) {
		ProductVariant variant = variants.findById(variantId).orElse(null);
		if (variant == null || !variant.isActive() || variant.getStockQty() <= 0) {
			return false;
		}
		int current = cart.getItems().getOrDefault(variantId, 0);
		cart.setQty(variantId, Math.min(current + Math.max(1, qty), variant.getStockQty()));
		return true;
	}

	public void setQty(long variantId, int qty) {
		cart.setQty(variantId, qty);
	}

	public void remove(long variantId) {
		cart.remove(variantId);
	}
}
