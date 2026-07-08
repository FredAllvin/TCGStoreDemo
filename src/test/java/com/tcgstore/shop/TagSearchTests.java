package com.tcgstore.shop;

import com.tcgstore.shop.domain.Category;
import com.tcgstore.shop.domain.Product;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Products are findable by their admin-set search tags, e.g. "pokemon". */
@SpringBootTest(properties = "app.upload-dir=target/test-uploads")
@Testcontainers(disabledWithoutDocker = true)
class TagSearchTests {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	WebApplicationContext context;
	@Autowired
	ProductRepository products;
	@Autowired
	CategoryRepository categories;

	MockMvc mvc;
	Long categoryId;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
		products.deleteAllInBatch();
		categories.deleteAllInBatch();

		Category category = new Category();
		category.setName("Pokémon");
		category.setSlug("pokemon-" + System.nanoTime());
		categoryId = categories.save(category).getId();
	}

	private void createProduct(String name, String tags) throws Exception {
		var request = multipart("/admin/products")
				.param("name", name)
				.param("categoryId", categoryId.toString())
				.with(user("admin").roles("ADMIN"))
				.with(csrf());
		if (tags != null) {
			request = request.param("tags", tags);
		}
		mvc.perform(request)
				.andExpect(status().is3xxRedirection())
				.andExpect(flash().attribute("flashSuccessKey", "admin.saved"));
	}

	@Test
	void searchFindsProductsByTag() throws Exception {
		// "Charizard ex" does not contain "pokemon" — only the tag can match it
		createProduct("Charizard ex", "pokemon, vintage");
		assertEquals("pokemon, vintage", products.findAll().get(0).getTags());
		createProduct("Sol Ring", null);

		mvc.perform(get("/search").param("q", "pokemon"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Charizard ex")))
				.andExpect(content().string(not(containsString("Sol Ring"))));
	}

	@Test
	void tagSearchIsCaseInsensitive() throws Exception {
		createProduct("Charizard ex", "pokemon");

		mvc.perform(get("/search").param("q", "POKEMON"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Charizard ex")));
	}

	@Test
	void untaggedProductsAreStillFoundByName() throws Exception {
		createProduct("Sol Ring", null);

		mvc.perform(get("/search").param("q", "sol ring"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Sol Ring")));
	}

	@Test
	void productsAreFoundByTheirCategoryTree() throws Exception {
		Category magic = new Category();
		magic.setName("Magic: The Gathering");
		magic.setSlug("magic-" + System.nanoTime());
		magic = categories.save(magic);

		Category singles = new Category();
		singles.setName("Singles");
		singles.setParent(magic);
		singles.setSlug("magic-singles-" + System.nanoTime());
		singles = categories.save(singles);

		// nothing on the product itself contains "magic" — only the parent category does
		Product counterspell = new Product();
		counterspell.setCategory(singles);
		counterspell.setName("Counterspell");
		counterspell.setSetName("Commander Masters");
		counterspell.setSlug("counterspell-" + System.nanoTime());
		counterspell.setActive(true);
		products.save(counterspell);

		mvc.perform(get("/search").param("q", "magic"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Counterspell")));
		mvc.perform(get("/search").param("q", "singles"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Counterspell")));
		// products in root-level categories must survive the parent join
		createProduct("Pikachu", null);
		mvc.perform(get("/search").param("q", "pikachu"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Pikachu")));
	}

	@Test
	void categorySearchWorksInBothLanguages() throws Exception {
		Category accessories = new Category();
		accessories.setName("Tillbehör");
		accessories.setNameEn("Accessories");
		accessories.setSlug("tillbehor-" + System.nanoTime());
		accessories = categories.save(accessories);

		Product binder = new Product();
		binder.setCategory(accessories);
		binder.setName("Pärm 360");
		binder.setSlug("parm-" + System.nanoTime());
		binder.setActive(true);
		products.save(binder);

		mvc.perform(get("/search").param("q", "tillbehör"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Pärm 360")));
		mvc.perform(get("/search").param("q", "accessories"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Pärm 360")));
	}

	@Test
	void overlongTagsAreRejected() throws Exception {
		mvc.perform(multipart("/admin/products")
						.param("name", "Charizard ex")
						.param("categoryId", categoryId.toString())
						.param("tags", "x".repeat(201))
						.with(user("admin").roles("ADMIN"))
						.with(csrf()))
				.andExpect(redirectedUrl("/admin/products/new"))
				.andExpect(flash().attribute("flashErrorKey", "admin.products.tooLong"));

		assertEquals(0, products.count());
	}
}
