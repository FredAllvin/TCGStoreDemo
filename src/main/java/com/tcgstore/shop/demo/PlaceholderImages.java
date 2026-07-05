package com.tcgstore.shop.demo;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Generates simple card-back style placeholder images for the demo catalog.
 * Deliberately no real card artwork — the demo is marketing material and
 * TCG publishers are protective of their art.
 */
public final class PlaceholderImages {

	private PlaceholderImages() {
	}

	public static void generateCard(Path target, String title, String subtitle, Color base) throws IOException {
		int w = 600;
		int h = 800;
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		try {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			g.setColor(new Color(242, 243, 245));
			g.fillRect(0, 0, w, h);

			g.setPaint(new GradientPaint(0, 0, base, w, h, base.darker().darker()));
			g.fill(new RoundRectangle2D.Float(24, 24, w - 48, h - 48, 36, 36));

			g.setColor(new Color(255, 255, 255, 60));
			g.setStroke(new BasicStroke(4f));
			g.draw(new RoundRectangle2D.Float(44, 44, w - 88, h - 88, 28, 28));

			String initial = title == null || title.isBlank() ? "?" : title.substring(0, 1).toUpperCase();
			g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 220));
			g.setColor(new Color(255, 255, 255, 80));
			FontMetrics fm = g.getFontMetrics();
			g.drawString(initial, (w - fm.stringWidth(initial)) / 2, h / 2 + fm.getAscent() / 3);

			g.setColor(Color.WHITE);
			g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
			drawCentered(g, ellipsize(title, 32), w, h - 150);

			g.setColor(new Color(255, 255, 255, 200));
			g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 22));
			drawCentered(g, ellipsize(subtitle, 42), w, h - 110);
		}
		finally {
			g.dispose();
		}
		Files.createDirectories(target.getParent());
		ImageIO.write(img, "png", target.toFile());
	}

	private static void drawCentered(Graphics2D g, String text, int width, int y) {
		if (text == null || text.isBlank()) {
			return;
		}
		FontMetrics fm = g.getFontMetrics();
		g.drawString(text, (width - fm.stringWidth(text)) / 2, y);
	}

	private static String ellipsize(String text, int max) {
		if (text == null) {
			return "";
		}
		return text.length() <= max ? text : text.substring(0, max - 1) + "…";
	}
}
