package com.tcgstore.shop.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SlugsAndMoneyTest {

	@Test
	void slugifiesSwedishAndSymbols() {
		assertEquals("pikachu-ex-057-191", Slugs.slugify("Pikachu ex 057/191"));
		assertEquals("tillbehor-parm", Slugs.slugify("Tillbehör & Pärm"));
		assertEquals("monkey-d-luffy", Slugs.slugify("Monkey.D.Luffy!!"));
		assertEquals("", Slugs.slugify(null));
	}

	@Test
	void parsesPricesInKronorToMinorUnits() {
		assertEquals(4500, FormatService.parseMinor("45"));
		assertEquals(4550, FormatService.parseMinor("45,50"));
		assertEquals(4550, FormatService.parseMinor("45.5"));
		assertEquals(123456, FormatService.parseMinor(" 1 234,56 "));
		assertEquals(0, FormatService.parseMinor("0"));
	}

	@Test
	void rejectsNonNumericPrices() {
		assertThrows(RuntimeException.class, () -> FormatService.parseMinor("abc"));
		assertThrows(RuntimeException.class, () -> FormatService.parseMinor("45,999")); // fractional öre
	}
}
