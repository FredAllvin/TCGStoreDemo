package com.tcgstore.shop.web.admin;

import com.tcgstore.shop.domain.Category;
import com.tcgstore.shop.domain.Condition;
import com.tcgstore.shop.domain.Finish;
import com.tcgstore.shop.domain.Grader;
import com.tcgstore.shop.domain.Product;
import com.tcgstore.shop.domain.ProductVariant;
import com.tcgstore.shop.domain.VariantKind;
import com.tcgstore.shop.repo.CategoryRepository;
import com.tcgstore.shop.repo.ProductRepository;
import com.tcgstore.shop.service.AdminCatalogService;
import com.tcgstore.shop.service.FormatService;
import com.tcgstore.shop.service.InvalidImageException;
import com.tcgstore.shop.web.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {

	private final ProductRepository products;
	private final CategoryRepository categories;
	private final AdminCatalogService catalog;

	public AdminProductController(ProductRepository products, CategoryRepository categories,
			AdminCatalogService catalog) {
		this.products = products;
		this.categories = categories;
		this.catalog = catalog;
	}

	@GetMapping
	public String list(@RequestParam(required = false) String q,
			@RequestParam(defaultValue = "0") int page, Model model) {
		Pageable pageable = PageRequest.of(Math.max(0, page), 20, Sort.by("createdAt").descending());
		Page<Product> result = (q == null || q.isBlank())
				? products.findAll(pageable)
				: products.findByNameContainingIgnoreCase(q.trim(), pageable);
		model.addAttribute("products", result);
		model.addAttribute("q", q == null ? "" : q);
		return "admin/products";
	}

	@GetMapping("/new")
	public String createForm(Model model) {
		model.addAttribute("product", new Product());
		model.addAttribute("allCategories", categories.findAllByOrderBySortOrderAscNameAsc());
		model.addAttribute("isNew", true);
		return "admin/product-form";
	}

	@PostMapping
	public String create(@RequestParam String name, @RequestParam Long categoryId,
			@RequestParam(required = false) String setName, @RequestParam(required = false) String cardNumber,
			@RequestParam(required = false) String rarity, @RequestParam(required = false) String language,
			@RequestParam(required = false) String tags,
			@RequestParam(required = false) String description,
			@RequestParam(required = false) String descriptionEn,
			@RequestParam(defaultValue = "false") boolean featured,
			@RequestParam(required = false) List<MultipartFile> files,
			RedirectAttributes redirect) {
		if (name == null || name.isBlank()) {
			redirect.addFlashAttribute("flashErrorKey", "admin.products.nameRequired");
			return "redirect:/admin/products/new";
		}
		if (anyFieldTooLong(name, setName, cardNumber, rarity, language, tags, description, descriptionEn)) {
			redirect.addFlashAttribute("flashErrorKey", "admin.products.tooLong");
			return "redirect:/admin/products/new";
		}
		Category category = categories.findById(categoryId).orElse(null);
		if (category == null) {
			redirect.addFlashAttribute("flashErrorKey", "invalid");
			return "redirect:/admin/products/new";
		}
		Product product = new Product();
		applyFields(product, category, name, setName, cardNumber, rarity, language, tags, description, descriptionEn,
				featured, true);
		Product saved = catalog.createProduct(product);
		try {
			if (files != null) {
				catalog.addImages(saved.getId(), files);
			}
			redirect.addFlashAttribute("flashSuccessKey", "admin.saved");
		}
		catch (InvalidImageException e) {
			redirect.addFlashAttribute("flashErrorKey", "admin.images.invalid");
		}
		return "redirect:/admin/products/" + saved.getId() + "/edit";
	}

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, Model model) {
		Product product = products.findById(id).orElseThrow(NotFoundException::new);
		model.addAttribute("product", product);
		model.addAttribute("allCategories", categories.findAllByOrderBySortOrderAscNameAsc());
		model.addAttribute("isNew", false);
		return "admin/product-form";
	}

	@PostMapping("/{id}")
	@Transactional
	public String update(@PathVariable Long id, @RequestParam String name, @RequestParam Long categoryId,
			@RequestParam(required = false) String setName, @RequestParam(required = false) String cardNumber,
			@RequestParam(required = false) String rarity, @RequestParam(required = false) String language,
			@RequestParam(required = false) String tags,
			@RequestParam(required = false) String description,
			@RequestParam(required = false) String descriptionEn,
			@RequestParam(defaultValue = "false") boolean featured,
			@RequestParam(defaultValue = "false") boolean active,
			RedirectAttributes redirect) {
		if (anyFieldTooLong(name, setName, cardNumber, rarity, language, tags, description, descriptionEn)) {
			redirect.addFlashAttribute("flashErrorKey", "admin.products.tooLong");
			return "redirect:/admin/products/" + id + "/edit";
		}
		Product product = products.findById(id).orElseThrow(NotFoundException::new);
		Category category = categories.findById(categoryId).orElse(product.getCategory());
		applyFields(product, category, name, setName, cardNumber, rarity, language, tags, description, descriptionEn,
				featured, active);
		products.save(product);
		redirect.addFlashAttribute("flashSuccessKey", "admin.saved");
		return "redirect:/admin/products/" + id + "/edit";
	}

	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id, RedirectAttributes redirect) {
		catalog.deleteProduct(id);
		redirect.addFlashAttribute("flashSuccessKey", "admin.deleted");
		return "redirect:/admin/products";
	}

	// --- variants ---

	@PostMapping("/{id}/variants")
	public String addVariant(@PathVariable Long id,
			@RequestParam VariantKind kind,
			@RequestParam(required = false) Condition condition,
			@RequestParam(required = false) Finish finish,
			@RequestParam(required = false) Grader grader,
			@RequestParam(required = false) String grade,
			@RequestParam(required = false) String certNumber,
			@RequestParam String price,
			@RequestParam(defaultValue = "0") int stockQty,
			RedirectAttributes redirect) {
		long priceMinor;
		try {
			priceMinor = FormatService.parseMinor(price);
			if (priceMinor < 0) {
				throw new NumberFormatException();
			}
		}
		catch (RuntimeException e) {
			redirect.addFlashAttribute("flashErrorKey", "admin.variants.badPrice");
			return "redirect:/admin/products/" + id + "/edit";
		}
		if (kind == VariantKind.RAW && condition == null) {
			redirect.addFlashAttribute("flashErrorKey", "admin.variants.conditionRequired");
			return "redirect:/admin/products/" + id + "/edit";
		}
		if (kind == VariantKind.GRADED && (grader == null || grade == null || grade.isBlank())) {
			redirect.addFlashAttribute("flashErrorKey", "admin.variants.gradeRequired");
			return "redirect:/admin/products/" + id + "/edit";
		}
		ProductVariant variant = new ProductVariant();
		variant.setKind(kind);
		if (kind == VariantKind.RAW) {
			variant.setCondition(condition);
			variant.setFinish(finish == null ? Finish.NONFOIL : finish);
		}
		if (kind == VariantKind.GRADED) {
			variant.setGrader(grader);
			variant.setGrade(grade.trim());
			variant.setCertNumber(certNumber == null || certNumber.isBlank() ? null : certNumber.trim());
		}
		variant.setPriceMinor(priceMinor);
		variant.setStockQty(Math.max(0, stockQty));
		catalog.addVariant(id, variant);
		redirect.addFlashAttribute("flashSuccessKey", "admin.saved");
		return "redirect:/admin/products/" + id + "/edit";
	}

	@PostMapping("/variants/{variantId}")
	public String updateVariant(@PathVariable Long variantId,
			@RequestParam String price,
			@RequestParam int stockQty,
			@RequestParam(defaultValue = "false") boolean active,
			@RequestParam Long productId,
			RedirectAttributes redirect) {
		try {
			catalog.updateVariant(variantId, FormatService.parseMinor(price), stockQty, active);
			redirect.addFlashAttribute("flashSuccessKey", "admin.saved");
		}
		catch (RuntimeException e) {
			redirect.addFlashAttribute("flashErrorKey", "admin.variants.badPrice");
		}
		return "redirect:/admin/products/" + productId + "/edit";
	}

	@PostMapping("/variants/{variantId}/delete")
	public String deleteVariant(@PathVariable Long variantId, RedirectAttributes redirect) {
		Long productId = catalog.deleteVariant(variantId);
		redirect.addFlashAttribute("flashSuccessKey", "admin.deleted");
		return "redirect:/admin/products/" + productId + "/edit";
	}

	// --- images ---

	@PostMapping("/{id}/images")
	public String uploadImages(@PathVariable Long id, @RequestParam("files") List<MultipartFile> files,
			RedirectAttributes redirect) {
		try {
			catalog.addImages(id, files);
			redirect.addFlashAttribute("flashSuccessKey", "admin.saved");
		}
		catch (InvalidImageException e) {
			redirect.addFlashAttribute("flashErrorKey", "admin.images.invalid");
		}
		return "redirect:/admin/products/" + id + "/edit";
	}

	@PostMapping("/images/{imageId}/delete")
	public String deleteImage(@PathVariable Long imageId, RedirectAttributes redirect) {
		Long productId = catalog.deleteImage(imageId);
		redirect.addFlashAttribute("flashSuccessKey", "admin.deleted");
		return "redirect:/admin/products/" + productId + "/edit";
	}

	private void applyFields(Product product, Category category, String name, String setName, String cardNumber,
			String rarity, String language, String tags, String description, String descriptionEn, boolean featured,
			boolean active) {
		product.setCategory(category);
		product.setName(name.trim());
		product.setSetName(trimToNull(setName));
		product.setCardNumber(trimToNull(cardNumber));
		product.setRarity(trimToNull(rarity));
		product.setLanguage(trimToNull(language));
		product.setTags(trimToNull(tags));
		product.setDescription(trimToNull(description));
		product.setDescriptionEn(trimToNull(descriptionEn));
		product.setFeatured(featured);
		product.setActive(active);
	}

	// Same limits as the form's maxlength attributes and the database columns.
	private static boolean anyFieldTooLong(String name, String setName, String cardNumber, String rarity,
			String language, String tags, String description, String descriptionEn) {
		return tooLong(name, 200) || tooLong(setName, 200) || tooLong(cardNumber, 50)
				|| tooLong(rarity, 80) || tooLong(language, 40) || tooLong(tags, 200)
				|| tooLong(description, 4000) || tooLong(descriptionEn, 4000);
	}

	private static boolean tooLong(String value, int max) {
		return value != null && value.length() > max;
	}

	private static String trimToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
