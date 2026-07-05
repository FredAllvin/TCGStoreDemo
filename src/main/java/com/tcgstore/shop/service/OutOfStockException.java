package com.tcgstore.shop.service;

/** Thrown when a cart line can no longer be reserved; the checkout transaction rolls back. */
public class OutOfStockException extends RuntimeException {
}
