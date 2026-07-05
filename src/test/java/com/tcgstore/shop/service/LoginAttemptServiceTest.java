package com.tcgstore.shop.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAttemptServiceTest {

	@Test
	void blocksAfterRepeatedFailuresAndResetsOnSuccess() {
		LoginAttemptService service = new LoginAttemptService();
		for (int i = 0; i < 7; i++) {
			service.recordFailure("1.2.3.4");
		}
		assertFalse(service.isBlocked("1.2.3.4"), "seven failures should not block yet");

		service.recordFailure("1.2.3.4");
		assertTrue(service.isBlocked("1.2.3.4"), "eighth failure should block");
		assertFalse(service.isBlocked("5.6.7.8"), "other IPs are unaffected");

		service.reset("1.2.3.4");
		assertFalse(service.isBlocked("1.2.3.4"));
	}
}
