package com.tcgstore.shop.web.admin;

import com.tcgstore.shop.repo.CategoryRepository;
import com.tcgstore.shop.service.AdminCatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryController {

	private final CategoryRepository categories;
	private final AdminCatalogService catalog;

	public AdminCategoryController(CategoryRepository categories, AdminCatalogService catalog) {
		this.categories = categories;
		this.catalog = catalog;
	}

	@GetMapping
	public String list(Model model) {
		model.addAttribute("rootCategories", categories.findByParentIsNullOrderBySortOrderAscNameAsc());
		return "admin/categories";
	}

	@PostMapping
	public String create(@RequestParam String name,
			@RequestParam(required = false) Long parentId,
			@RequestParam(defaultValue = "0") int sortOrder,
			RedirectAttributes redirect) {
		if (name == null || name.isBlank()) {
			redirect.addFlashAttribute("flashErrorKey", "admin.categories.nameRequired");
		}
		else {
			catalog.createCategory(name, parentId, sortOrder);
			redirect.addFlashAttribute("flashSuccessKey", "admin.saved");
		}
		return "redirect:/admin/categories";
	}

	@PostMapping("/{id}")
	public String update(@PathVariable Long id, @RequestParam String name,
			@RequestParam(defaultValue = "0") int sortOrder, RedirectAttributes redirect) {
		if (name != null && !name.isBlank()) {
			catalog.updateCategory(id, name, sortOrder);
			redirect.addFlashAttribute("flashSuccessKey", "admin.saved");
		}
		return "redirect:/admin/categories";
	}

	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id, RedirectAttributes redirect) {
		if (catalog.deleteCategory(id)) {
			redirect.addFlashAttribute("flashSuccessKey", "admin.deleted");
		}
		else {
			redirect.addFlashAttribute("flashErrorKey", "admin.categories.notEmpty");
		}
		return "redirect:/admin/categories";
	}
}
