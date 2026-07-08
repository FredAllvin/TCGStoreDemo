package com.tcgstore.shop.service;

import com.tcgstore.shop.config.AppProperties;
import com.tcgstore.shop.domain.AdminUser;
import com.tcgstore.shop.repo.AdminUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Service
public class AdminAccountService implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminAccountService.class);

	/** Minimum length required for the ADMIN_PASSWORD that bootstraps a real store. */
	private static final int MIN_PROD_PASSWORD_LENGTH = 12;

	/** Placeholder/example/common values that must never ship to production. */
	private static final Set<String> WEAK_PASSWORDS = Set.of(
			"admin", "password", "changeme", "change-me", "change-me-too", "change-me-please",
			"demo123", "admin123", "secret", "letmein");

	private final AdminUserRepository repository;
	private final PasswordEncoder passwordEncoder;
	private final AppProperties props;
	private final Environment environment;

	public AdminAccountService(AdminUserRepository repository, PasswordEncoder passwordEncoder, AppProperties props,
			Environment environment) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
		this.props = props;
		this.environment = environment;
	}

	@Override
	public void run(ApplicationArguments args) {
		validateProductionCredentials();
		ensureConfiguredAdmin();
	}

	/**
	 * Refuses to start a real (prod, non-demo) store with a blank, default or
	 * trivially short admin password — the pairing that turns any weakening of the
	 * login throttle into an account-takeover path. The public demo (prod,demo) is
	 * exempt: it deliberately runs on throwaway demo/demo123 credentials.
	 */
	private void validateProductionCredentials() {
		boolean prod = environment.acceptsProfiles(Profiles.of("prod"));
		boolean demo = environment.acceptsProfiles(Profiles.of("demo"));
		if (!prod || demo) {
			return;
		}
		String username = props.admin().username();
		String password = props.admin().password();
		if (username == null || username.isBlank() || password == null || password.isBlank()) {
			throw new IllegalStateException(
					"ADMIN_USERNAME and ADMIN_PASSWORD must be set for the prod profile. Configure them in .env.");
		}
		if (password.length() < MIN_PROD_PASSWORD_LENGTH
				|| WEAK_PASSWORDS.contains(password.toLowerCase(Locale.ROOT))) {
			throw new IllegalStateException("ADMIN_PASSWORD is too weak for production: use at least "
					+ MIN_PROD_PASSWORD_LENGTH + " characters and not a common or example value. "
					+ "Set a strong ADMIN_PASSWORD in .env.");
		}
	}

	/**
	 * Creates the admin account configured via ADMIN_USERNAME / ADMIN_PASSWORD.
	 * An existing account's password is left alone (it may have been changed via
	 * /admin/password and must survive restarts) — except in the demo profile,
	 * where the nightly reset restores the throwaway demo credentials.
	 */
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
		AdminUser existing = repository.findByUsernameIgnoreCase(username).orElse(null);
		if (existing == null) {
			AdminUser admin = new AdminUser();
			admin.setUsername(username);
			admin.setPasswordHash(passwordEncoder.encode(password));
			repository.save(admin);
			return;
		}
		if (environment.acceptsProfiles(Profiles.of("demo"))) {
			existing.setPasswordHash(passwordEncoder.encode(password));
			repository.save(existing);
		}
		else if (!passwordEncoder.matches(password, existing.getPasswordHash())) {
			log.info("Admin '{}' already exists with a different password than ADMIN_PASSWORD; keeping the stored "
					+ "one. Change it via /admin/password, or delete the admin_user row to re-bootstrap from .env.",
					username);
		}
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
