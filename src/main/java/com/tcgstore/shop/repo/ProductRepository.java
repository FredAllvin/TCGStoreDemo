package com.tcgstore.shop.repo;

import com.tcgstore.shop.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

	Optional<Product> findBySlug(String slug);

	Optional<Product> findBySlugAndActiveTrue(String slug);

	boolean existsBySlug(String slug);

	List<Product> findTop8ByActiveTrueAndFeaturedTrueOrderByCreatedAtDesc();

	List<Product> findTop8ByActiveTrueOrderByCreatedAtDesc();

	Page<Product> findByActiveTrueAndCategoryIdIn(Collection<Long> categoryIds, Pageable pageable);

	@Query("""
			select p from Product p
			where p.active = true and p.category.id in :categoryIds
			  and exists (select 1 from ProductVariant v
			              where v.product = p and v.active = true and v.stockQty > 0)
			""")
	Page<Product> findInStockByCategoryIdIn(@Param("categoryIds") Collection<Long> categoryIds, Pageable pageable);

	/**
	 * Matches name, set, tags, card number, and the category tree the product
	 * sits in (both languages) — so "magic" finds every card filed under
	 * Magic: The Gathering. The parent join must stay a LEFT join: an implicit
	 * path like p.category.parent.name would inner-join and silently drop all
	 * products in root-level categories from the results.
	 *
	 * @param pattern the LIKE pattern with wildcards escaped, see CatalogService.likePattern
	 */
	@Query("""
			select p from Product p
			join p.category c
			left join c.parent parent
			where p.active = true and (
			      lower(p.name) like lower(:pattern) escape '!'
			   or lower(p.setName) like lower(:pattern) escape '!'
			   or lower(p.tags) like lower(:pattern) escape '!'
			   or lower(c.name) like lower(:pattern) escape '!'
			   or lower(c.nameEn) like lower(:pattern) escape '!'
			   or lower(parent.name) like lower(:pattern) escape '!'
			   or lower(parent.nameEn) like lower(:pattern) escape '!'
			   or lower(p.cardNumber) = lower(:q))
			""")
	Page<Product> search(@Param("q") String q, @Param("pattern") String pattern, Pageable pageable);

	// admin listing
	Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

	long countByCategoryId(Long categoryId);
}
