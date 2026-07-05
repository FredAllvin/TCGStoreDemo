package com.tcgstore.shop.service;

import com.tcgstore.shop.domain.Category;
import com.tcgstore.shop.domain.Product;
import com.tcgstore.shop.domain.ProductImage;
import com.tcgstore.shop.domain.ProductVariant;
import com.tcgstore.shop.repo.CategoryRepository;
import com.tcgstore.shop.repo.ProductImageRepository;
import com.tcgstore.shop.repo.ProductRepository;
import com.tcgstore.shop.repo.ProductVariantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** All catalog mutations from the admin panel live here, in one transactional place. */
@Service
public class AdminCatalogService {

	private final ProductRepository products;
	private final CategoryRepository categories;
	private final ProductVariantRepository variants;
	private final ProductImageRepository images;
	private final ImageService imageService;

	public AdminCatalogService(ProductRepository products, CategoryRepository categories,
			ProductVariantRepository variants, ProductImageRepository images, ImageService imageService) {
		this.products = products;
		this.categories = categories;
		this.variants = variants;
		this.images = images;
		this.imageService = imageService;
	}

	// --- products ---

	@Transactional
	public Product createProduct(Product product) {
		String base = Slugs.slugify(product.getCardNumber() != null
				? product.getName() + "-" + product.getCardNumber()
				: product.getName());
		String slug = base.isBlank() ? "produkt" : base;
		int i = 2;
		while (products.existsBySlug(slug)) {
			slug = base + "-" + i++;
		}
		product.setSlug(slug);
		return products.save(product);
	}

	@Transactional
	public void deleteProduct(Long productId) {
		products.findById(productId).ifPresent(product -> {
			product.getImages().forEach(image -> imageService.delete(image.getPath()));
			products.delete(product);
		});
	}

	// --- variants ---

	@Transactional
	public void addVariant(Long productId, ProductVariant variant) {
		Product product = products.findById(productId).orElseThrow();
		product.addVariant(variant);
		products.save(product);
	}

	@Transactional
	public void updateVariant(Long variantId, long priceMinor, int stockQty, boolean active) {
		ProductVariant variant = variants.findById(variantId).orElseThrow();
		variant.setPriceMinor(priceMinor);
		variant.setStockQty(Math.max(0, stockQty));
		variant.setActive(active);
		variants.save(variant);
	}

	/** @return the product id the variant belonged to */
	@Transactional
	public Long deleteVariant(Long variantId) {
		ProductVariant variant = variants.findById(variantId).orElseThrow();
		Product product = variant.getProduct();
		product.getVariants().remove(variant);
		products.save(product);
		return product.getId();
	}

	// --- images ---

	@Transactional
	public void addImages(Long productId, List<MultipartFile> files) {
		Product product = products.findById(productId).orElseThrow();
		int sort = product.getImages().size();
		for (MultipartFile file : files) {
			if (file == null || file.isEmpty()) {
				continue;
			}
			String path = imageService.store(file, "products");
			ProductImage image = new ProductImage();
			image.setPath(path);
			image.setAlt(product.getName());
			image.setSortOrder(sort++);
			product.addImage(image);
		}
		products.save(product);
	}

	/** @return the product id the image belonged to */
	@Transactional
	public Long deleteImage(Long imageId) {
		ProductImage image = images.findById(imageId).orElseThrow();
		Product product = image.getProduct();
		product.getImages().remove(image);
		products.save(product);
		imageService.delete(image.getPath());
		return product.getId();
	}

	// --- categories ---

	@Transactional
	public void createCategory(String name, Long parentId, int sortOrder) {
		Category category = new Category();
		category.setName(name.trim());
		category.setSortOrder(sortOrder);
		if (parentId != null) {
			category.setParent(categories.findById(parentId).orElse(null));
		}
		String parentPrefix = category.getParent() != null ? category.getParent().getName() + "-" : "";
		String base = Slugs.slugify(parentPrefix + name);
		String slug = base.isBlank() ? "kategori" : base;
		int i = 2;
		while (categories.existsBySlug(slug)) {
			slug = base + "-" + i++;
		}
		category.setSlug(slug);
		categories.save(category);
	}

	@Transactional
	public void updateCategory(Long id, String name, int sortOrder) {
		Category category = categories.findById(id).orElseThrow();
		category.setName(name.trim());
		category.setSortOrder(sortOrder);
		categories.save(category);
	}

	/** @return false if the category still has products or children */
	@Transactional
	public boolean deleteCategory(Long id) {
		Category category = categories.findById(id).orElseThrow();
		if (products.countByCategoryId(id) > 0 || categories.countByParent(category) > 0) {
			return false;
		}
		categories.delete(category);
		return true;
	}
}
