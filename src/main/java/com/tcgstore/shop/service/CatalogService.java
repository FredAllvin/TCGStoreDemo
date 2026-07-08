package com.tcgstore.shop.service;

import com.tcgstore.shop.domain.Category;
import com.tcgstore.shop.domain.Product;
import com.tcgstore.shop.repo.CategoryRepository;
import com.tcgstore.shop.repo.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CatalogService {

	private final ProductRepository productRepository;
	private final CategoryRepository categoryRepository;

	public CatalogService(ProductRepository productRepository, CategoryRepository categoryRepository) {
		this.productRepository = productRepository;
		this.categoryRepository = categoryRepository;
	}

	public List<Product> featured() {
		return productRepository.findTop8ByActiveTrueAndFeaturedTrueOrderByCreatedAtDesc();
	}

	public List<Product> recent() {
		return productRepository.findTop8ByActiveTrueOrderByCreatedAtDesc();
	}

	public Optional<Category> categoryBySlug(String slug) {
		return categoryRepository.findBySlug(slug);
	}

	public Optional<Product> productBySlug(String slug) {
		return productRepository.findBySlugAndActiveTrue(slug);
	}

	/** Products in a category and all of its children. */
	public Page<Product> byCategory(Category category, boolean inStockOnly, Pageable pageable) {
		List<Long> ids = new ArrayList<>();
		ids.add(category.getId());
		category.getChildren().forEach(child -> ids.add(child.getId()));
		return inStockOnly
				? productRepository.findInStockByCategoryIdIn(ids, pageable)
				: productRepository.findByActiveTrueAndCategoryIdIn(ids, pageable);
	}

	public Page<Product> search(String query, Pageable pageable) {
		String q = query.trim();
		return productRepository.search(q, likePattern(q), pageable);
	}

	/** Escapes LIKE wildcards so a search for "100%" or "_" matches literally. */
	static String likePattern(String q) {
		return "%" + q.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
	}
}
