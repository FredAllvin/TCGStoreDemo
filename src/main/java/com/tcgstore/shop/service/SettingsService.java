package com.tcgstore.shop.service;

import com.tcgstore.shop.domain.StoreSettings;
import com.tcgstore.shop.repo.StoreSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The settings row is read on every page render, so it is cached in memory
 * and refreshed whenever the admin saves changes.
 */
@Service
public class SettingsService {

	private final StoreSettingsRepository repository;
	private volatile StoreSettings cached;

	public SettingsService(StoreSettingsRepository repository) {
		this.repository = repository;
	}

	public StoreSettings get() {
		StoreSettings settings = cached;
		if (settings == null) {
			settings = repository.findById(1L)
					.orElseThrow(() -> new IllegalStateException("store_settings row missing — check Flyway migration"));
			cached = settings;
		}
		return settings;
	}

	@Transactional
	public void save(StoreSettings settings) {
		settings.setId(1L);
		cached = repository.save(settings);
	}

	public void refresh() {
		cached = null;
	}
}
