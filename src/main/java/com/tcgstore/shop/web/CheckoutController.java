package com.tcgstore.shop.web;

import com.tcgstore.shop.domain.Order;
import com.tcgstore.shop.domain.ShippingOption;
import com.tcgstore.shop.repo.ShippingOptionRepository;
import com.tcgstore.shop.service.CartService;
import com.tcgstore.shop.service.CartService.CartView;
import com.tcgstore.shop.service.CheckoutService;
import com.tcgstore.shop.service.OutOfStockException;
import com.tcgstore.shop.service.payment.PaymentService;
import com.tcgstore.shop.web.dto.CheckoutForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class CheckoutController {

	private final CartService cartService;
	private final CheckoutService checkoutService;
	private final ShippingOptionRepository shippingOptions;
	private final PaymentService paymentService;

	public CheckoutController(CartService cartService, CheckoutService checkoutService,
			ShippingOptionRepository shippingOptions, PaymentService paymentService) {
		this.cartService = cartService;
		this.checkoutService = checkoutService;
		this.shippingOptions = shippingOptions;
		this.paymentService = paymentService;
	}

	@GetMapping("/checkout")
	public String checkout(Model model) {
		CartView cart = cartService.view();
		if (cart.isEmpty()) {
			return "redirect:/cart";
		}
		if (!model.containsAttribute("form")) {
			model.addAttribute("form", new CheckoutForm());
		}
		addCheckoutModel(model, cart);
		return "shop/checkout";
	}

	@PostMapping("/checkout")
	public String placeOrder(@Valid @ModelAttribute("form") CheckoutForm form, BindingResult binding,
			Model model, RedirectAttributes redirect) {
		CartView cart = cartService.view();
		if (cart.isEmpty()) {
			return "redirect:/cart";
		}
		if (binding.hasErrors()) {
			addCheckoutModel(model, cart);
			return "shop/checkout";
		}
		try {
			Order order = checkoutService.placeOrder(form, paymentService.active().name());
			return "redirect:" + paymentService.active().startPayment(order);
		}
		catch (OutOfStockException e) {
			redirect.addFlashAttribute("flashErrorKey", "checkout.outOfStock");
			return "redirect:/cart";
		}
		catch (IllegalArgumentException e) {
			binding.rejectValue("shippingOptionId", "invalid");
			addCheckoutModel(model, cart);
			return "shop/checkout";
		}
	}

	private void addCheckoutModel(Model model, CartView cart) {
		List<ShippingOption> options = shippingOptions.findByActiveTrueOrderBySortOrderAsc();
		Map<Long, Long> costs = new LinkedHashMap<>();
		options.forEach(option -> costs.put(option.getId(), checkoutService.shippingCost(option, cart.subtotal())));
		model.addAttribute("cart", cart);
		model.addAttribute("shippingOptions", options);
		model.addAttribute("shippingCosts", costs);
	}
}
