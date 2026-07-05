package com.tcgstore.shop.demo;

import com.tcgstore.shop.config.AppProperties;
import com.tcgstore.shop.repo.CategoryRepository;
import com.tcgstore.shop.repo.OrderRepository;
import com.tcgstore.shop.repo.ProcessedWebhookEventRepository;
import com.tcgstore.shop.repo.ProductRepository;
import com.tcgstore.shop.repo.ShippingOptionRepository;
import com.tcgstore.shop.service.AdminAccountService;
import com.tcgstore.shop.service.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Nightly reset for the public demo instance: wipes all business data,
 * re-seeds the placeholder catalog and restores the demo admin credentials,
 * so visitors can play freely without permanently changing anything.
 */
@Component
@Profile("demo")
public class DemoResetJob {

	private static final Logger log = LoggerFactory.getLogger(DemoResetJob.class);

	private final OrderRepository orders;
	private final ProductRepository products;
	private final CategoryRepository categories;
	private final ShippingOptionRepository shippingOptions;
	private final ProcessedWebhookEventRepository webhookEvents;
	private final DemoDataSeeder seeder;
	private final AdminAccountService adminAccounts;
	private final SettingsService settingsService;
	private final AppProperties props;

	public DemoResetJob(OrderRepository orders, ProductRepository products, CategoryRepository categories,
			ShippingOptionRepository shippingOptions, ProcessedWebhookEventRepository webhookEvents,
			DemoDataSeeder seeder, AdminAccountService adminAccounts, SettingsService settingsService,
			AppProperties props) {
		this.orders = orders;
		this.products = products;
		this.categories = categories;
		this.shippingOptions = shippingOptions;
		this.webhookEvents = webhookEvents;
		this.seeder = seeder;
		this.adminAccounts = adminAccounts;
		this.settingsService = settingsService;
		this.props = props;
	}

	@Scheduled(cron = "${demo.reset-cron}", zone = "Europe/Stockholm")
	public void reset() {
		log.info("Demo reset: wiping data and re-seeding…");
		// bulk deletes; child rows go via ON DELETE CASCADE in the schema
		webhookEvents.deleteAllInBatch();
		orders.deleteAllInBatch();
		products.deleteAllInBatch();
		categories.deleteAllInBatch();
		shippingOptions.deleteAllInBatch();
		wipeUploadedProductImages();
		settingsService.refresh();
		seeder.seedIfEmpty();
		adminAccounts.ensureConfiguredAdmin();
		log.info("Demo reset complete");
	}

	private void wipeUploadedProductImages() {
		Path productDir = Paths.get(props.uploadDir()).toAbsolutePath().normalize().resolve("products");
		if (!Files.isDirectory(productDir)) {
			return;
		}
		try (Stream<Path> files = Files.walk(productDir)) {
			files.sorted(Comparator.reverseOrder())
					.filter(path -> !path.equals(productDir))
					.forEach(path -> path.toFile().delete());
		}
		catch (IOException e) {
			log.warn("Could not wipe demo product images: {}", e.getMessage());
		}
	}
}
