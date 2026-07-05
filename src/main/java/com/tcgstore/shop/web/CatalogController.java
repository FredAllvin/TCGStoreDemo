package com.tcgstore.shop.web;

import com.tcgstore.shop.domain.Category;
import com.tcgstore.shop.domain.Product;
import com.tcgstore.shop.service.CatalogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CatalogController {

	private static final int PAGE_SIZE = 24;

	private final CatalogService catalog;

	public CatalogController(CatalogService catalog) {
		this.catalog = catalog;
	}

	@GetMapping("/c/{slug}")
	public String category(@PathVariable String slug,
			@RequestParam(defaultValue = "false") boolean inStock,
			@RequestParam(defaultValue = "newest") String sort,
			@RequestParam(defaultValue = "0") int page,
			Model model) {
		Category category = catalog.categoryBySlug(slug).orElseThrow(NotFoundException::new);
		Sort order = "name".equals(sort) ? Sort.by("name").ascending() : Sort.by("createdAt").descending();
		Page<Product> products = catalog.byCategory(category, inStock,
				PageRequest.of(Math.max(0, page), PAGE_SIZE, order));
		model.addAttribute("category", category);
		model.addAttribute("products", products);
		model.addAttribute("inStock", inStock);
		model.addAttribute("sort", sort);
		return "shop/category";
	}

	@GetMapping("/p/{slug}")
	public String product(@PathVariable String slug, Model model) {
		Product product = catalog.productBySlug(slug).orElseThrow(NotFoundException::new);
		model.addAttribute("product", product);
		return "shop/product";
	}

	@GetMapping("/search")
	public String search(@RequestParam(required = false) String q,
			@RequestParam(defaultValue = "0") int page,
			Model model) {
		if (q == null || q.trim().length() < 2) {
			return "redirect:/";
		}
		Page<Product> products = catalog.search(q,
				PageRequest.of(Math.max(0, page), PAGE_SIZE, Sort.by("createdAt").descending()));
		model.addAttribute("q", q.trim());
		model.addAttribute("products", products);
		return "shop/search";
	}
}
