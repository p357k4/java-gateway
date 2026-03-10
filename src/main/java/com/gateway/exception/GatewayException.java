package com.gateway.exception;

/**
 * Sealed exception hierarchy for the gateway application.
 * Different error types allow callers to handle specific failure modes.
 */
public sealed class GatewayException extends RuntimeException {

    public GatewayException(String message) {
        super(message);
    }

    public GatewayException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Validation errors (4xx HTTP responses)
     */
    public static final class ValidationException extends GatewayException {
        public ValidationException(String message) {
            super(message);
        }
    }

    /**
     * External service errors (5xx HTTP responses)
     */
    public static final class ServiceException extends GatewayException {
        public ServiceException(String message) {
            super(message);
        }

        public ServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Configuration or initialization errors
     */
    public static final class ConfigurationException extends GatewayException {
        public ConfigurationException(String message) {
            super(message);
        }

        public ConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Request/response processing errors
     */
    public static final class ProcessingException extends GatewayException {
        public ProcessingException(String message) {
            super(message);
        }

        public ProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
