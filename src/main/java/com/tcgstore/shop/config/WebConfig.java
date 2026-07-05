package com.tcgstore.shop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final AppProperties props;

	public WebConfig(AppProperties props) {
		this.props = props;
	}

	@Bean
	public LocaleResolver localeResolver() {
		CookieLocaleResolver resolver = new CookieLocaleResolver("SHOP_LOCALE");
		resolver.setDefaultLocale(Locale.of("sv", "SE"));
		return resolver;
	}

	@Bean
	public LocaleChangeInterceptor localeChangeInterceptor() {
		LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
		interceptor.setParamName("lang");
		interceptor.setIgnoreInvalidLocale(true);
		return interceptor;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(localeChangeInterceptor());
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		Path uploadDir = Paths.get(props.uploadDir()).toAbsolutePath().normalize();
		registry.addResourceHandler("/uploads/**")
				.addResourceLocations(uploadDir.toUri().toString())
				.setCachePeriod(3600);
	}
}
