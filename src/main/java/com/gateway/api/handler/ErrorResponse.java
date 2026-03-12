package com.gateway.api.handler;

/**
 * Custom event fired through the pipeline when an error needs to be responded
 * to.
 * Allows handlers to short-circuit processing and flow error state to the
 * response handler.
 */
public record ErrorResponse(int statusCode, String message) {
}
