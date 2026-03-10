package com.gateway.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Standard error response returned to clients
 */
public record ErrorDetails(
        @JsonProperty("error") String error,
        @JsonProperty("code") int code) {

    /**
     * Create an error response with default 500 status
     */
    public ErrorDetails(String error) {
        this(error, 500);
    }
}
