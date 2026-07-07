package com.tcgstore.shop.service;

import com.tcgstore.shop.config.AppProperties;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Handles admin image uploads. Files are size-capped, must actually decode as
 * an image (content sniffing, not just the file name), are re-encoded via
 * Thumbnailator (which strips any embedded payloads/metadata) and stored under
 * random names outside the classpath.
 */
@Service
public class ImageService {

	private static final Logger log = LoggerFactory.getLogger(ImageService.class);
	private static final long MAX_BYTES = 5L * 1024 * 1024;
	// Pixel ceiling checked from the header *before* decoding, so a small but
	// enormously-dimensioned file (a decompression bomb) cannot exhaust memory.
	// Generous for real camera photos (~24–40 MP), fatal to a 30000×30000 bomb.
	private static final long MAX_PIXELS = 40_000_000L;
	private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");
	private static final int MAX_DIMENSION = 1400;

	private final Path root;

	public ImageService(AppProperties props) {
		this.root = Paths.get(props.uploadDir()).toAbsolutePath().normalize();
	}

	/** @return the stored path relative to the uploads dir, e.g. "products/ab12….jpg" */
	public String store(MultipartFile file, String subdir) {
		if (file == null || file.isEmpty()) {
			throw new InvalidImageException("empty");
		}
		if (file.getSize() > MAX_BYTES) {
			throw new InvalidImageException("too-large");
		}
		String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
		String ext = original.contains(".")
				? original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT)
				: "";
		if (!ALLOWED_EXTENSIONS.contains(ext)) {
			throw new InvalidImageException("bad-type");
		}

		byte[] bytes;
		try {
			bytes = file.getBytes();
		}
		catch (IOException e) {
			throw new InvalidImageException("unreadable");
		}
		// Reject decompression bombs from the declared dimensions before we ever
		// allocate a full BufferedImage for them.
		ensureSaneDimensions(bytes);

		BufferedImage image;
		try {
			image = ImageIO.read(new ByteArrayInputStream(bytes));
		}
		catch (IOException e) {
			throw new InvalidImageException("unreadable");
		}
		if (image == null) {
			// the bytes are not really an image, whatever the extension says
			throw new InvalidImageException("bad-type");
		}

		boolean hasAlpha = image.getColorModel().hasAlpha();
		String outExt = hasAlpha ? "png" : "jpg";
		String fileName = UUID.randomUUID() + "." + outExt;
		Path target = root.resolve(subdir).resolve(fileName).normalize();
		if (!target.startsWith(root)) {
			throw new InvalidImageException("bad-path");
		}
		try {
			Files.createDirectories(target.getParent());
			Thumbnails.of(image)
					.size(MAX_DIMENSION, MAX_DIMENSION)
					.keepAspectRatio(true)
					.outputFormat(outExt)
					.outputQuality(0.88)
					.toFile(target.toFile());
		}
		catch (IOException e) {
			throw new InvalidImageException("store-failed");
		}
		return subdir + "/" + fileName;
	}

	/**
	 * Reads only the image header to learn its declared width/height and rejects
	 * anything above {@link #MAX_PIXELS}. Also doubles as content sniffing: bytes
	 * with no matching {@link ImageReader} are not a real image.
	 */
	private void ensureSaneDimensions(byte[] bytes) {
		try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
			if (iis == null) {
				throw new InvalidImageException("bad-type");
			}
			Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
			if (!readers.hasNext()) {
				throw new InvalidImageException("bad-type");
			}
			ImageReader reader = readers.next();
			try {
				reader.setInput(iis);
				long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
				if (pixels > MAX_PIXELS) {
					throw new InvalidImageException("too-large");
				}
			}
			finally {
				reader.dispose();
			}
		}
		catch (IOException e) {
			throw new InvalidImageException("unreadable");
		}
	}

	public void delete(String relativePath) {
		if (relativePath == null || relativePath.isBlank()) {
			return;
		}
		Path target = root.resolve(relativePath).normalize();
		if (!target.startsWith(root)) {
			return;
		}
		try {
			Files.deleteIfExists(target);
		}
		catch (IOException e) {
			log.warn("Could not delete image {}: {}", relativePath, e.getMessage());
		}
	}
}
