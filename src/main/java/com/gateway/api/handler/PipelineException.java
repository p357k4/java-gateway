package com.gateway.api.handler;

/**
 * Exception types for pipeline execution errors.
 *
 * Sealed class hierarchy allows precise error handling by callers.
 * Distinguishes between timeout and execution failures.
 */
public sealed class PipelineException extends RuntimeException {

    public PipelineException(String message) {
        super(message);
    }

    public PipelineException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Stage exceeded its configured timeout
     */
    public static final class TimeoutException extends PipelineException {
        public TimeoutException(String message) {
            super(message);
        }

        public TimeoutException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Stage failed during execution
     */
    public static final class ExecutionException extends PipelineException {
        public ExecutionException(String message) {
            super(message);
        }

        public ExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
