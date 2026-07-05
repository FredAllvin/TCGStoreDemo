package com.tcgstore.shop.web;

import com.tcgstore.shop.service.CartService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CartController {

	private static final String SLUG_PATTERN = "[a-z0-9-]{1,220}";

	private final CartService cartService;

	public CartController(CartService cartService) {
		this.cartService = cartService;
	}

	@GetMapping("/cart")
	public String cart(Model model) {
		model.addAttribute("cart", cartService.view());
		return "shop/cart";
	}

	@PostMapping("/cart/add")
	public String add(@RequestParam long variantId,
			@RequestParam(defaultValue = "1") int qty,
			@RequestParam(required = false) String back,
			RedirectAttributes redirect) {
		boolean added = cartService.add(variantId, qty);
		if (added) {
			redirect.addFlashAttribute("flashSuccessKey", "cart.added");
		}
		else {
			redirect.addFlashAttribute("flashErrorKey", "product.outOfStock");
		}
		// only redirect back to a well-formed product slug on our own site
		if (back != null && back.matches(SLUG_PATTERN)) {
			return "redirect:/p/" + back;
		}
		return "redirect:/cart";
	}

	@PostMapping("/cart/update")
	public String update(@RequestParam long variantId, @RequestParam int qty) {
		cartService.setQty(variantId, Math.max(0, qty));
		return "redirect:/cart";
	}

	@PostMapping("/cart/remove")
	public String remove(@RequestParam long variantId) {
		cartService.remove(variantId);
		return "redirect:/cart";
	}
}
