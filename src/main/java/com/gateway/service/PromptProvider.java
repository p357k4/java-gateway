package com.gateway.service;

import com.gateway.exception.GatewayException;

/**
 * Interface for providing system instructions to be sent to Gemini API
 */
public interface PromptProvider {

    /**
     * Get the system instruction template
     *
     * @return the system instruction template ready to send to Gemini
     * @throws GatewayException if the prompt cannot be loaded or processed
     */
    String getPrompt() throws GatewayException;
}
