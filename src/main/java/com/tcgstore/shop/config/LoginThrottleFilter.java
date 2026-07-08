package com.tcgstore.shop.config;

import com.tcgstore.shop.service.LoginAttemptService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Rejects admin login attempts from IPs that are currently locked out.
 * Registered inside the security chain by {@link SecurityConfig} (deliberately
 * not a component, so the servlet container does not register it twice).
 */
public class LoginThrottleFilter extends OncePerRequestFilter {

	private final LoginAttemptService attempts;

	public LoginThrottleFilter(LoginAttemptService attempts) {
		this.attempts = attempts;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !("POST".equals(request.getMethod()) && "/admin/login".equals(request.getRequestURI()));
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		if (attempts.isBlocked(request.getRemoteAddr())
				|| attempts.isUsernameBlocked(request.getParameter("username"))) {
			response.sendRedirect("/admin/login?locked");
			return;
		}
		chain.doFilter(request, response);
	}
}
