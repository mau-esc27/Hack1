package org.ide.hack1.exception;

public class ApiError extends RuntimeException {
    public ApiError(String message) {
        super(message);
    }
}
