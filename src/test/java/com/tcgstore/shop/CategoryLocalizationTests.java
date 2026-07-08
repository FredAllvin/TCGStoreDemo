package com.tcgstore.shop;

import com.tcgstore.shop.domain.Category;
import com.tcgstore.shop.repo.CategoryRepository;
import com.tcgstore.shop.repo.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Category names show in the visitor's language: "Tillbehör" for Swedish, "Accessories" for English. */
@SpringBootTest(properties = "app.upload-dir=target/test-uploads")
@Testcontainers(disabledWithoutDocker = true)
class CategoryLocalizationTests {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	WebApplicationContext context;
	@Autowired
	CategoryRepository categories;
	@Autowired
	ProductRepository products;

	MockMvc mvc;
	Category accessories;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
		products.deleteAllInBatch();
		categories.deleteAllInBatch();

		Category category = new Category();
		category.setName("Tillbehör");
		category.setNameEn("Accessories");
		category.setSlug("tillbehor-" + System.nanoTime());
		category.setSortOrder(4);
		accessories = categories.save(category);
	}

	@Test
	void categoryPageShowsTheEnglishNameForEnglishVisitors() throws Exception {
		mvc.perform(get("/c/" + accessories.getSlug()).param("lang", "en"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Accessories")))
				.andExpect(content().string(not(containsString("Tillbehör"))));
	}

	@Test
	void categoryPageShowsTheSwedishNameByDefault() throws Exception {
		mvc.perform(get("/c/" + accessories.getSlug()))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Tillbehör")));
	}

	@Test
	void englishFallsBackToTheSwedishNameWhenNoTranslationExists() throws Exception {
		accessories.setNameEn(null);
		categories.save(accessories);

		mvc.perform(get("/c/" + accessories.getSlug()).param("lang", "en"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Tillbehör")));
	}

	@Test
	void adminCanCreateACategoryWithAnEnglishName() throws Exception {
		mvc.perform(post("/admin/categories")
						.param("name", "Pärmar")
						.param("nameEn", "Binders")
						.param("sortOrder", "5")
						.with(user("admin").roles("ADMIN"))
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(flash().attribute("flashSuccessKey", "admin.saved"));

		Category saved = categories.findAll().stream()
				.filter(c -> "Pärmar".equals(c.getName()))
				.findFirst().orElseThrow();
		assertEquals("Binders", saved.getNameEn());
	}

	@Test
	void adminCanUpdateTheEnglishName() throws Exception {
		mvc.perform(post("/admin/categories/" + accessories.getId())
						.param("name", "Tillbehör")
						.param("nameEn", "Supplies")
						.param("sortOrder", "4")
						.with(user("admin").roles("ADMIN"))
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(flash().attribute("flashSuccessKey", "admin.saved"));

		assertEquals("Supplies", categories.findById(accessories.getId()).orElseThrow().getNameEn());
	}

	@Test
	void overlongEnglishNameIsRejected() throws Exception {
		mvc.perform(post("/admin/categories/" + accessories.getId())
						.param("name", "Tillbehör")
						.param("nameEn", "x".repeat(121))
						.param("sortOrder", "4")
						.with(user("admin").roles("ADMIN"))
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(flash().attribute("flashErrorKey", "admin.categories.tooLong"));

		assertEquals("Accessories", categories.findById(accessories.getId()).orElseThrow().getNameEn());
	}
}
