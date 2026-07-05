package com.tcgstore.shop;

import com.tcgstore.shop.domain.Category;
import com.tcgstore.shop.domain.Condition;
import com.tcgstore.shop.domain.Finish;
import com.tcgstore.shop.domain.Grader;
import com.tcgstore.shop.domain.Order;
import com.tcgstore.shop.domain.OrderStatus;
import com.tcgstore.shop.domain.Product;
import com.tcgstore.shop.domain.ProductVariant;
import com.tcgstore.shop.domain.ShippingOption;
import com.tcgstore.shop.domain.VariantKind;
import com.tcgstore.shop.repo.CategoryRepository;
import com.tcgstore.shop.repo.OrderRepository;
import com.tcgstore.shop.repo.ProcessedWebhookEventRepository;
import com.tcgstore.shop.repo.ProductRepository;
import com.tcgstore.shop.repo.ProductVariantRepository;
import com.tcgstore.shop.repo.ShippingOptionRepository;
import com.tcgstore.shop.service.Cart;
import com.tcgstore.shop.service.CheckoutService;
import com.tcgstore.shop.service.OrderService;
import com.tcgstore.shop.service.OutOfStockException;
import com.tcgstore.shop.web.dto.CheckoutForm;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "app.upload-dir=target/test-uploads")
@Testcontainers(disabledWithoutDocker = true)
class CheckoutFlowTests {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	CheckoutService checkoutService;
	@Autowired
	OrderService orderService;
	@Autowired
	Cart cart;
	@Autowired
	ProductRepository products;
	@Autowired
	CategoryRepository categories;
	@Autowired
	ProductVariantRepository variants;
	@Autowired
	ShippingOptionRepository shippingOptions;
	@Autowired
	OrderRepository orders;
	@Autowired
	ProcessedWebhookEventRepository webhookEvents;

	Long rawVariantId;
	Long gradedVariantId;
	Long shippingId;

	@BeforeEach
	void setUp() {
		// session-scoped Cart needs a request context
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
		webhookEvents.deleteAllInBatch();
		orders.deleteAllInBatch();
		products.deleteAllInBatch();
		categories.deleteAllInBatch();
		shippingOptions.deleteAllInBatch();

		Category category = new Category();
		category.setName("Pokémon");
		category.setSlug("pokemon-" + System.nanoTime());
		category = categories.save(category);

		Product product = new Product();
		product.setCategory(category);
		product.setName("Charizard ex");
		product.setSlug("charizard-" + System.nanoTime());

		ProductVariant raw = new ProductVariant();
		raw.setKind(VariantKind.RAW);
		raw.setCondition(Condition.NM);
		raw.setFinish(Finish.NONFOIL);
		raw.setPriceMinor(10_000);
		raw.setStockQty(5);
		product.addVariant(raw);

		ProductVariant graded = new ProductVariant();
		graded.setKind(VariantKind.GRADED);
		graded.setGrader(Grader.PSA);
		graded.setGrade("10");
		graded.setCertNumber("12345678");
		graded.setPriceMinor(100_000);
		graded.setStockQty(1);
		product.addVariant(graded);

		product = products.save(product);
		rawVariantId = product.getVariants().get(0).getId();
		gradedVariantId = product.getVariants().get(1).getId();

		ShippingOption shipping = new ShippingOption();
		shipping.setName("Post");
		shipping.setPriceMinor(2_900);
		shippingId = shippingOptions.save(shipping).getId();
	}

	@AfterEach
	void tearDown() {
		RequestContextHolder.resetRequestAttributes();
	}

	private CheckoutForm form() {
		CheckoutForm form = new CheckoutForm();
		form.setName("Test Kund");
		form.setEmail("test@example.com");
		form.setAddressLine1("Testgatan 1");
		form.setPostalCode("12345");
		form.setCity("Stockholm");
		form.setCountry("Sverige");
		form.setShippingOptionId(shippingId);
		return form;
	}

	@Test
	void placeOrderComputesTotalsFromDatabaseAndReservesStock() {
		cart.add(rawVariantId, 2);

		Order order = checkoutService.placeOrder(form(), "mock");

		assertEquals(20_000, order.getSubtotalMinor());
		assertEquals(2_900, order.getShippingMinor());
		assertEquals(22_900, order.getTotalMinor());
		assertEquals(OrderStatus.PENDING, order.getStatus());
		assertNotNull(order.getAccessToken());
		assertEquals(3, variants.findById(rawVariantId).orElseThrow().getStockQty());
		assertTrue(cart.isEmpty(), "cart is cleared after checkout");
	}

	@Test
	void lastGradedSlabCanOnlyBeSoldOnce() throws Exception {
		// two customers race for a one-of-one graded card
		AtomicInteger successes = new AtomicInteger();
		AtomicInteger outOfStock = new AtomicInteger();
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(2);

		Runnable buyer = () -> {
			RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
			try {
				cart.add(gradedVariantId, 1);
				start.await();
				checkoutService.placeOrder(form(), "mock");
				successes.incrementAndGet();
			}
			catch (OutOfStockException e) {
				outOfStock.incrementAndGet();
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			finally {
				RequestContextHolder.resetRequestAttributes();
				done.countDown();
			}
		};
		new Thread(buyer).start();
		new Thread(buyer).start();
		start.countDown();
		done.await();

		assertEquals(1, successes.get(), "exactly one buyer gets the slab");
		assertEquals(1, outOfStock.get(), "the other gets an out-of-stock error");
		assertEquals(0, variants.findById(gradedVariantId).orElseThrow().getStockQty());
	}

	@Test
	void cancellingAnOrderRestoresStock() {
		cart.add(rawVariantId, 3);
		Order order = checkoutService.placeOrder(form(), "mock");
		assertEquals(2, variants.findById(rawVariantId).orElseThrow().getStockQty());

		orderService.cancel(orders.findById(order.getId()).orElseThrow());

		assertEquals(OrderStatus.CANCELLED, orders.findById(order.getId()).orElseThrow().getStatus());
		assertEquals(5, variants.findById(rawVariantId).orElseThrow().getStockQty());
	}

	@Test
	void markPaidIsIdempotent() {
		cart.add(rawVariantId, 1);
		Order order = checkoutService.placeOrder(form(), "mock");

		orderService.markPaid(orders.findById(order.getId()).orElseThrow(), "REF-1");
		orderService.markPaid(orders.findById(order.getId()).orElseThrow(), "REF-2");

		Order reloaded = orders.findById(order.getId()).orElseThrow();
		assertEquals(OrderStatus.PAID, reloaded.getStatus());
		assertEquals("REF-1", reloaded.getPaymentRef(), "second markPaid must not overwrite the first");
	}

	@Test
	void expiredPendingOrdersAreReleased() {
		cart.add(rawVariantId, 2);
		Order order = checkoutService.placeOrder(form(), "mock");

		Order stale = orders.findById(order.getId()).orElseThrow();
		stale.setCreatedAt(Instant.now().minus(2, ChronoUnit.HOURS));
		orders.save(stale);

		int released = orderService.releaseExpiredPending(java.time.Duration.ofMinutes(60));

		assertEquals(1, released);
		assertEquals(OrderStatus.CANCELLED, orders.findById(order.getId()).orElseThrow().getStatus());
		assertEquals(5, variants.findById(rawVariantId).orElseThrow().getStockQty());
	}
}
