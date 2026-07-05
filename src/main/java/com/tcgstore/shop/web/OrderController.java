package com.tcgstore.shop.web;

import com.tcgstore.shop.domain.Order;
import com.tcgstore.shop.domain.OrderStatus;
import com.tcgstore.shop.repo.OrderRepository;
import com.tcgstore.shop.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

/** Customer-facing order pages: the mock payment page and the confirmation page. */
@Controller
public class OrderController {

	private final OrderRepository orders;
	private final OrderService orderService;

	public OrderController(OrderRepository orders, OrderService orderService) {
		this.orders = orders;
		this.orderService = orderService;
	}

	@GetMapping("/pay/mock/{orderNumber}")
	public String mockPayPage(@PathVariable String orderNumber, @RequestParam("t") String token, Model model) {
		Order order = findOrder(orderNumber, token);
		if (order.getStatus() != OrderStatus.PENDING) {
			return confirmationUrl(order);
		}
		model.addAttribute("order", order);
		return "shop/pay-mock";
	}

	@PostMapping("/pay/mock/{orderNumber}")
	public String mockPay(@PathVariable String orderNumber, @RequestParam("t") String token) {
		Order order = findOrder(orderNumber, token);
		orderService.markPaid(order, "MOCK-" + UUID.randomUUID().toString().substring(0, 8));
		return confirmationUrl(order);
	}

	@PostMapping("/pay/mock/{orderNumber}/cancel")
	public String mockCancel(@PathVariable String orderNumber, @RequestParam("t") String token,
			RedirectAttributes redirect) {
		Order order = findOrder(orderNumber, token);
		orderService.cancel(order);
		redirect.addFlashAttribute("flashErrorKey", "pay.cancelled");
		return "redirect:/";
	}

	@GetMapping("/order/{orderNumber}")
	public String confirmation(@PathVariable String orderNumber, @RequestParam("t") String token, Model model) {
		model.addAttribute("order", findOrder(orderNumber, token));
		return "shop/order";
	}

	private Order findOrder(String orderNumber, String token) {
		return orders.findByOrderNumberAndAccessToken(orderNumber, token).orElseThrow(NotFoundException::new);
	}

	private String confirmationUrl(Order order) {
		return "redirect:/order/" + order.getOrderNumber() + "?t=" + order.getAccessToken();
	}
}
