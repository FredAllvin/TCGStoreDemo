package com.tcgstore.shop;

import com.tcgstore.shop.domain.Category;
import com.tcgstore.shop.domain.Condition;
import com.tcgstore.shop.domain.Finish;
import com.tcgstore.shop.domain.Order;
import com.tcgstore.shop.domain.OrderStatus;
import com.tcgstore.shop.domain.Product;
import com.tcgstore.shop.domain.ProductVariant;
import com.tcgstore.shop.domain.ShippingOption;
import com.tcgstore.shop.domain.StoreSettings;
import com.tcgstore.shop.domain.VariantKind;
import com.tcgstore.shop.repo.CategoryRepository;
import com.tcgstore.shop.repo.OrderRepository;
import com.tcgstore.shop.repo.ProductRepository;
import com.tcgstore.shop.repo.ShippingOptionRepository;
import com.tcgstore.shop.service.OrderService;
import com.tcgstore.shop.service.SettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the site-wide fixes from the 2026-07 audit: language links that keep
 * the query string, localized shipping names, the localized country default,
 * the configurable default locale, literal wildcard search, and payments that
 * arrive for already-cancelled orders being flagged instead of swallowed.
 */
@SpringBootTest(properties = "app.upload-dir=target/test-uploads")
@Testcontainers(disabledWithoutDocker = true)
class SiteWideFixesTests {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	WebApplicationContext context;
	@Autowired
	CategoryRepository categories;
	@Autowired
	ProductRepository products;
	@Autowired
	ShippingOptionRepository shippingOptions;
	@Autowired
	OrderRepository orders;
	@Autowired
	OrderService orderService;
	@Autowired
	SettingsService settingsService;

	MockMvc mvc;
	String productSlug;
	Long variantId;
	Long shippingId;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
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
		productSlug = "charizard-" + System.nanoTime();
		product.setSlug(productSlug);

		ProductVariant raw = new ProductVariant();
		raw.setKind(VariantKind.RAW);
		raw.setCondition(Condition.NM);
		raw.setFinish(Finish.NONFOIL);
		raw.setPriceMinor(12_345);
		raw.setStockQty(5);
		product.addVariant(raw);
		product = products.save(product);
		variantId = product.getVariants().get(0).getId();

		ShippingOption shipping = new ShippingOption();
		shipping.setName("PostNord spårbart paket");
		shipping.setNameEn("PostNord tracked parcel");
		shipping.setPriceMinor(2_900);
		shippingId = shippingOptions.save(shipping).getId();
	}

	// --- language switcher keeps the query string ---

	@Test
	void languageLinksKeepTheSearchQuery() throws Exception {
		// a raw query string in the URI: MockMvc's .param() never populates getQueryString()
		mvc.perform(get("/search?q=charizard&lang=en"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("/search?q=charizard&amp;lang=sv")))
				.andExpect(content().string(not(containsString("lang=en&amp;lang=sv"))));
	}

	@Test
	void languageLinksKeepTheOrderAccessToken() throws Exception {
		Order order = placeOrderThroughTheShop();

		mvc.perform(get("/order/" + order.getOrderNumber() + "?t=" + order.getAccessToken()))
				.andExpect(status().isOk())
				.andExpect(content().string(
						containsString("t=" + order.getAccessToken() + "&amp;lang=en")));
	}

	// --- localized shipping names ---

	@Test
	void checkoutShowsTheShippingNameInTheVisitorsLanguage() throws Exception {
		MockHttpSession session = new MockHttpSession();
		addToCart(session);

		mvc.perform(get("/checkout").session(session).param("lang", "en"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("PostNord tracked parcel")));
		mvc.perform(get("/checkout").session(session).param("lang", "sv"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("PostNord spårbart paket")));
	}

	@Test
	void orderConfirmationShowsTheLocalizedShippingSnapshotAndNoEmailPromise() throws Exception {
		Order order = placeOrderThroughTheShop();

		mvc.perform(get("/order/" + order.getOrderNumber())
						.param("t", order.getAccessToken()).param("lang", "en"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("PostNord tracked parcel")))
				.andExpect(content().string(not(containsString("A confirmation will be sent"))));
	}

	// --- localized checkout country default ---

	@Test
	void checkoutCountryDefaultsToTheVisitorsLanguage() throws Exception {
		MockHttpSession session = new MockHttpSession();
		addToCart(session);

		mvc.perform(get("/checkout").session(session).param("lang", "en"))
				.andExpect(content().string(containsString("value=\"Sweden\"")));
		mvc.perform(get("/checkout").session(session).param("lang", "sv"))
				.andExpect(content().string(containsString("value=\"Sverige\"")));
	}

	// --- store-configurable default locale ---

	@Test
	void storeDefaultLocaleSettingAppliesToVisitorsWithoutACookie() throws Exception {
		StoreSettings settings = settingsService.get();
		try {
			settings.setDefaultLocale("en");
			settingsService.save(settings);

			mvc.perform(get("/"))
					.andExpect(status().isOk())
					.andExpect(content().string(containsString("Search cards, sets, numbers")));
		}
		finally {
			settings.setDefaultLocale("sv");
			settingsService.save(settings);
		}
	}

	// --- localized stock unit on the product page ---

	@Test
	void productPageShowsTheStockUnitInTheVisitorsLanguage() throws Exception {
		mvc.perform(get("/p/" + productSlug).param("lang", "en"))
				.andExpect(content().string(containsString("5 pcs")));
		mvc.perform(get("/p/" + productSlug).param("lang", "sv"))
				.andExpect(content().string(containsString("5 st")));
	}

	// --- variant-less products don't show a bogus zero price ---

	@Test
	void productWithoutVariantsShowsNoZeroPrice() throws Exception {
		Product empty = new Product();
		empty.setCategory(categories.findAll().get(0));
		empty.setName("Tom produkt");
		empty.setSlug("tom-" + System.nanoTime());
		empty.setFeatured(true);
		products.save(empty);

		mvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Tom produkt")))
				.andExpect(content().string(not(containsString("0,00"))));
	}

	// --- wildcard characters in search match literally ---

	@Test
	void searchWildcardsAreTreatedLiterally() throws Exception {
		mvc.perform(get("/search").param("q", "%%"))
				.andExpect(status().isOk())
				.andExpect(content().string(not(containsString("Charizard ex"))));
		mvc.perform(get("/search").param("q", "ch_rizard"))
				.andExpect(status().isOk())
				.andExpect(content().string(not(containsString("Charizard ex"))));
		mvc.perform(get("/search").param("q", "charizard"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Charizard ex")));
	}

	// --- payment for an already-cancelled order is flagged, not swallowed ---

	@Test
	void paymentArrivingForACancelledOrderKeepsTheReferenceForManualHandling() {
		Order order = new Order();
		order.setOrderNumber("T-" + System.nanoTime());
		order.setAccessToken(UUID.randomUUID().toString());
		order.setCustomerName("Test Kund");
		order.setEmail("test@example.com");
		order.setAddressLine1("Testgatan 1");
		order.setPostalCode("12345");
		order.setCity("Stockholm");
		order.setCountry("Sverige");
		order.setShippingName("Post");
		order.setCurrency("SEK");
		order = orders.save(order);

		orderService.cancel(order);
		orderService.markPaid(orders.findById(order.getId()).orElseThrow(), "LATE-REF");

		Order reloaded = orders.findById(order.getId()).orElseThrow();
		assertEquals(OrderStatus.CANCELLED, reloaded.getStatus(), "a late payment must not resurrect the order");
		assertEquals("LATE-REF", reloaded.getPaymentRef(), "the payment reference is kept for manual refunding");
	}

	// --- admin form limits ---

	@Test
	void overlongSettingsFieldsAreRejectedWithAClearMessage() throws Exception {
		mvc.perform(post("/admin/settings")
						.param("storeName", "Kortbutiken")
						.param("primaryColor", "#1f2a44")
						.param("accentColor", "#e8590c")
						.param("currency", "SEK")
						.param("contactPhone", "0".repeat(51))
						.with(user("admin").roles("ADMIN")).with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(flash().attribute("flashErrorKey", "admin.settings.tooLong"));
	}

	@Test
	void overlongShippingNameGetsTheRightErrorNotBadPrice() throws Exception {
		mvc.perform(post("/admin/settings/shipping")
						.param("name", "x".repeat(121))
						.param("price", "69")
						.with(user("admin").roles("ADMIN")).with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(flash().attribute("flashErrorKey", "admin.settings.tooLong"));

		mvc.perform(post("/admin/settings/shipping")
						.param("name", "Bud")
						.param("price", "not-a-price")
						.with(user("admin").roles("ADMIN")).with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(flash().attribute("flashErrorKey", "admin.variants.badPrice"));
	}

	@Test
	void adminCanAddAShippingOptionWithAnEnglishName() throws Exception {
		mvc.perform(post("/admin/settings/shipping")
						.param("name", "Bud inom stan")
						.param("nameEn", "Local courier")
						.param("price", "49")
						.with(user("admin").roles("ADMIN")).with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(flash().attribute("flashSuccessKey", "admin.saved"));

		ShippingOption saved = shippingOptions.findAll().stream()
				.filter(option -> "Bud inom stan".equals(option.getName()))
				.findFirst().orElseThrow();
		assertEquals("Local courier", saved.getNameEn());
	}

	// --- helpers ---

	private void addToCart(MockHttpSession session) throws Exception {
		mvc.perform(post("/cart/add")
						.param("variantId", variantId.toString())
						.param("qty", "1")
						.session(session).with(csrf()))
				.andExpect(status().is3xxRedirection());
	}

	/** Runs the real cart → checkout flow and returns the created order. */
	private Order placeOrderThroughTheShop() throws Exception {
		MockHttpSession session = new MockHttpSession();
		addToCart(session);

		MvcResult result = mvc.perform(post("/checkout")
						.param("name", "Test Kund")
						.param("email", "test@example.com")
						.param("addressLine1", "Testgatan 1")
						.param("postalCode", "12345")
						.param("city", "Stockholm")
						.param("country", "Sverige")
						.param("shippingOptionId", shippingId.toString())
						.session(session).with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andReturn();

		String redirect = result.getResponse().getRedirectedUrl();
		assertNotNull(redirect);
		assertTrue(redirect.startsWith("/pay/mock/"), "mock payment redirect expected, got: " + redirect);
		Order order = orders.findAll().get(0);
		assertEquals("PostNord tracked parcel", order.getShippingNameEn(), "both name snapshots are stored");
		return order;
	}
}
