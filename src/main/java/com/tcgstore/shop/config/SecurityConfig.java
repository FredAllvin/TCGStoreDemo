package com.tcgstore.shop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/admin/login").permitAll()
				.requestMatchers("/admin/**").hasRole("ADMIN")
				.anyRequest().permitAll())
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
