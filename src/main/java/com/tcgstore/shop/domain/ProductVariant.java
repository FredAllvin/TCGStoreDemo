package com.tcgstore.shop.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class ProductVariant {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Product product;

	@Enumerated(EnumType.STRING)
	private VariantKind kind = VariantKind.STANDARD;

	@Enumerated(EnumType.STRING)
	private Condition condition;

	@Enumerated(EnumType.STRING)
	private Finish finish;

	@Enumerated(EnumType.STRING)
	private Grader grader;

	private String grade;
	private String certNumber;
	private String sku;
	private long priceMinor;
	private int stockQty;

	/** Optional own photo (e.g. the actual slab); falls back to product images. */
	private String imagePath;
	private boolean active = true;

	/** Canonical label stored on order lines, e.g. "NM · Foil" or "PSA 10 #82634511". */
	public String displayLabel() {
		return switch (kind) {
			case STANDARD -> "";
			case RAW -> {
				String label = condition != null ? condition.name() : "";
				if (finish == Finish.FOIL) {
					label += " · Foil";
				}
				else if (finish == Finish.REVERSE) {
					label += " · Reverse Holo";
				}
				yield label;
			}
			case GRADED -> {
				String label = (grader != null ? grader.name() + " " : "") + (grade != null ? grade : "");
				if (certNumber != null && !certNumber.isBlank()) {
					label += " #" + certNumber;
				}
				yield label.trim();
			}
		};
	}

	public boolean inStock() {
		return stockQty > 0;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
	}

	public VariantKind getKind() {
		return kind;
	}

	public void setKind(VariantKind kind) {
		this.kind = kind;
	}

	public Condition getCondition() {
		return condition;
	}

	public void setCondition(Condition condition) {
		this.condition = condition;
	}

	public Finish getFinish() {
		return finish;
	}

	public void setFinish(Finish finish) {
		this.finish = finish;
	}

	public Grader getGrader() {
		return grader;
	}

	public void setGrader(Grader grader) {
		this.grader = grader;
	}

	public String getGrade() {
		return grade;
	}

	public void setGrade(String grade) {
		this.grade = grade;
	}

	public String getCertNumber() {
		return certNumber;
	}

	public void setCertNumber(String certNumber) {
		this.certNumber = certNumber;
	}

	public String getSku() {
		return sku;
	}

	public void setSku(String sku) {
		this.sku = sku;
	}

	public long getPriceMinor() {
		return priceMinor;
	}

	public void setPriceMinor(long priceMinor) {
		this.priceMinor = priceMinor;
	}

	public int getStockQty() {
		return stockQty;
	}

	public void setStockQty(int stockQty) {
		this.stockQty = stockQty;
	}

	public String getImagePath() {
		return imagePath;
	}

	public void setImagePath(String imagePath) {
		this.imagePath = imagePath;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
}
