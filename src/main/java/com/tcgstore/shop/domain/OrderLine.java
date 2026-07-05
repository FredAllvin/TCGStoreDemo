package com.tcgstore.shop.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * Snapshot of what was bought. Name/label/price are copied at purchase time so
 * order history survives product edits and deletions; variantId is only kept
 * for restocking and may become null if the variant is deleted.
 */
@Entity
public class OrderLine {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id")
	private Order order;

	private Long variantId;
	private String productName;
	private String variantLabel;
	private long unitPriceMinor;
	private int qty;
	private long lineTotalMinor;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Order getOrder() {
		return order;
	}

	public void setOrder(Order order) {
		this.order = order;
	}

	public Long getVariantId() {
		return variantId;
	}

	public void setVariantId(Long variantId) {
		this.variantId = variantId;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public String getVariantLabel() {
		return variantLabel;
	}

	public void setVariantLabel(String variantLabel) {
		this.variantLabel = variantLabel;
	}

	public long getUnitPriceMinor() {
		return unitPriceMinor;
	}

	public void setUnitPriceMinor(long unitPriceMinor) {
		this.unitPriceMinor = unitPriceMinor;
	}

	public int getQty() {
		return qty;
	}

	public void setQty(int qty) {
		this.qty = qty;
	}

	public long getLineTotalMinor() {
		return lineTotalMinor;
	}

	public void setLineTotalMinor(long lineTotalMinor) {
		this.lineTotalMinor = lineTotalMinor;
	}
}
