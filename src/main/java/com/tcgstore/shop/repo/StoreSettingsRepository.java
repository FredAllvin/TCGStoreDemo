package com.tcgstore.shop.repo;

import com.tcgstore.shop.domain.StoreSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreSettingsRepository extends JpaRepository<StoreSettings, Long> {
}
