package com.tcgstore.shop.service;

import com.tcgstore.shop.domain.AdminUser;
import com.tcgstore.shop.repo.AdminUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DbUserDetailsService implements UserDetailsService {

	private final AdminUserRepository repository;

	public DbUserDetailsService(AdminUserRepository repository) {
		this.repository = repository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		AdminUser admin = repository.findByUsernameIgnoreCase(username)
				.orElseThrow(() -> new UsernameNotFoundException("No admin: " + username));
		return User.withUsername(admin.getUsername())
				.password(admin.getPasswordHash())
				.roles("ADMIN")
				.build();
	}
}
