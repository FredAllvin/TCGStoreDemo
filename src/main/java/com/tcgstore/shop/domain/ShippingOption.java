package com.tcgstore.shop.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.util.Locale;

@Entity
public class ShippingOption {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;
	private String nameEn;
	private long priceMinor;
	private int sortOrder;
	private boolean active = true;

	/** The name for the given locale, falling back to the other language. */
	public String nameFor(Locale locale) {
		boolean english = locale != null && "en".equals(locale.getLanguage());
		String preferred = english ? nameEn : name;
		String other = english ? name : nameEn;
		return preferred == null || preferred.isBlank() ? other : preferred;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNameEn() {
		return nameEn;
	}

	public void setNameEn(String nameEn) {
		this.nameEn = nameEn;
	}

	public long getPriceMinor() {
		return priceMinor;
	}

	public void setPriceMinor(long priceMinor) {
		this.priceMinor = priceMinor;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
}
