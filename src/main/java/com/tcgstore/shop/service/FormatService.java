package com.tcgstore.shop.service;

import com.tcgstore.shop.domain.StoreSettings;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.Locale;

/**
 * Formatting helpers exposed to templates as the "fmt" bean, e.g.
 * ${@fmt.money(variant.priceMinor)}. Prices are stored as long minor units
 * (öre/cents) and only ever converted for display.
 */
@Service("fmt")
public class FormatService {

	private static final ZoneId STORE_ZONE = ZoneId.of("Europe/Stockholm");
	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(STORE_ZONE);
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(STORE_ZONE);

	private final SettingsService settingsService;

	public FormatService(SettingsService settingsService) {
		this.settingsService = settingsService;
	}

	public String money(long minor) {
		StoreSettings settings = settingsService.get();
		return formatCurrency(minor, settings.getCurrency(), LocaleContextHolder.getLocale());
	}

	/**
	 * Approximate EUR display price (never charged) when the store has
	 * configured a display rate and its own currency is not EUR.
	 */
	public String moneyEur(long minor) {
		StoreSettings settings = settingsService.get();
		if (settings.getEurDisplayRate() == null || "EUR".equalsIgnoreCase(settings.getCurrency())) {
			return null;
		}
		Currency base = Currency.getInstance(settings.getCurrency());
		BigDecimal eur = BigDecimal.valueOf(minor)
				.movePointLeft(base.getDefaultFractionDigits())
				.multiply(settings.getEurDisplayRate());
		NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en"));
		nf.setCurrency(Currency.getInstance("EUR"));
		return "≈ " + nf.format(eur);
	}

	public String currency(long minor, String currencyCode) {
		return formatCurrency(minor, currencyCode, LocaleContextHolder.getLocale());
	}

	public String dateTime(Instant instant) {
		return instant == null ? "" : DATE_TIME.format(instant);
	}

	public String date(Instant instant) {
		return instant == null ? "" : DATE.format(instant);
	}

	private String formatCurrency(long minor, String currencyCode, Locale locale) {
		Currency currency = Currency.getInstance(currencyCode);
		NumberFormat nf = NumberFormat.getCurrencyInstance(locale);
		nf.setCurrency(currency);
		return nf.format(BigDecimal.valueOf(minor).movePointLeft(currency.getDefaultFractionDigits()));
	}
}
