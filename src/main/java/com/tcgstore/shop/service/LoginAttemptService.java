package com.tcgstore.shop.service;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory brute-force protection for the admin login: after too
 * many failed attempts from one IP, that IP is locked out for a while.
 */
@Service
public class LoginAttemptService {

	private static final int MAX_ATTEMPTS = 8;
	private static final long LOCK_SECONDS = 15 * 60;
	private static final int MAX_TRACKED_IPS = 10_000;

	private record Attempts(int count, Instant lockedUntil) {
	}

	private final Map<String, Attempts> attemptsByIp = new ConcurrentHashMap<>();

	public boolean isBlocked(String ip) {
		Attempts attempts = attemptsByIp.get(ip);
		if (attempts == null || attempts.lockedUntil() == null) {
			return false;
		}
		if (attempts.lockedUntil().isBefore(Instant.now())) {
			attemptsByIp.remove(ip);
			return false;
		}
		return true;
	}

	public void recordFailure(String ip) {
		if (attemptsByIp.size() > MAX_TRACKED_IPS) {
			attemptsByIp.clear();
		}
		attemptsByIp.compute(ip, (key, previous) -> {
			int count = previous == null ? 1 : previous.count() + 1;
			Instant lockedUntil = count >= MAX_ATTEMPTS ? Instant.now().plusSeconds(LOCK_SECONDS) : null;
			return new Attempts(count, lockedUntil);
		});
	}

	public void reset(String ip) {
		attemptsByIp.remove(ip);
	}

	@EventListener
	public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
		String ip = remoteAddress(event.getAuthentication().getDetails());
		if (ip != null) {
			recordFailure(ip);
		}
	}

	@EventListener
	public void onSuccess(AuthenticationSuccessEvent event) {
		String ip = remoteAddress(event.getAuthentication().getDetails());
		if (ip != null) {
			reset(ip);
		}
	}

	private String remoteAddress(Object details) {
		return details instanceof WebAuthenticationDetails web ? web.getRemoteAddress() : null;
	}
}
