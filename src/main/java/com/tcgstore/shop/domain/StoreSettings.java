package com.tcgstore.shop.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Locale;

/** Singleton row (id = 1) holding all per-store branding and configuration. */
@Entity
@Table(name = "store_settings")
public class StoreSettings {

	@Id
	private Long id;

	private String storeName;
	private String tagline;
	private String taglineEn;
	private String logoPath;
	private String primaryColor;
	private String accentColor;
	private String currency;
	private String defaultLocale;
	private BigDecimal eurDisplayRate;
	private String contactEmail;
	private String contactPhone;
	private String addressLine;
	private String instagramUrl;
	private String facebookUrl;
	private String discordUrl;
	private Long freeShipThresholdMinor;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getStoreName() {
		return storeName;
	}

	public void setStoreName(String storeName) {
		this.storeName = storeName;
	}

	public String getTagline() {
		return tagline;
	}

	public void setTagline(String tagline) {
		this.tagline = tagline;
	}

	public String getTaglineEn() {
		return taglineEn;
	}

	public void setTaglineEn(String taglineEn) {
		this.taglineEn = taglineEn;
	}

	/** The tagline for the given locale, falling back to the other language. */
	public String taglineFor(Locale locale) {
		boolean english = locale != null && "en".equals(locale.getLanguage());
		String preferred = english ? taglineEn : tagline;
		String other = english ? tagline : taglineEn;
		return preferred == null || preferred.isBlank() ? other : preferred;
	}

	public String getLogoPath() {
		return logoPath;
	}

	public void setLogoPath(String logoPath) {
		this.logoPath = logoPath;
	}

	public String getPrimaryColor() {
		return primaryColor;
	}

	public void setPrimaryColor(String primaryColor) {
		this.primaryColor = primaryColor;
	}

	public String getAccentColor() {
		return accentColor;
	}

	public void setAccentColor(String accentColor) {
		this.accentColor = accentColor;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getDefaultLocale() {
		return defaultLocale;
	}

	public void setDefaultLocale(String defaultLocale) {
		this.defaultLocale = defaultLocale;
	}

	public BigDecimal getEurDisplayRate() {
		return eurDisplayRate;
	}

	public void setEurDisplayRate(BigDecimal eurDisplayRate) {
		this.eurDisplayRate = eurDisplayRate;
	}

	public String getContactEmail() {
		return contactEmail;
	}

	public void setContactEmail(String contactEmail) {
		this.contactEmail = contactEmail;
	}

	public String getContactPhone() {
		return contactPhone;
	}

	public void setContactPhone(String contactPhone) {
		this.contactPhone = contactPhone;
	}

	public String getAddressLine() {
		return addressLine;
	}

	public void setAddressLine(String addressLine) {
		this.addressLine = addressLine;
	}

	public String getInstagramUrl() {
		return instagramUrl;
	}

	public void setInstagramUrl(String instagramUrl) {
		this.instagramUrl = instagramUrl;
	}

	public String getFacebookUrl() {
		return facebookUrl;
	}

	public void setFacebookUrl(String facebookUrl) {
		this.facebookUrl = facebookUrl;
	}

	public String getDiscordUrl() {
		return discordUrl;
	}

	public void setDiscordUrl(String discordUrl) {
		this.discordUrl = discordUrl;
	}

	public Long getFreeShipThresholdMinor() {
		return freeShipThresholdMinor;
	}

	public void setFreeShipThresholdMinor(Long freeShipThresholdMinor) {
		this.freeShipThresholdMinor = freeShipThresholdMinor;
	}
}
