package com.tcgstore.shop.domain;

/**
 * STANDARD: sealed product/accessory with a single price+stock.
 * RAW: ungraded single, sold per condition/finish.
 * GRADED: slabbed single (PSA/BGS/CGC...), usually quantity 1.
 */
public enum VariantKind {
	STANDARD, RAW, GRADED
}
