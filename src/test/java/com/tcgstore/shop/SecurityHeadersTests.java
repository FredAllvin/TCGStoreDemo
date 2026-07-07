package com.tcgstore.shop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The CSP header the security audit added, minus the parts that broke checkout. */
@SpringBootTest(properties = "app.upload-dir=target/test-uploads")
@Testcontainers(disabledWithoutDocker = true)
class SecurityHeadersTests {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	WebApplicationContext context;

	MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
	}

	@Test
	void cspAllowsTheRedirectToStripeCheckout() throws Exception {
		// Chrome enforces form-action against the 302 a form POST answers with,
		// so 'self' alone blocks the hand-off to Stripe's hosted checkout page.
		mvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Security-Policy",
						containsString("form-action 'self' https://checkout.stripe.com")));
	}

	@Test
	void cspStillForbidsFramingAndOffSiteScripts() throws Exception {
		mvc.perform(get("/"))
				.andExpect(header().string("Content-Security-Policy", containsString("default-src 'self'")))
				.andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")));
	}
}
