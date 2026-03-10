package com.gateway.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response from Gemini API containing candidates with generated content.
 *
 * Nested records represent the hierarchical structure of the API response:
 * - Candidates are possible completions
 * - Each candidate has content (parts and role)
 * - Each part is a text segment
 */
public record GeminiResponse(
        @JsonProperty("candidates") List<GeminiResponse.Candidate> candidates) {

    /**
     * A candidate response from the Gemini API
     */
    public record Candidate(
            @JsonProperty("content") Content content,
            @JsonProperty("finishReason") String finishReason) {
    }

    /**
     * Content wrapper containing parts and role
     */
    public record Content(
            @JsonProperty("parts") List<Part> parts,
            @JsonProperty("role") String role) {
    }

    /**
     * Individual text part in the response
     */
    public record Part(
            @JsonProperty("text") String text) {
    }
}
