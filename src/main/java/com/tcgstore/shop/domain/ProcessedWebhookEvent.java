package com.tcgstore.shop.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.Instant;

/** Records handled payment-webhook event ids so replays and retries are no-ops. */
@Entity
public class ProcessedWebhookEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String eventId;
	private Instant processedAt = Instant.now();

	public ProcessedWebhookEvent() {
	}

	public ProcessedWebhookEvent(String eventId) {
		this.eventId = eventId;
	}

	public Long getId() {
		return id;
	}

	public String getEventId() {
		return eventId;
	}

	public void setEventId(String eventId) {
		this.eventId = eventId;
	}

	public Instant getProcessedAt() {
		return processedAt;
	}

	public void setProcessedAt(Instant processedAt) {
		this.processedAt = processedAt;
	}
}
