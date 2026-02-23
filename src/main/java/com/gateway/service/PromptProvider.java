package com.gateway.service;

/**
 * Interface for providing system instructions to be sent to Gemini API
 */
public interface PromptProvider {

    /**
     * Get the system instruction template
     *
     * @return the system instruction template ready to send to Gemini
     */
    String getPrompt();
}
