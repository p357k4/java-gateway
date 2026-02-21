package com.gateway.service;

import com.gateway.api.dto.GeminiResponse;
import com.gateway.exception.GatewayException;

/**
 * Interface for communicating with the Gemini API
 */
public interface GeminiClient {
    
    /**
     * Send a prompt to the Gemini API and get a response
     * 
     * @param prompt the prompt text to send to Gemini
     * @param modelName the model to use (e.g., "gemini-2.0-flash")
     * @return the response from Gemini API
     * @throws GatewayException if the API call fails or response is invalid
     */
    GeminiResponse sendRequest(String prompt, String modelName) throws GatewayException;
}
