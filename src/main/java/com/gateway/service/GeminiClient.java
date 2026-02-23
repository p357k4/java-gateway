package com.gateway.service;

import com.gateway.api.dto.GeminiRequest;
import com.gateway.api.dto.GeminiResponse;

/**
 * Interface for communicating with the Gemini API
 */
public interface GeminiClient {

    /**
     * Send a request to the Gemini API with system instruction and document
     *
     * @param request the request containing system instruction, document, and model
     *                name
     * @return the response from Gemini API
     */
    GeminiResponse sendRequest(GeminiRequest request);
}
