package com.tcgstore.shop.repo;

import com.tcgstore.shop.domain.ShippingOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShippingOptionRepository extends JpaRepository<ShippingOption, Long> {

	List<ShippingOption> findByActiveTrueOrderBySortOrderAsc();

	List<ShippingOption> findAllByOrderBySortOrderAsc();
}
