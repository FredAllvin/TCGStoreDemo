package com.tcgstore.shop.service;

import java.util.Locale;

public final class Slugs {

	private Slugs() {
	}

	public static String slugify(String input) {
		if (input == null) {
			return "";
		}
		String slug = input.toLowerCase(Locale.ROOT)
				.replace("å", "a").replace("ä", "a").replace("ö", "o")
				.replace("é", "e").replace("ü", "u")
				.replaceAll("[^a-z0-9]+", "-")
				.replaceAll("(^-+|-+$)", "");
		return slug.length() > 200 ? slug.substring(0, 200) : slug;
	}
}
