package com.gateway.service;

import com.gateway.exception.GatewayException;

/**
 * Interface for providing prompts to be sent to Gemini API
 */
public interface PromptProvider {
    
    /**
     * Get a prompt with the document substituted for {document} placeholder
     * 
     * @param document the document content to inject into the prompt
     * @return the complete prompt ready to send to Gemini
     * @throws GatewayException if the prompt cannot be loaded or processed
     */
    String getPrompt(String document) throws GatewayException;
}
