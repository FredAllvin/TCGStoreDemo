package com.tcgstore.shop.config;

import com.tcgstore.shop.repo.CategoryRepository;
import com.tcgstore.shop.service.Cart;
import com.tcgstore.shop.service.SettingsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Adds the attributes every page needs (branding, nav, cart badge) so
 * individual controllers stay focused on their own data. Scoped to our web
 * package so Boot's error controller renders without these dependencies.
 */
@ControllerAdvice(basePackages = "com.tcgstore.shop.web")
public class GlobalModelAdvice {

	private final SettingsService settingsService;
	private final CategoryRepository categoryRepository;
	private final Cart cart;
	private final Environment environment;

	public GlobalModelAdvice(SettingsService settingsService, CategoryRepository categoryRepository, Cart cart,
			Environment environment) {
		this.settingsService = settingsService;
		this.categoryRepository = categoryRepository;
		this.cart = cart;
		this.environment = environment;
	}

	@ModelAttribute
	public void globalAttributes(Model model, HttpServletRequest request) {
		model.addAttribute("settings", settingsService.get());
		model.addAttribute("navCategories", categoryRepository.findByParentIsNullOrderBySortOrderAscNameAsc());
		model.addAttribute("cartCount", cart.itemCount());
		model.addAttribute("demoMode", environment.acceptsProfiles(Profiles.of("demo")));
		model.addAttribute("currentPath", request.getRequestURI());
		model.addAttribute("currentUrl", currentUrl(request));
	}

	/**
	 * The current URI including its query string (minus any lang parameter), so
	 * the language switcher keeps search terms, filters and order tokens intact.
	 */
	private static String currentUrl(HttpServletRequest request) {
		String query = request.getQueryString();
		if (query == null || query.isBlank()) {
			return request.getRequestURI();
		}
		String withoutLang = Arrays.stream(query.split("&"))
				.filter(param -> !param.startsWith("lang="))
				.collect(Collectors.joining("&"));
		return withoutLang.isBlank() ? request.getRequestURI() : request.getRequestURI() + "?" + withoutLang;
	}
}
