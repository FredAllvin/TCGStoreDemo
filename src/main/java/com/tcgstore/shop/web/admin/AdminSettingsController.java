package com.tcgstore.shop.web.admin;

import com.tcgstore.shop.domain.ShippingOption;
import com.tcgstore.shop.domain.StoreSettings;
import com.tcgstore.shop.repo.ShippingOptionRepository;
import com.tcgstore.shop.service.FormatService;
import com.tcgstore.shop.service.ImageService;
import com.tcgstore.shop.service.InvalidImageException;
import com.tcgstore.shop.service.SettingsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin/settings")
public class AdminSettingsController {

	private final SettingsService settingsService;
	private final ShippingOptionRepository shippingOptions;
	private final ImageService imageService;

	public AdminSettingsController(SettingsService settingsService, ShippingOptionRepository shippingOptions,
			ImageService imageService) {
		this.settingsService = settingsService;
		this.shippingOptions = shippingOptions;
		this.imageService = imageService;
	}

	@GetMapping
	public String form(Model model) {
		model.addAttribute("shippingList", shippingOptions.findAllByOrderBySortOrderAsc());
		return "admin/settings";
	}

	@PostMapping
	public String save(@RequestParam String storeName,
			@RequestParam(required = false) String tagline,
			@RequestParam(required = false) String taglineEn,
			@RequestParam String primaryColor,
			@RequestParam String accentColor,
			@RequestParam String currency,
			@RequestParam(defaultValue = "sv") String defaultLocale,
			@RequestParam(required = false) String eurDisplayRate,
			@RequestParam(required = false) String contactEmail,
			@RequestParam(required = false) String contactPhone,
			@RequestParam(required = false) String addressLine,
			@RequestParam(required = false) String instagramUrl,
			@RequestParam(required = false) String facebookUrl,
			@RequestParam(required = false) String discordUrl,
			@RequestParam(required = false) String freeShipThreshold,
			@RequestParam(required = false) MultipartFile logo,
			@RequestParam(defaultValue = "false") boolean removeLogo,
			RedirectAttributes redirect) {
		StoreSettings settings = settingsService.get();
		if (storeName == null || storeName.isBlank()) {
			redirect.addFlashAttribute("flashErrorKey", "admin.settings.nameRequired");
			return "redirect:/admin/settings";
		}
		if (tooLong(storeName, 120) || tooLong(tagline, 200) || tooLong(taglineEn, 200) || tooLong(contactEmail, 200)
				|| tooLong(contactPhone, 50) || tooLong(addressLine, 300) || tooLong(instagramUrl, 300)
				|| tooLong(facebookUrl, 300) || tooLong(discordUrl, 300)) {
			redirect.addFlashAttribute("flashErrorKey", "admin.settings.tooLong");
			return "redirect:/admin/settings";
		}
		settings.setStoreName(storeName.trim());
		settings.setTagline(trimToNull(tagline));
		settings.setTaglineEn(trimToNull(taglineEn));
		settings.setPrimaryColor(sanitizeColor(primaryColor, "#1f2a44"));
		settings.setAccentColor(sanitizeColor(accentColor, "#e8590c"));
		settings.setCurrency(currency.matches("[A-Z]{3}") ? currency : "SEK");
		settings.setDefaultLocale(defaultLocale.matches("sv|en") ? defaultLocale : "sv");
		settings.setContactEmail(trimToNull(contactEmail));
		settings.setContactPhone(trimToNull(contactPhone));
		settings.setAddressLine(trimToNull(addressLine));
		settings.setInstagramUrl(safeUrl(instagramUrl));
		settings.setFacebookUrl(safeUrl(facebookUrl));
		settings.setDiscordUrl(safeUrl(discordUrl));

		try {
			settings.setEurDisplayRate(eurDisplayRate == null || eurDisplayRate.isBlank()
					? null : new BigDecimal(eurDisplayRate.trim().replace(',', '.')));
		}
		catch (NumberFormatException e) {
			settings.setEurDisplayRate(null);
		}
		try {
			settings.setFreeShipThresholdMinor(freeShipThreshold == null || freeShipThreshold.isBlank()
					? null : FormatService.parseMinor(freeShipThreshold));
		}
		catch (RuntimeException e) {
			settings.setFreeShipThresholdMinor(null);
		}

		if (removeLogo && settings.getLogoPath() != null) {
			imageService.delete(settings.getLogoPath());
			settings.setLogoPath(null);
		}
		boolean logoRejected = false;
		if (logo != null && !logo.isEmpty()) {
			try {
				String old = settings.getLogoPath();
				settings.setLogoPath(imageService.store(logo, "branding"));
				imageService.delete(old);
			}
			catch (InvalidImageException e) {
				logoRejected = true;
				redirect.addFlashAttribute("flashErrorKey", "admin.images.invalid");
			}
		}

		settingsService.save(settings);
		if (!logoRejected) {
			redirect.addFlashAttribute("flashSuccessKey", "admin.saved");
		}
		return "redirect:/admin/settings";
	}

	// --- shipping options ---

	@PostMapping("/shipping")
	public String addShipping(@RequestParam String name,
			@RequestParam(required = false) String nameEn,
			@RequestParam String price,
			@RequestParam(defaultValue = "0") int sortOrder, RedirectAttributes redirect) {
		if (tooLong(name, 120) || tooLong(nameEn, 120)) {
			redirect.addFlashAttribute("flashErrorKey", "admin.settings.tooLong");
			return "redirect:/admin/settings";
		}
		try {
			ShippingOption option = new ShippingOption();
			option.setName(name.trim());
			option.setNameEn(trimToNull(nameEn));
			option.setPriceMinor(FormatService.parseMinor(price));
			option.setSortOrder(sortOrder);
			shippingOptions.save(option);
			redirect.addFlashAttribute("flashSuccessKey", "admin.saved");
		}
		catch (RuntimeException e) {
			redirect.addFlashAttribute("flashErrorKey", "admin.variants.badPrice");
		}
		return "redirect:/admin/settings";
	}

	@PostMapping("/shipping/{id}/toggle")
	public String toggleShipping(@PathVariable Long id, RedirectAttributes redirect) {
		shippingOptions.findById(id).ifPresent(option -> {
			option.setActive(!option.isActive());
			shippingOptions.save(option);
		});
		redirect.addFlashAttribute("flashSuccessKey", "admin.saved");
		return "redirect:/admin/settings";
	}

	@PostMapping("/shipping/{id}/delete")
	public String deleteShipping(@PathVariable Long id, RedirectAttributes redirect) {
		shippingOptions.deleteById(id);
		redirect.addFlashAttribute("flashSuccessKey", "admin.deleted");
		return "redirect:/admin/settings";
	}

	private static String trimToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	// Same limits as the form's maxlength attributes and the database columns.
	private static boolean tooLong(String value, int max) {
		return value != null && value.length() > max;
	}

	private static String sanitizeColor(String color, String fallback) {
		return color != null && color.matches("#[0-9a-fA-F]{6}") ? color : fallback;
	}

	private static String safeUrl(String url) {
		String trimmed = trimToNull(url);
		if (trimmed == null) {
			return null;
		}
		return trimmed.matches("https?://.*") ? trimmed : null;
	}
}
