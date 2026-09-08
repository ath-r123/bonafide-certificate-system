package com.bonafide.exception;

/** Thrown when user input or a business rule (e.g. certificate history) blocks an operation. */
public class ValidationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ValidationException(String message) { super(message); }
}
