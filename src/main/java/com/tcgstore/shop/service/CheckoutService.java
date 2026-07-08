package com.tcgstore.shop.service;

import com.tcgstore.shop.domain.Order;
import com.tcgstore.shop.domain.OrderLine;
import com.tcgstore.shop.domain.Product;
import com.tcgstore.shop.domain.ShippingOption;
import com.tcgstore.shop.repo.OrderRepository;
import com.tcgstore.shop.repo.ProductVariantRepository;
import com.tcgstore.shop.repo.ShippingOptionRepository;
import com.tcgstore.shop.service.CartService.CartLine;
import com.tcgstore.shop.service.CartService.CartView;
import com.tcgstore.shop.web.dto.CheckoutForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class CheckoutService {

	private final CartService cartService;
	private final Cart cart;
	private final ProductVariantRepository variants;
	private final ShippingOptionRepository shippingOptions;
	private final OrderRepository orders;
	private final SettingsService settingsService;

	public CheckoutService(CartService cartService, Cart cart, ProductVariantRepository variants,
			ShippingOptionRepository shippingOptions, OrderRepository orders, SettingsService settingsService) {
		this.cartService = cartService;
		this.cart = cart;
		this.variants = variants;
		this.shippingOptions = shippingOptions;
		this.orders = orders;
		this.settingsService = settingsService;
	}

	public long shippingCost(ShippingOption option, long subtotal) {
		Long threshold = settingsService.get().getFreeShipThresholdMinor();
		return threshold != null && subtotal >= threshold ? 0 : option.getPriceMinor();
	}

	/**
	 * Creates a PENDING order and atomically reserves stock for every line.
	 * If any single line cannot be reserved the whole transaction rolls back,
	 * so a one-of-one graded card can never be sold twice.
	 */
	@Transactional
	public Order placeOrder(CheckoutForm form, String paymentProvider) {
		CartView view = cartService.view();
		if (view.isEmpty()) {
			throw new OutOfStockException();
		}
		ShippingOption shipping = shippingOptions.findById(form.getShippingOptionId())
				.filter(ShippingOption::isActive)
				.orElseThrow(IllegalArgumentException::new);

		// Reserve in a stable order to avoid deadlocks between concurrent checkouts.
		List<CartLine> sorted = view.lines().stream()
				.sorted(Comparator.comparingLong(line -> line.variant().getId()))
				.toList();
		for (CartLine line : sorted) {
			if (variants.reserveStock(line.variant().getId(), line.qty()) == 0) {
				throw new OutOfStockException();
			}
		}

		long shippingCost = shippingCost(shipping, view.subtotal());

		Order order = new Order();
		order.setOrderNumber("%d-%04d".formatted(
				Year.now(ZoneId.of("Europe/Stockholm")).getValue(), orders.nextOrderSequence()));
		order.setAccessToken(UUID.randomUUID().toString());
		order.setCustomerName(form.getName().trim());
		order.setEmail(form.getEmail().trim());
		order.setPhone(blankToNull(form.getPhone()));
		order.setAddressLine1(form.getAddressLine1().trim());
		order.setAddressLine2(blankToNull(form.getAddressLine2()));
		order.setPostalCode(form.getPostalCode().trim());
		order.setCity(form.getCity().trim());
		order.setCountry(form.getCountry().trim());
		order.setCustomerNote(blankToNull(form.getNote()));
		order.setShippingName(shipping.getName());
		order.setShippingNameEn(shipping.getNameEn());
		order.setShippingMinor(shippingCost);
		order.setSubtotalMinor(view.subtotal());
		order.setTotalMinor(view.subtotal() + shippingCost);
		order.setCurrency(settingsService.get().getCurrency());
		order.setPaymentProvider(paymentProvider);

		for (CartLine line : view.lines()) {
			OrderLine orderLine = new OrderLine();
			orderLine.setVariantId(line.variant().getId());
			Product product = line.product();
			orderLine.setProductName(product.getSetName() != null
					? product.getName() + " – " + product.getSetName()
					: product.getName());
			orderLine.setVariantLabel(line.variant().displayLabel());
			orderLine.setUnitPriceMinor(line.variant().getPriceMinor());
			orderLine.setQty(line.qty());
			orderLine.setLineTotalMinor(line.lineTotal());
			order.addLine(orderLine);
		}

		Order saved = orders.save(order);
		cart.clear();
		return saved;
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
