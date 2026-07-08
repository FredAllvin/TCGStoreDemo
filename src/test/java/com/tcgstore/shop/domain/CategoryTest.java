package com.tcgstore.shop.domain;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** nameFor mirrors Product.descriptionFor: prefer the requested language, fall back to the other. */
class CategoryTest {

	private static Category cat(String name, String nameEn) {
		Category category = new Category();
		category.setName(name);
		category.setNameEn(nameEn);
		return category;
	}

	@Test
	void prefersTheRequestedLanguage() {
		Category category = cat("Tillbehör", "Accessories");
		assertEquals("Accessories", category.nameFor(Locale.ENGLISH));
		assertEquals("Accessories", category.nameFor(Locale.of("en", "GB")));
		assertEquals("Tillbehör", category.nameFor(Locale.of("sv", "SE")));
	}

	@Test
	void fallsBackToSwedishWhenTheEnglishNameIsMissing() {
		assertEquals("Tillbehör", cat("Tillbehör", null).nameFor(Locale.ENGLISH));
		assertEquals("Tillbehör", cat("Tillbehör", " ").nameFor(Locale.ENGLISH));
	}

	@Test
	void fallsBackToEnglishWhenTheSwedishNameIsMissing() {
		assertEquals("Accessories", cat(null, "Accessories").nameFor(Locale.of("sv", "SE")));
	}

	@Test
	void nullLocaleMeansSwedish() {
		assertEquals("Tillbehör", cat("Tillbehör", "Accessories").nameFor(null));
	}
}
