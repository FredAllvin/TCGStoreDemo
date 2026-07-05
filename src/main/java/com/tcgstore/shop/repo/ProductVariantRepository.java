package com.tcgstore.shop.repo;

import com.tcgstore.shop.domain.ProductVariant;
import com.tcgstore.shop.domain.VariantKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

	/**
	 * Atomic reservation: only succeeds (returns 1) if enough stock is left.
	 * Runs as a conditional UPDATE so two simultaneous buyers can never
	 * oversell the last copy of a card.
	 */
	@Modifying
	@Query("""
			update ProductVariant v set v.stockQty = v.stockQty - :qty
			where v.id = :id and v.active = true and v.stockQty >= :qty
			""")
	int reserveStock(@Param("id") Long id, @Param("qty") int qty);

	@Modifying
	@Query("update ProductVariant v set v.stockQty = v.stockQty + :qty where v.id = :id")
	int releaseStock(@Param("id") Long id, @Param("qty") int qty);

	List<ProductVariant> findTop10ByActiveTrueAndKindAndStockQtyLessThanEqualOrderByStockQtyAsc(
			VariantKind kind, int threshold);
}
