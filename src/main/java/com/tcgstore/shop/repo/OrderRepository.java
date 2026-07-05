package com.tcgstore.shop.repo;

import com.tcgstore.shop.domain.Order;
import com.tcgstore.shop.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

	Optional<Order> findByOrderNumber(String orderNumber);

	Optional<Order> findByOrderNumberAndAccessToken(String orderNumber, String accessToken);

	Optional<Order> findByPaymentRef(String paymentRef);

	Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

	Page<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);

	List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, Instant cutoff);

	List<Order> findTop8ByOrderByCreatedAtDesc();

	long countByStatus(OrderStatus status);

	@Query("select coalesce(sum(o.totalMinor), 0) from Order o where o.status in :statuses and o.createdAt >= :since")
	long revenueSince(@Param("statuses") Collection<OrderStatus> statuses, @Param("since") Instant since);

	@Query(value = "select nextval('order_number_seq')", nativeQuery = true)
	long nextOrderSequence();
}
