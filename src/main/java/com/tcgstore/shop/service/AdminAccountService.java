package com.tcgstore.shop.service;

import com.tcgstore.shop.config.AppProperties;
import com.tcgstore.shop.domain.AdminUser;
import com.tcgstore.shop.repo.AdminUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAccountService implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminAccountService.class);

	private final AdminUserRepository repository;
	private final PasswordEncoder passwordEncoder;
	private final AppProperties props;

	public AdminAccountService(AdminUserRepository repository, PasswordEncoder passwordEncoder, AppProperties props) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
		this.props = props;
	}

	@Override
	public void run(ApplicationArguments args) {
		ensureConfiguredAdmin();
	}

	/** Creates or updates the admin account configured via ADMIN_USERNAME / ADMIN_PASSWORD. */
	@Transactional
	public void ensureConfiguredAdmin() {
		String username = props.admin().username();
		String password = props.admin().password();
		if (username == null || username.isBlank() || password == null || password.isBlank()) {
			if (repository.count() == 0) {
				log.error("No admin account exists and ADMIN_USERNAME/ADMIN_PASSWORD are not set — "
						+ "the admin panel will be unreachable until they are configured.");
			}
			return;
		}
		AdminUser admin = repository.findByUsernameIgnoreCase(username).orElseGet(AdminUser::new);
		admin.setUsername(username);
		admin.setPasswordHash(passwordEncoder.encode(password));
		repository.save(admin);
	}

	@Transactional
	public boolean changePassword(String username, String currentPassword, String newPassword) {
		AdminUser admin = repository.findByUsernameIgnoreCase(username).orElse(null);
		if (admin == null || !passwordEncoder.matches(currentPassword, admin.getPasswordHash())) {
			return false;
		}
		admin.setPasswordHash(passwordEncoder.encode(newPassword));
		repository.save(admin);
		return true;
	}
}
