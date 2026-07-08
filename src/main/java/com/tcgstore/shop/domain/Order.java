package com.tcgstore.shop.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Entity
@Table(name = "orders")
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String orderNumber;

	/** Random token letting the (guest) customer view their confirmation page. */
	private String accessToken;

	@Enumerated(EnumType.STRING)
	private OrderStatus status = OrderStatus.PENDING;

	private String customerName;
	private String email;
	private String phone;
	private String addressLine1;
	private String addressLine2;
	private String postalCode;
	private String city;
	private String country;
	private String customerNote;

	private String shippingName;
	private String shippingNameEn;
	private long shippingMinor;
	private long subtotalMinor;
	private long totalMinor;
	private String currency;

	private String paymentProvider;
	private String paymentRef;

	private Instant createdAt = Instant.now();
	private Instant paidAt;
	private Instant shippedAt;
	private Instant cancelledAt;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id asc")
	private List<OrderLine> lines = new ArrayList<>();

	public void addLine(OrderLine line) {
		line.setOrder(this);
		lines.add(line);
	}

	public int itemCount() {
		return lines.stream().mapToInt(OrderLine::getQty).sum();
	}

	/** The shipping name snapshot for the given locale, falling back to the other language. */
	public String shippingNameFor(Locale locale) {
		boolean english = locale != null && "en".equals(locale.getLanguage());
		String preferred = english ? shippingNameEn : shippingName;
		String other = english ? shippingName : shippingNameEn;
		return preferred == null || preferred.isBlank() ? other : preferred;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getOrderNumber() {
		return orderNumber;
	}

	public void setOrderNumber(String orderNumber) {
		this.orderNumber = orderNumber;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public void setStatus(OrderStatus status) {
		this.status = status;
	}

	public String getCustomerName() {
		return customerName;
	}

	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getAddressLine1() {
		return addressLine1;
	}

	public void setAddressLine1(String addressLine1) {
		this.addressLine1 = addressLine1;
	}

	public String getAddressLine2() {
		return addressLine2;
	}

	public void setAddressLine2(String addressLine2) {
		this.addressLine2 = addressLine2;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getCustomerNote() {
		return customerNote;
	}

	public void setCustomerNote(String customerNote) {
		this.customerNote = customerNote;
	}

	public String getShippingName() {
		return shippingName;
	}

	public void setShippingName(String shippingName) {
		this.shippingName = shippingName;
	}

	public String getShippingNameEn() {
		return shippingNameEn;
	}

	public void setShippingNameEn(String shippingNameEn) {
		this.shippingNameEn = shippingNameEn;
	}

	public long getShippingMinor() {
		return shippingMinor;
	}

	public void setShippingMinor(long shippingMinor) {
		this.shippingMinor = shippingMinor;
	}

	public long getSubtotalMinor() {
		return subtotalMinor;
	}

	public void setSubtotalMinor(long subtotalMinor) {
		this.subtotalMinor = subtotalMinor;
	}

	public long getTotalMinor() {
		return totalMinor;
	}

	public void setTotalMinor(long totalMinor) {
		this.totalMinor = totalMinor;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getPaymentProvider() {
		return paymentProvider;
	}

	public void setPaymentProvider(String paymentProvider) {
		this.paymentProvider = paymentProvider;
	}

	public String getPaymentRef() {
		return paymentRef;
	}

	public void setPaymentRef(String paymentRef) {
		this.paymentRef = paymentRef;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getPaidAt() {
		return paidAt;
	}

	public void setPaidAt(Instant paidAt) {
		this.paidAt = paidAt;
	}

	public Instant getShippedAt() {
		return shippedAt;
	}

	public void setShippedAt(Instant shippedAt) {
		this.shippedAt = shippedAt;
	}

	public Instant getCancelledAt() {
		return cancelledAt;
	}

	public void setCancelledAt(Instant cancelledAt) {
		this.cancelledAt = cancelledAt;
	}

	public List<OrderLine> getLines() {
		return lines;
	}
}
