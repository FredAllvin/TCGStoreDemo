package com.tcgstore.shop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(String uploadDir, String baseUrl, Admin admin) {

	public record Admin(String username, String password) {
	}
}
