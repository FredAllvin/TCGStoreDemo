package com.tcgstore.shop.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.NoSuchElementException;

/** Maps "row not found" lookups (e.g. stale admin links) to a 404 instead of a 500. */
@ControllerAdvice
public class GlobalExceptionAdvice {

	@ExceptionHandler(NoSuchElementException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public String notFound() {
		return "error/404";
	}
}
