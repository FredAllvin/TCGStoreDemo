package com.tcgstore.shop.web;

import com.tcgstore.shop.service.CatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

	private final CatalogService catalog;

	public HomeController(CatalogService catalog) {
		this.catalog = catalog;
	}

	@GetMapping("/")
	public String home(Model model) {
		model.addAttribute("featured", catalog.featured());
		model.addAttribute("recent", catalog.recent());
		return "shop/home";
	}
}
