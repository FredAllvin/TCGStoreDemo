package com.tcgstore.shop.web.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminAuthController {

	@GetMapping("/admin/login")
	public String loginPage() {
		return "admin/login";
	}
}
