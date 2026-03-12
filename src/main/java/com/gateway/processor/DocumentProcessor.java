package com.gateway.processor;

import com.gateway.api.dto.ProcessRequest;
import com.gateway.api.dto.GeminiRequest;
import com.gateway.api.dto.GeminiResponse;
import com.gateway.service.GeminiClient;
import com.gateway.service.PromptProvider;

/**
 * Processes documents with Gemini API
 */
@FunctionalInterface
public interface DocumentProcessor {
    GeminiResponse process(ProcessRequest request);

    /**
     * Create processor by composing dependencies
     */
    static DocumentProcessor create(PromptProvider prompt, GeminiClient client) {
        return request -> client.sendRequest(
                new GeminiRequest(prompt.getPrompt(), request.document(), request.modelName()));
    }
}
