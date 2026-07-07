package com.tcgstore.shop.demo;

import com.tcgstore.shop.config.AppProperties;
import com.tcgstore.shop.domain.Category;
import com.tcgstore.shop.domain.Condition;
import com.tcgstore.shop.domain.Finish;
import com.tcgstore.shop.domain.Grader;
import com.tcgstore.shop.domain.Product;
import com.tcgstore.shop.domain.ProductImage;
import com.tcgstore.shop.domain.ProductVariant;
import com.tcgstore.shop.domain.ShippingOption;
import com.tcgstore.shop.domain.StoreSettings;
import com.tcgstore.shop.domain.VariantKind;
import com.tcgstore.shop.repo.CategoryRepository;
import com.tcgstore.shop.repo.ProductRepository;
import com.tcgstore.shop.repo.ShippingOptionRepository;
import com.tcgstore.shop.repo.StoreSettingsRepository;
import com.tcgstore.shop.service.SettingsService;
import com.tcgstore.shop.service.Slugs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Fills the store with placeholder demo data on first start (dev/demo profiles only). */
@Component
@Profile({"dev", "demo"})
public class DemoDataSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

	private static final Color POKEMON = new Color(0x2A75BB);
	private static final Color MAGIC = new Color(0x7A4A2B);
	private static final Color YUGIOH = new Color(0x6D4A9E);
	private static final Color ONEPIECE = new Color(0xB03A2E);
	private static final Color ACCESSORY = new Color(0x50606E);

	private final ProductRepository products;
	private final CategoryRepository categories;
	private final ShippingOptionRepository shippingOptions;
	private final StoreSettingsRepository settingsRepository;
	private final SettingsService settingsService;
	private final AppProperties props;

	private int createdAtOffset;

	public DemoDataSeeder(ProductRepository products, CategoryRepository categories,
			ShippingOptionRepository shippingOptions, StoreSettingsRepository settingsRepository,
			SettingsService settingsService, AppProperties props) {
		this.products = products;
		this.categories = categories;
		this.shippingOptions = shippingOptions;
		this.settingsRepository = settingsRepository;
		this.settingsService = settingsService;
		this.props = props;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		seedIfEmpty();
	}

	@Transactional
	public void seedIfEmpty() {
		if (products.count() > 0) {
			return;
		}
		log.info("Seeding demo catalog…");
		seedSettings();
		seedShipping();
		seedCatalog();
		log.info("Demo catalog seeded: {} products", products.count());
	}

	private void seedSettings() {
		StoreSettings s = settingsRepository.findById(1L).orElseThrow();
		s.setStoreName("Kortbutiken");
		s.setTagline("Singles, sealed & tillbehör för samlare");
		s.setPrimaryColor("#1f2a44");
		s.setAccentColor("#e8590c");
		s.setCurrency("SEK");
		s.setDefaultLocale("sv");
		s.setEurDisplayRate(new BigDecimal("0.088"));
		s.setContactEmail("info@kortbutiken.example");
		s.setContactPhone("070-123 45 67");
		s.setAddressLine("Kortgatan 12, 123 45 Stockholm");
		s.setInstagramUrl("https://instagram.com/example");
		s.setFreeShipThresholdMinor(100_000L); // fri frakt över 1 000 kr
		settingsRepository.save(s);
		settingsService.refresh();
	}

	private void seedShipping() {
		if (shippingOptions.count() > 0) {
			return;
		}
		ship("Postbrev (ej spårbart)", 2_900, 0);
		ship("PostNord spårbart paket", 6_900, 1);
		ship("Hämta i butik", 0, 2);
	}

	private void ship(String name, long priceMinor, int sort) {
		ShippingOption option = new ShippingOption();
		option.setName(name);
		option.setPriceMinor(priceMinor);
		option.setSortOrder(sort);
		shippingOptions.save(option);
	}

	private void seedCatalog() {
		Category pokemon = cat("Pokémon", null, 0);
		Category pokemonSingles = cat("Singles", pokemon, 0);
		Category pokemonSealed = cat("Sealed", pokemon, 1);
		Category magic = cat("Magic: The Gathering", null, 1);
		Category magicSingles = cat("Singles", magic, 0);
		Category magicSealed = cat("Sealed", magic, 1);
		Category yugioh = cat("Yu-Gi-Oh!", null, 2);
		Category yugiohSingles = cat("Singles", yugioh, 0);
		Category yugiohSealed = cat("Sealed", yugioh, 1);
		Category onePiece = cat("One Piece", null, 3);
		Category onePieceSingles = cat("Singles", onePiece, 0);
		Category onePieceSealed = cat("Sealed", onePiece, 1);
		Category accessories = cat("Tillbehör", null, 4);

		// --- Pokémon singles ---
		Product p = single(pokemonSingles, "Pikachu ex", "Surging Sparks", "057/191", "Double Rare", "EN",
				"Populärt samlarkort i toppskick, direkt från display.",
				"Popular collector card in top condition, straight from a display.", true, POKEMON);
		raw(p, Condition.NM, Finish.NONFOIL, 4_500, 2);
		raw(p, Condition.LP, Finish.NONFOIL, 3_500, 1);

		p = single(pokemonSingles, "Charizard ex", "Obsidian Flames", "125/197", "Special Illustration Rare", "EN",
				"Eftertraktat Charizard-kort. Grade-kandidat i NM.",
				"Sought-after Charizard card. Grading candidate in NM.", true, POKEMON);
		raw(p, Condition.NM, Finish.NONFOIL, 89_500, 1);
		raw(p, Condition.MP, Finish.NONFOIL, 62_000, 1);
		graded(p, Grader.PSA, "9", "82634511", 189_500);

		p = single(pokemonSingles, "Gardevoir ex", "Scarlet & Violet", "086/198", "Double Rare", "EN",
				"Meta-kort för turneringsspelare.",
				"Meta card for tournament players.", false, POKEMON);
		raw(p, Condition.NM, Finish.NONFOIL, 12_500, 3);
		raw(p, Condition.LP, Finish.NONFOIL, 9_500, 2);

		p = single(pokemonSingles, "Umbreon VMAX (Alt Art)", "Evolving Skies", "215/203", "Alternate Art Secret", "EN",
				"En av seriens mest eftertraktade alt arts – gradad PSA 10.",
				"One of the set's most sought-after alt arts – graded PSA 10.", true, POKEMON);
		graded(p, Grader.PSA, "10", "71034982", 2_495_000);

		p = single(pokemonSingles, "Iono (Full Art)", "Paldea Evolved", "185/193", "Ultra Rare", "EN",
				"Populär trainer i full art.",
				"Popular trainer in full art.", false, POKEMON);
		raw(p, Condition.NM, Finish.NONFOIL, 8_900, 2);

		p = single(pokemonSingles, "Snorlax", "151", "143/165", "Rare", "EN",
				"Klassiker ur 151-serien.",
				"A classic from the 151 set.", false, POKEMON);
		raw(p, Condition.NM, Finish.NONFOIL, 1_500, 8);
		raw(p, Condition.NM, Finish.REVERSE, 2_500, 3);

		// --- Pokémon sealed ---
		sealed(pokemonSealed, "Surging Sparks Booster Box (36-pack)", "Surging Sparks", "EN",
				"Obruten booster box, 36 paket.",
				"Unopened booster box, 36 packs.", true, 549_500, 3, POKEMON);
		sealed(pokemonSealed, "Prismatic Evolutions Elite Trainer Box", "Prismatic Evolutions", "EN",
				"ETB med 9 paket och tillbehör.",
				"ETB with 9 packs and accessories.", false, 129_500, 5, POKEMON);
		sealed(pokemonSealed, "151 Booster Bundle", "151", "EN",
				"6 paket ur den populära 151-serien.",
				"6 packs from the popular 151 set.", false, 64_900, 7, POKEMON);
		sealed(pokemonSealed, "Paldea Evolved Booster (löspaket)", "Paldea Evolved", "EN",
				"Enskilt boosterpaket.",
				"Single booster pack.", false, 5_900, 40, POKEMON);

		// --- Magic singles ---
		p = single(magicSingles, "Ragavan, Nimble Pilferer", "Modern Horizons 2", "138", "Mythic", "EN",
				"Modern-staple. Även gradad BGS 9.5 i lager.",
				"Modern staple. Graded BGS 9.5 also in stock.", true, MAGIC);
		raw(p, Condition.NM, Finish.NONFOIL, 79_500, 2);
		raw(p, Condition.LP, Finish.NONFOIL, 65_000, 1);
		graded(p, Grader.BGS, "9.5", "0012345678", 165_000);

		p = single(magicSingles, "Counterspell", "Commander Masters", "081", "Uncommon", "EN",
				"Klassisk counter – hörnsten i varje blå lek.",
				"The classic counter – a cornerstone of every blue deck.", false, MAGIC);
		raw(p, Condition.NM, Finish.NONFOIL, 2_500, 12);

		p = single(magicSingles, "The One Ring", "The Lord of the Rings: Tales of Middle-earth", "246", "Mythic", "EN",
				"En ring att styra dem alla.",
				"One ring to rule them all.", true, MAGIC);
		raw(p, Condition.NM, Finish.NONFOIL, 189_500, 1);

		p = single(magicSingles, "Sol Ring", "Commander 2021", "263", "Uncommon", "EN",
				"Commander-staple nummer ett.",
				"The number one Commander staple.", false, MAGIC);
		raw(p, Condition.NM, Finish.NONFOIL, 1_900, 20);
		raw(p, Condition.LP, Finish.NONFOIL, 1_200, 6);

		p = single(magicSingles, "Wrenn and Six", "Modern Horizons", "217", "Mythic", "EN",
				"Kraftfull planeswalker för Modern.",
				"Powerful planeswalker for Modern.", false, MAGIC);
		raw(p, Condition.NM, Finish.NONFOIL, 45_000, 2);
		raw(p, Condition.MP, Finish.NONFOIL, 32_000, 1);

		// --- Magic sealed ---
		sealed(magicSealed, "Bloomburrow Play Booster Box", "Bloomburrow", "EN",
				"36 play boosters.",
				"36 play boosters.", false, 449_500, 4, MAGIC);
		sealed(magicSealed, "Modern Horizons 3 Collector Booster", "Modern Horizons 3", "EN",
				"Enskild collector booster.",
				"Single collector booster.", false, 89_500, 6, MAGIC);
		sealed(magicSealed, "Duskmourn Bundle", "Duskmourn: House of Horror", "EN",
				"9 paket + tillbehör.",
				"9 packs + accessories.", false, 54_900, 5, MAGIC);

		// --- Yu-Gi-Oh! singles ---
		p = single(yugiohSingles, "Blue-Eyes White Dragon", "Legend of Blue Eyes White Dragon", "LOB-001", "Ultra Rare", "EN",
				"Ikonen. 1st edition-marknadens kung – LP eller gradad PSA 8.",
				"The icon. King of the 1st edition market – LP or graded PSA 8.", true, YUGIOH);
		raw(p, Condition.LP, Finish.NONFOIL, 350_000, 1);
		graded(p, Grader.PSA, "8", "45120963", 890_000);

		p = single(yugiohSingles, "Ash Blossom & Joyous Spring", "Rarity Collection", "RA01-EN008", "Secret Rare", "EN",
				"Handtrap-staple i varje meta-lek.",
				"Hand trap staple in every meta deck.", false, YUGIOH);
		raw(p, Condition.NM, Finish.NONFOIL, 15_900, 3);

		p = single(yugiohSingles, "Snake-Eye Ash", "Phantom Nightmare", "PHNI-EN005", "Secret Rare", "EN",
				"Nyckelkort i Snake-Eye-strategin.",
				"Key card in the Snake-Eye strategy.", false, YUGIOH);
		raw(p, Condition.NM, Finish.NONFOIL, 8_900, 4);

		p = single(yugiohSingles, "Nibiru, the Primal Being", "Rarity Collection II", "RA02-EN012", "Ultimate Rare", "EN",
				"Board breaker som vinner matcher.",
				"A board breaker that wins games.", false, YUGIOH);
		raw(p, Condition.NM, Finish.NONFOIL, 12_500, 2);

		// --- Yu-Gi-Oh! sealed ---
		sealed(yugiohSealed, "Rarity Collection II Booster Box", "Rarity Collection II", "EN",
				"24 paket fyllda med staples.",
				"24 packs full of staples.", false, 189_500, 3, YUGIOH);
		sealed(yugiohSealed, "Legacy of Destruction Booster", "Legacy of Destruction", "EN",
				"Enskilt boosterpaket.",
				"Single booster pack.", false, 4_500, 30, YUGIOH);

		// --- One Piece singles ---
		p = single(onePieceSingles, "Monkey.D.Luffy (Leader)", "Awakening of the New Era", "OP05-060", "Leader", "EN",
				"Populär leader ur OP05.",
				"Popular leader from OP05.", true, ONEPIECE);
		raw(p, Condition.NM, Finish.NONFOIL, 25_000, 2);

		p = single(onePieceSingles, "Shanks", "Romance Dawn", "OP01-120", "Secret Rare", "EN",
				"Chase-kortet ur OP01. Även gradad CGC 9.5.",
				"The chase card of OP01. Graded CGC 9.5 also in stock.", false, ONEPIECE);
		raw(p, Condition.NM, Finish.NONFOIL, 89_500, 1);
		graded(p, Grader.CGC, "9.5", "4067812", 195_000);

		p = single(onePieceSingles, "Nami (Alt Art)", "Pillars of Strength", "OP03-040", "Super Rare", "EN",
				"Eftertraktad alt art.",
				"Sought-after alt art.", false, ONEPIECE);
		raw(p, Condition.NM, Finish.NONFOIL, 45_000, 1);

		// --- One Piece sealed ---
		sealed(onePieceSealed, "OP-09 Booster Box (JP)", "The Four Emperors", "JP",
				"Japansk booster box, 24 paket.",
				"Japanese booster box, 24 packs.", false, 99_500, 8, ONEPIECE);
		sealed(onePieceSealed, "Starter Deck: 3D2Y", "3D2Y", "EN",
				"Komplett startlek.",
				"Complete starter deck.", false, 19_900, 6, ONEPIECE);

		// --- Accessories ---
		accessory(accessories, "Dragon Shield Matte – Midnight Blue (100)",
				"100 sleeves i standardstorlek.",
				"100 standard-size sleeves.", 12_900, 25);
		accessory(accessories, "Ultra Pro Toploader 3\"x4\" (25-pack)",
				"Skydda dina dyraste kort.",
				"Protect your most valuable cards.", 4_900, 40);
		accessory(accessories, "Samlarpärm 9-pocket (360 kort)",
				"Sidladdad pärm med dragkedja.",
				"Side-loading binder with zipper.", 24_900, 10);
		accessory(accessories, "Spelmatta – svart neutral",
				"Slitstark spelmatta 61×35 cm.",
				"Durable playmat, 61×35 cm.", 19_900, 15);
		accessory(accessories, "Deck Box – röd (100+)",
				"Rymmer 100 sleevade kort.",
				"Holds 100 sleeved cards.", 3_900, 30);
	}

	// --- helpers ---

	private Category cat(String name, Category parent, int sort) {
		Category category = new Category();
		category.setName(name);
		category.setParent(parent);
		category.setSortOrder(sort);
		String slug = parent == null ? Slugs.slugify(name) : Slugs.slugify(parent.getName() + "-" + name);
		category.setSlug(slug);
		return categories.save(category);
	}

	private Product single(Category category, String name, String set, String number, String rarity, String language,
			String description, String descriptionEn, boolean featured, Color color) {
		Product product = baseProduct(category, name, set, number, rarity, language, description, descriptionEn,
				featured);
		attachImage(product, name, set, color);
		return products.save(product);
	}

	private void sealed(Category category, String name, String set, String language, String description,
			String descriptionEn, boolean featured, long priceMinor, int stock, Color color) {
		Product product = baseProduct(category, name, set, null, null, language, description, descriptionEn, featured);
		standard(product, priceMinor, stock);
		attachImage(product, name, set, color);
		products.save(product);
	}

	private void accessory(Category category, String name, String description, String descriptionEn, long priceMinor,
			int stock) {
		Product product = baseProduct(category, name, null, null, null, null, description, descriptionEn, false);
		standard(product, priceMinor, stock);
		attachImage(product, name, "Tillbehör", ACCESSORY);
		products.save(product);
	}

	private Product baseProduct(Category category, String name, String set, String number, String rarity,
			String language, String description, String descriptionEn, boolean featured) {
		Product product = new Product();
		product.setCategory(category);
		product.setName(name);
		product.setSetName(set);
		product.setCardNumber(number);
		product.setRarity(rarity);
		product.setLanguage(language);
		product.setTags(gameTags(category));
		product.setDescription(description);
		product.setDescriptionEn(descriptionEn);
		product.setFeatured(featured);
		// spread creation times so "recently added" looks alive
		product.setCreatedAt(Instant.now().minus(createdAtOffset++, ChronoUnit.HOURS));
		String slugBase = Slugs.slugify(number == null ? name : name + "-" + number);
		String slug = slugBase;
		int i = 2;
		while (products.existsBySlug(slug)) {
			slug = slugBase + "-" + i++;
		}
		product.setSlug(slug);
		return product;
	}

	/** Search tags from the root category, so e.g. "pokemon" finds Pokémon items despite the é. */
	private static String gameTags(Category category) {
		Category root = category;
		while (root.getParent() != null) {
			root = root.getParent();
		}
		return switch (root.getName()) {
			case "Pokémon" -> "pokemon";
			case "Magic: The Gathering" -> "magic, mtg";
			case "Yu-Gi-Oh!" -> "yugioh, yu-gi-oh";
			case "One Piece" -> "one piece";
			case "Tillbehör" -> "tillbehor, accessories";
			default -> null;
		};
	}

	private void raw(Product product, Condition condition, Finish finish, long priceMinor, int stock) {
		ProductVariant variant = new ProductVariant();
		variant.setKind(VariantKind.RAW);
		variant.setCondition(condition);
		variant.setFinish(finish);
		variant.setPriceMinor(priceMinor);
		variant.setStockQty(stock);
		product.addVariant(variant);
		products.save(product);
	}

	private void graded(Product product, Grader grader, String grade, String cert, long priceMinor) {
		ProductVariant variant = new ProductVariant();
		variant.setKind(VariantKind.GRADED);
		variant.setGrader(grader);
		variant.setGrade(grade);
		variant.setCertNumber(cert);
		variant.setPriceMinor(priceMinor);
		variant.setStockQty(1);
		product.addVariant(variant);
		products.save(product);
	}

	private void standard(Product product, long priceMinor, int stock) {
		ProductVariant variant = new ProductVariant();
		variant.setKind(VariantKind.STANDARD);
		variant.setPriceMinor(priceMinor);
		variant.setStockQty(stock);
		product.addVariant(variant);
	}

	private void attachImage(Product product, String title, String subtitle, Color color) {
		String fileName = "products/" + Slugs.slugify(title + (subtitle != null ? "-" + subtitle : "")) + ".png";
		Path target = Paths.get(props.uploadDir()).resolve(fileName);
		try {
			if (!target.toFile().exists()) {
				PlaceholderImages.generateCard(target, title, subtitle, color);
			}
			ProductImage image = new ProductImage();
			image.setPath(fileName);
			image.setAlt(title);
			product.addImage(image);
		}
		catch (IOException e) {
			log.warn("Could not generate placeholder image for {}: {}", title, e.getMessage());
		}
	}
}
