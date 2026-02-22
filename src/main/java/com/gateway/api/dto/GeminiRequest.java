package com.gateway.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for Gemini API containing system instruction and document
 */
public record GeminiRequest(
        @JsonProperty("systemInstruction") String systemInstruction,
        @JsonProperty("document") String document,
        @JsonProperty("modelName") String modelName) {
}
