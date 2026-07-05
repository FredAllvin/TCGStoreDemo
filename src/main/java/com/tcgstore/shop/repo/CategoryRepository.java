package com.tcgstore.shop.repo;

import com.tcgstore.shop.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

	Optional<Category> findBySlug(String slug);

	boolean existsBySlug(String slug);

	List<Category> findByParentIsNullOrderBySortOrderAscNameAsc();

	List<Category> findAllByOrderBySortOrderAscNameAsc();

	long countByParent(Category parent);
}
