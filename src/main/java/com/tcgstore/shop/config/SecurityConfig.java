package com.tcgstore.shop.config;

import com.tcgstore.shop.service.LoginAttemptService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	// Locks down where the browser may load scripts/styles/etc. from and forbids
	// framing (clickjacking) and off-site form posts. 'unsafe-inline' is required
	// because the templates use inline event handlers (delete confirmations, the
	// gallery, the variant-kind toggle) and inline brand-colour <style> blocks;
	// tightening it to nonces/hashes would mean refactoring those out first.
	private static final String CONTENT_SECURITY_POLICY = String.join("; ",
			"default-src 'self'",
			"script-src 'self' 'unsafe-inline'",
			"style-src 'self' 'unsafe-inline'",
			"img-src 'self' data:",
			"font-src 'self'",
			"object-src 'none'",
			"base-uri 'self'",
			"frame-ancestors 'none'",
			"form-action 'self'");

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http, LoginAttemptService loginAttempts) throws Exception {
		http
			.addFilterBefore(new LoginThrottleFilter(loginAttempts), UsernamePasswordAuthenticationFilter.class)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/admin/login").permitAll()
				.requestMatchers("/admin/**").hasRole("ADMIN")
				// Only the health probe is meant to be public; require auth for any
				// other actuator endpoint that might get exposed in the future.
				.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
				.requestMatchers("/actuator/**").hasRole("ADMIN")
				.anyRequest().permitAll())
			.headers(headers -> headers
				.contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY)))
			.formLogin(form -> form
				.loginPage("/admin/login")
				.loginProcessingUrl("/admin/login")
				.defaultSuccessUrl("/admin", false)
				.failureUrl("/admin/login?error")
				.permitAll())
			.logout(logout -> logout
				.logoutUrl("/admin/logout")
				.logoutSuccessUrl("/"))
			// Stripe posts webhooks without a CSRF token; authenticity is
			// checked by verifying the webhook signature instead.
			.csrf(csrf -> csrf.ignoringRequestMatchers("/webhooks/stripe"));
		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
