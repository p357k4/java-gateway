package com.gateway.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response DTO for Gemini API
 */
public record GeminiResponse(
    @JsonProperty("candidates") List<Candidate> candidates
) {
    public record Candidate(
        @JsonProperty("content") Content content,
        @JsonProperty("finishReason") String finishReason
    ) {}

    public record Content(
        @JsonProperty("parts") List<Part> parts,
        @JsonProperty("role") String role
    ) {}

    public record Part(
        @JsonProperty("text") String text
    ) {}
}

