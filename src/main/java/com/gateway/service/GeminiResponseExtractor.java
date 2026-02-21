package com.gateway.service;

import com.gateway.api.dto.GeminiResponse;

/**
 * Extracts meaningful data from Gemini API responses
 */
public final class GeminiResponseExtractor {
    
    /**
     * Extracts the text response from the first candidate in a Gemini response
     * 
     * @param response the Gemini API response
     * @return the text response, or null if not found
     */
    public static String extractText(GeminiResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            return null;
        }

        final var candidate = response.candidates().get(0);
        if (candidate.content() == null || candidate.content().parts() == null || candidate.content().parts().isEmpty()) {
            return null;
        }

        return candidate.content().parts().get(0).text();
    }
}
