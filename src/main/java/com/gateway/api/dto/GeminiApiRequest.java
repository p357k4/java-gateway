package com.gateway.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Request envelope for Gemini API.
 * Wraps system instruction and content before sending to the API.
 */
public record GeminiApiRequest(
        @JsonProperty("systemInstruction") SystemInstruction systemInstruction,
        @JsonProperty("contents") List<Content> contents) {

    /**
     * System instruction part specifying how the model should behave
     */
    public record SystemInstruction(
            @JsonProperty("parts") List<TextPart> parts) {
    }

    /**
     * User content to be processed
     */
    public record Content(
            @JsonProperty("role") String role,
            @JsonProperty("parts") List<TextPart> parts) {
    }

    /**
     * Text part within system instruction or content
     */
    public record TextPart(
            @JsonProperty("text") String text) {
    }

    /**
     * Create a request with system instruction and document content
     */
    public static GeminiApiRequest create(String systemInstruction, String document) {
        return new GeminiApiRequest(
                new SystemInstruction(List.of(new TextPart(systemInstruction))),
                List.of(new Content("user", List.of(new TextPart(document)))));
    }
}
