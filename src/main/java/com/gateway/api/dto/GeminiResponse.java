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
                /**
                 * Extract the first part's text from this candidate's content.
                 * 
                 * @return Optional containing the text, or empty if not found
                 */
                public java.util.Optional<String> firstPartText() {
                        return java.util.Optional.ofNullable(content)
                                        .flatMap(c -> java.util.Optional.ofNullable(c.parts()))
                                        .stream()
                                        .flatMap(list -> list.stream())
                                        .map(Part::text)
                                        .filter(text -> text != null && !text.isEmpty())
                                        .findFirst();
                }
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

        /**
         * Extract the first text content from the response using Optional chaining.
         * Safely navigates the nested structure
         * ({@code candidates[0].content().parts()[0].text()}).
         *
         * @return Optional containing the first text, or empty if any level is
         *         null/empty
         */
        public java.util.Optional<String> firstText() {
                return java.util.Optional.ofNullable(candidates)
                                .stream()
                                .flatMap(list -> list.stream())
                                .map(Candidate::firstPartText)
                                .flatMap(java.util.Optional::stream)
                                .findFirst();
        }
}
