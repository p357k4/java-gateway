package com.gateway.exception;

/**
 * Custom exception for Gateway application
 */
public class GatewayException extends Exception {
    
    public GatewayException(String message) {
        super(message);
    }

    public GatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
