package com.tcgstore.shop.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Entity
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Category category;

	private String name;
	private String slug;
	private String setName;
	private String cardNumber;
	private String rarity;
	private String language;
	private String description;
	private String descriptionEn;
	private boolean active = true;
	private boolean featured;
	private Instant createdAt = Instant.now();

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("sortOrder asc, id asc")
	private List<ProductImage> images = new ArrayList<>();

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id asc")
	private List<ProductVariant> variants = new ArrayList<>();

	// --- display helpers (used by templates) ---

	public String mainImagePath() {
		return images.isEmpty() ? null : images.getFirst().getPath();
	}

	/** The description for the given locale, falling back to the other language. */
	public String descriptionFor(Locale locale) {
		boolean english = locale != null && "en".equals(locale.getLanguage());
		String preferred = english ? descriptionEn : description;
		String other = english ? description : descriptionEn;
		return preferred == null || preferred.isBlank() ? other : preferred;
	}

	public List<ProductVariant> activeVariants() {
		return variants.stream().filter(ProductVariant::isActive).toList();
	}

	public List<ProductVariant> rawVariants() {
		return activeVariants().stream()
				.filter(v -> v.getKind() == VariantKind.RAW)
				.sorted(Comparator.comparing(ProductVariant::getCondition)
						.thenComparing(v -> v.getFinish() == null ? 0 : v.getFinish().ordinal()))
				.toList();
	}

	public List<ProductVariant> gradedVariants() {
		return activeVariants().stream()
				.filter(v -> v.getKind() == VariantKind.GRADED)
				.sorted(Comparator.comparingLong(ProductVariant::getPriceMinor).reversed())
				.toList();
	}

	public List<ProductVariant> standardVariants() {
		return activeVariants().stream().filter(v -> v.getKind() == VariantKind.STANDARD).toList();
	}

	public long minPriceMinor() {
		return activeVariants().stream().mapToLong(ProductVariant::getPriceMinor).min().orElse(0);
	}

	public boolean hasPriceRange() {
		return activeVariants().stream().mapToLong(ProductVariant::getPriceMinor).distinct().count() > 1;
	}

	public boolean inStock() {
		return activeVariants().stream().anyMatch(v -> v.getStockQty() > 0);
	}

	// --- accessors ---

	public void addImage(ProductImage image) {
		image.setProduct(this);
		images.add(image);
	}

	public void addVariant(ProductVariant variant) {
		variant.setProduct(this);
		variants.add(variant);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	public String getSetName() {
		return setName;
	}

	public void setSetName(String setName) {
		this.setName = setName;
	}

	public String getCardNumber() {
		return cardNumber;
	}

	public void setCardNumber(String cardNumber) {
		this.cardNumber = cardNumber;
	}

	public String getRarity() {
		return rarity;
	}

	public void setRarity(String rarity) {
		this.rarity = rarity;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescriptionEn() {
		return descriptionEn;
	}

	public void setDescriptionEn(String descriptionEn) {
		this.descriptionEn = descriptionEn;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public boolean isFeatured() {
		return featured;
	}

	public void setFeatured(boolean featured) {
		this.featured = featured;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public List<ProductImage> getImages() {
		return images;
	}

	public List<ProductVariant> getVariants() {
		return variants;
	}
}
