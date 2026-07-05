package com.tcgstore.shop.web.admin;

import com.tcgstore.shop.service.AdminAccountService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/password")
public class AdminPasswordController {

	private final AdminAccountService accounts;

	public AdminPasswordController(AdminAccountService accounts) {
		this.accounts = accounts;
	}

	@GetMapping
	public String form() {
		return "admin/password";
	}

	@PostMapping
	public String change(@AuthenticationPrincipal UserDetails user,
			@RequestParam String currentPassword,
			@RequestParam String newPassword,
			@RequestParam String confirmPassword,
			RedirectAttributes redirect) {
		if (newPassword.length() < 8) {
			redirect.addFlashAttribute("flashErrorKey", "admin.password.tooShort");
		}
		else if (!newPassword.equals(confirmPassword)) {
			redirect.addFlashAttribute("flashErrorKey", "admin.password.mismatch");
		}
		else if (!accounts.changePassword(user.getUsername(), currentPassword, newPassword)) {
			redirect.addFlashAttribute("flashErrorKey", "admin.password.wrongCurrent");
		}
		else {
			redirect.addFlashAttribute("flashSuccessKey", "admin.password.changed");
		}
		return "redirect:/admin/password";
	}
}
