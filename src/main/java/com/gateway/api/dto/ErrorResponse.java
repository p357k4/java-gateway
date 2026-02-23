package com.gateway.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Error response DTO
 */
public record ErrorResponse(
        @JsonProperty("error") String error) {
}
