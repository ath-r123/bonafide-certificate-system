package com.bonafide.exception;

/** Wraps SQLException so callers are not forced to handle JDBC checked exceptions everywhere. */
public class DataAccessException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DataAccessException(String message, Throwable cause) { super(message, cause); }
}
