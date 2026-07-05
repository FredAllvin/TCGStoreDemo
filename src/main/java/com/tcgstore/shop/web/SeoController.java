package com.tcgstore.shop.web;

import com.tcgstore.shop.config.AppProperties;
import com.tcgstore.shop.repo.CategoryRepository;
import com.tcgstore.shop.repo.ProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class SeoController {

	private final AppProperties props;
	private final CategoryRepository categories;
	private final ProductRepository products;

	public SeoController(AppProperties props, CategoryRepository categories, ProductRepository products) {
		this.props = props;
		this.categories = categories;
		this.products = products;
	}

	@GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
	@ResponseBody
	public String robots() {
		return """
				User-agent: *
				Disallow: /admin
				Disallow: /cart
				Disallow: /checkout
				Disallow: /pay/
				Disallow: /order/
				Sitemap: %s/sitemap.xml
				""".formatted(props.baseUrl());
	}

	@GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
	@ResponseBody
	public String sitemap() {
		String base = props.baseUrl();
		StringBuilder xml = new StringBuilder();
		xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
		appendUrl(xml, base + "/");
		categories.findAllByOrderBySortOrderAscNameAsc()
				.forEach(category -> appendUrl(xml, base + "/c/" + category.getSlug()));
		// cap the sitemap at a sane size; plenty for a shop of this scale
		products.findAll(PageRequest.of(0, 5000, Sort.by("createdAt").descending()))
				.filter(p -> p.isActive())
				.forEach(product -> appendUrl(xml, base + "/p/" + product.getSlug()));
		xml.append("</urlset>\n");
		return xml.toString();
	}

	private void appendUrl(StringBuilder xml, String loc) {
		xml.append("  <url><loc>").append(loc).append("</loc></url>\n");
	}
}
