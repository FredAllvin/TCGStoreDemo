package com.tcgstore.shop.web.admin;

import com.tcgstore.shop.domain.OrderStatus;
import com.tcgstore.shop.domain.VariantKind;
import com.tcgstore.shop.repo.OrderRepository;
import com.tcgstore.shop.repo.ProductRepository;
import com.tcgstore.shop.repo.ProductVariantRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Controller
public class AdminDashboardController {

	private final OrderRepository orders;
	private final ProductRepository products;
	private final ProductVariantRepository variants;

	public AdminDashboardController(OrderRepository orders, ProductRepository products,
			ProductVariantRepository variants) {
		this.orders = orders;
		this.products = products;
		this.variants = variants;
	}

	@GetMapping("/admin")
	public String dashboard(Model model) {
		model.addAttribute("pendingCount", orders.countByStatus(OrderStatus.PENDING));
		model.addAttribute("paidCount", orders.countByStatus(OrderStatus.PAID));
		model.addAttribute("shippedCount", orders.countByStatus(OrderStatus.SHIPPED));
		model.addAttribute("productCount", products.count());
		model.addAttribute("revenue30", orders.revenueSince(
				List.of(OrderStatus.PAID, OrderStatus.SHIPPED),
				Instant.now().minus(30, ChronoUnit.DAYS)));
		model.addAttribute("recentOrders", orders.findTop8ByOrderByCreatedAtDesc());
		model.addAttribute("lowStock",
				variants.findTop10ByActiveTrueAndKindAndStockQtyLessThanEqualOrderByStockQtyAsc(VariantKind.STANDARD, 3));
		return "admin/dashboard";
	}
}
