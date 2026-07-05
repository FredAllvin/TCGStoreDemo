package com.tcgstore.shop.repo;

import com.tcgstore.shop.domain.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

	Optional<AdminUser> findByUsernameIgnoreCase(String username);
}
