package com.tcgstore.shop.web.admin;

import com.tcgstore.shop.domain.Order;
import com.tcgstore.shop.domain.OrderStatus;
import com.tcgstore.shop.repo.OrderRepository;
import com.tcgstore.shop.service.OrderService;
import com.tcgstore.shop.web.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {

	private final OrderRepository orders;
	private final OrderService orderService;

	public AdminOrderController(OrderRepository orders, OrderService orderService) {
		this.orders = orders;
		this.orderService = orderService;
	}

	@GetMapping
	public String list(@RequestParam(required = false) OrderStatus status,
			@RequestParam(defaultValue = "0") int page, Model model) {
		Pageable pageable = PageRequest.of(Math.max(0, page), 25);
		Page<Order> result = status == null
				? orders.findAllByOrderByCreatedAtDesc(pageable)
				: orders.findByStatusOrderByCreatedAtDesc(status, pageable);
		model.addAttribute("orders", result);
		model.addAttribute("statusFilter", status);
		model.addAttribute("allStatuses", OrderStatus.values());
		return "admin/orders";
	}

	@GetMapping("/{id}")
	public String detail(@PathVariable Long id, Model model) {
		model.addAttribute("order", orders.findById(id).orElseThrow(NotFoundException::new));
		return "admin/order-detail";
	}

	@PostMapping("/{id}/paid")
	public String markPaid(@PathVariable Long id, RedirectAttributes redirect) {
		orderService.markPaid(order(id), "MANUAL");
		redirect.addFlashAttribute("flashSuccessKey", "admin.saved");
		return "redirect:/admin/orders/" + id;
	}

	@PostMapping("/{id}/shipped")
	public String markShipped(@PathVariable Long id, RedirectAttributes redirect) {
		orderService.markShipped(order(id));
		redirect.addFlashAttribute("flashSuccessKey", "admin.saved");
		return "redirect:/admin/orders/" + id;
	}

	@PostMapping("/{id}/cancel")
	public String cancel(@PathVariable Long id, RedirectAttributes redirect) {
		orderService.cancel(order(id));
		redirect.addFlashAttribute("flashSuccessKey", "admin.orders.cancelled");
		return "redirect:/admin/orders/" + id;
	}

	private Order order(Long id) {
		return orders.findById(id).orElseThrow(NotFoundException::new);
	}
}
