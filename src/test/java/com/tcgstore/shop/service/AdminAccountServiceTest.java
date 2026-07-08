package com.tcgstore.shop.service;

import com.tcgstore.shop.config.AppProperties;
import com.tcgstore.shop.domain.AdminUser;
import com.tcgstore.shop.repo.AdminUserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The env-configured admin is created when missing, but a password changed via
 * /admin/password must survive restarts — except on the demo box, whose nightly
 * reset deliberately restores the throwaway demo credentials.
 */
class AdminAccountServiceTest {

	private final PasswordEncoder encoder = new BCryptPasswordEncoder();
	private final AdminUserRepository repository = mock(AdminUserRepository.class);

	private AdminAccountService service(String envPassword, String... profiles) {
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles(profiles);
		AppProperties props = new AppProperties("target/test-uploads", "http://localhost",
				new AppProperties.Admin("boss", envPassword));
		return new AdminAccountService(repository, encoder, props, environment);
	}

	@Test
	void createsTheConfiguredAdminWhenMissing() {
		when(repository.findByUsernameIgnoreCase("boss")).thenReturn(Optional.empty());

		service("correct horse battery").ensureConfiguredAdmin();

		ArgumentCaptor<AdminUser> saved = ArgumentCaptor.forClass(AdminUser.class);
		verify(repository).save(saved.capture());
		assertEquals("boss", saved.getValue().getUsername());
		assertTrue(encoder.matches("correct horse battery", saved.getValue().getPasswordHash()));
	}

	@Test
	void keepsAPasswordChangedViaTheAdminPanelOutsideDemo() {
		AdminUser existing = new AdminUser();
		existing.setUsername("boss");
		existing.setPasswordHash(encoder.encode("changed-in-the-ui"));
		when(repository.findByUsernameIgnoreCase("boss")).thenReturn(Optional.of(existing));

		service("correct horse battery", "prod").ensureConfiguredAdmin();

		verify(repository, never()).save(any());
	}

	@Test
	void demoProfileRestoresTheConfiguredPassword() {
		AdminUser existing = new AdminUser();
		existing.setUsername("boss");
		existing.setPasswordHash(encoder.encode("changed-by-a-visitor"));
		when(repository.findByUsernameIgnoreCase("boss")).thenReturn(Optional.of(existing));

		service("demo123-env", "prod", "demo").ensureConfiguredAdmin();

		ArgumentCaptor<AdminUser> saved = ArgumentCaptor.forClass(AdminUser.class);
		verify(repository).save(saved.capture());
		assertTrue(encoder.matches("demo123-env", saved.getValue().getPasswordHash()));
	}
}
