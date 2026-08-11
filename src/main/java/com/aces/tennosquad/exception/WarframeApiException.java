package com.aces.tennosquad.exception;

public class WarframeApiException extends RuntimeException {

    public WarframeApiException(String message) {
        super(message);
    }

    public WarframeApiException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}