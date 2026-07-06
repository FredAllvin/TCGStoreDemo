package com.tcgstore.shop;

import com.tcgstore.shop.domain.Category;
import com.tcgstore.shop.domain.Product;
import com.tcgstore.shop.domain.ProductImage;
import com.tcgstore.shop.repo.CategoryRepository;
import com.tcgstore.shop.repo.ProductImageRepository;
import com.tcgstore.shop.repo.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Creating a product in the admin panel, including images in the same submit. */
@SpringBootTest(properties = "app.upload-dir=target/test-uploads")
@Testcontainers(disabledWithoutDocker = true)
class AdminProductCreationTests {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	WebApplicationContext context;
	@Autowired
	ProductRepository products;
	@Autowired
	ProductImageRepository images;
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

	private static MockMultipartFile realPng(String name) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB), "png", out);
		return new MockMultipartFile("files", name, "image/png", out.toByteArray());
	}

	@Test
	void newProductFormOffersImageUpload() throws Exception {
		mvc.perform(get("/admin/products/new").with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("enctype=\"multipart/form-data\"")))
				.andExpect(content().string(containsString("name=\"files\"")));
	}

	@Test
	void createStoresUploadedImagesOnTheNewProduct() throws Exception {
		mvc.perform(multipart("/admin/products")
						.file(realPng("front.png"))
						.file(realPng("back.png"))
						.param("name", "Charizard ex")
						.param("categoryId", categoryId.toString())
						.with(user("admin").roles("ADMIN"))
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrlPattern("/admin/products/*/edit"))
				.andExpect(flash().attribute("flashSuccessKey", "admin.saved"));

		assertEquals("Charizard ex", products.findAll().get(0).getName());
		assertEquals(2, images.count());
		for (ProductImage image : images.findAll()) {
			assertTrue(Files.exists(Paths.get("target/test-uploads", image.getPath())),
					"uploaded file is on disk: " + image.getPath());
		}
	}

	@Test
	void invalidImageStillCreatesTheProductButFlagsTheError() throws Exception {
		MockMultipartFile fake = new MockMultipartFile("files", "card.png", "image/png",
				"MZ not an image".getBytes());

		mvc.perform(multipart("/admin/products")
						.file(fake)
						.param("name", "Pikachu")
						.param("categoryId", categoryId.toString())
						.with(user("admin").roles("ADMIN"))
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrlPattern("/admin/products/*/edit"))
				.andExpect(flash().attribute("flashErrorKey", "admin.images.invalid"));

		assertEquals("Pikachu", products.findAll().get(0).getName());
		assertEquals(0, images.count());
	}

	@Test
	void emptyFileInputIsIgnored() throws Exception {
		// what the browser sends when no image is chosen
		MockMultipartFile none = new MockMultipartFile("files", "", "application/octet-stream", new byte[0]);

		mvc.perform(multipart("/admin/products")
						.file(none)
						.param("name", "Sleeves")
						.param("categoryId", categoryId.toString())
						.with(user("admin").roles("ADMIN"))
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(flash().attribute("flashSuccessKey", "admin.saved"));

		assertEquals(1, products.count());
		assertEquals(0, images.count());
	}

	@Test
	void createRejectsOverlongFields() throws Exception {
		mvc.perform(multipart("/admin/products")
						.param("name", "x".repeat(201))
						.param("categoryId", categoryId.toString())
						.with(user("admin").roles("ADMIN"))
						.with(csrf()))
				.andExpect(redirectedUrl("/admin/products/new"))
				.andExpect(flash().attribute("flashErrorKey", "admin.products.tooLong"));

		mvc.perform(multipart("/admin/products")
						.param("name", "Charizard ex")
						.param("categoryId", categoryId.toString())
						.param("description", "x".repeat(4001))
						.with(user("admin").roles("ADMIN"))
						.with(csrf()))
				.andExpect(redirectedUrl("/admin/products/new"))
				.andExpect(flash().attribute("flashErrorKey", "admin.products.tooLong"));

		assertEquals(0, products.count());
	}

	@Test
	void descriptionFollowsTheShopLanguage() throws Exception {
		mvc.perform(multipart("/admin/products")
						.param("name", "Mew ex")
						.param("categoryId", categoryId.toString())
						.param("description", "Svensk beskrivning av kortet.")
						.param("descriptionEn", "English description of the card.")
						.with(user("admin").roles("ADMIN"))
						.with(csrf()))
				.andExpect(status().is3xxRedirection());
		String slug = products.findAll().get(0).getSlug();

		mvc.perform(get("/p/" + slug))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Svensk beskrivning av kortet.")));

		mvc.perform(get("/p/" + slug).param("lang", "en"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("English description of the card.")));
	}

	@Test
	void missingTranslationFallsBackToSwedish() throws Exception {
		mvc.perform(multipart("/admin/products")
						.param("name", "Ditto")
						.param("categoryId", categoryId.toString())
						.param("description", "Bara svensk beskrivning.")
						.with(user("admin").roles("ADMIN"))
						.with(csrf()))
				.andExpect(status().is3xxRedirection());
		String slug = products.findAll().get(0).getSlug();

		mvc.perform(get("/p/" + slug).param("lang", "en"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Bara svensk beskrivning.")));
	}

	@Test
	void updateRejectsOverlongDescription() throws Exception {
		Product product = new Product();
		product.setCategory(categories.findById(categoryId).orElseThrow());
		product.setName("Pikachu");
		product.setSlug("pikachu-" + System.nanoTime());
		Long id = products.save(product).getId();

		mvc.perform(multipart("/admin/products/" + id)
						.param("name", "Pikachu")
						.param("categoryId", categoryId.toString())
						.param("description", "x".repeat(4001))
						.with(user("admin").roles("ADMIN"))
						.with(csrf()))
				.andExpect(redirectedUrl("/admin/products/" + id + "/edit"))
				.andExpect(flash().attribute("flashErrorKey", "admin.products.tooLong"));

		assertNull(products.findById(id).orElseThrow().getDescription());
	}
}
