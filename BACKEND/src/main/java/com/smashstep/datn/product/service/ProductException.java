package com.smashstep.datn.product.service;

import org.springframework.http.HttpStatus;

public class ProductException extends RuntimeException {
    private final HttpStatus status;
    public ProductException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
    public HttpStatus getStatus() { return status; }
    public static ProductException notFound(String message) { return new ProductException(HttpStatus.NOT_FOUND, message); }
    public static ProductException conflict(String message) { return new ProductException(HttpStatus.CONFLICT, message); }
    public static ProductException badRequest(String message) { return new ProductException(HttpStatus.BAD_REQUEST, message); }
}

