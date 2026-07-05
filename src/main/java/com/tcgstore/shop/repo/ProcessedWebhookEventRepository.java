package com.tcgstore.shop.repo;

import com.tcgstore.shop.domain.ProcessedWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedWebhookEventRepository extends JpaRepository<ProcessedWebhookEvent, Long> {

	boolean existsByEventId(String eventId);
}
