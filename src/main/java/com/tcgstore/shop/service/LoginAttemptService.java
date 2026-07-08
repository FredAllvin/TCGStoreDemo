package com.tcgstore.shop.service;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory brute-force protection for the admin login. Two dimensions:
 * per client IP (the common case) and, with a higher ceiling, per username —
 * so an attacker rotating spoofed X-Forwarded-For addresses still cannot
 * hammer one account indefinitely.
 */
@Service
public class LoginAttemptService {

	private static final int MAX_ATTEMPTS_PER_IP = 8;
	private static final int MAX_ATTEMPTS_PER_USERNAME = 20;
	private static final long LOCK_SECONDS = 15 * 60;
	private static final int MAX_TRACKED_KEYS = 10_000;

	private record Attempts(int count, Instant lockedUntil) {
	}

	private final Map<String, Attempts> attemptsByKey = new ConcurrentHashMap<>();

	public boolean isBlocked(String ip) {
		return blocked("ip:" + ip);
	}

	public boolean isUsernameBlocked(String username) {
		return username != null && !username.isBlank() && blocked(usernameKey(username));
	}

	public void recordFailure(String ip) {
		recordFailure("ip:" + ip, MAX_ATTEMPTS_PER_IP);
	}

	public void recordUsernameFailure(String username) {
		if (username != null && !username.isBlank()) {
			recordFailure(usernameKey(username), MAX_ATTEMPTS_PER_USERNAME);
		}
	}

	public void reset(String ip) {
		attemptsByKey.remove("ip:" + ip);
	}

	public void resetUsername(String username) {
		if (username != null && !username.isBlank()) {
			attemptsByKey.remove(usernameKey(username));
		}
	}

	private boolean blocked(String key) {
		Attempts attempts = attemptsByKey.get(key);
		if (attempts == null || attempts.lockedUntil() == null) {
			return false;
		}
		if (attempts.lockedUntil().isBefore(Instant.now())) {
			attemptsByKey.remove(key);
			return false;
		}
		return true;
	}

	private void recordFailure(String key, int maxAttempts) {
		if (attemptsByKey.size() > MAX_TRACKED_KEYS) {
			attemptsByKey.clear();
		}
		attemptsByKey.compute(key, (k, previous) -> {
			int count = previous == null ? 1 : previous.count() + 1;
			Instant lockedUntil = count >= maxAttempts ? Instant.now().plusSeconds(LOCK_SECONDS) : null;
			return new Attempts(count, lockedUntil);
		});
	}

	private static String usernameKey(String username) {
		return "user:" + username.trim().toLowerCase(Locale.ROOT);
	}

	@EventListener
	public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
		String ip = remoteAddress(event.getAuthentication().getDetails());
		if (ip != null) {
			recordFailure(ip);
		}
		recordUsernameFailure(event.getAuthentication().getName());
	}

	@EventListener
	public void onSuccess(AuthenticationSuccessEvent event) {
		String ip = remoteAddress(event.getAuthentication().getDetails());
		if (ip != null) {
			reset(ip);
		}
		resetUsername(event.getAuthentication().getName());
	}

	private String remoteAddress(Object details) {
		return details instanceof WebAuthenticationDetails web ? web.getRemoteAddress() : null;
	}
}
