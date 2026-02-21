package com.gateway.processor;

import com.gateway.api.dto.ProcessRequest;
import com.gateway.api.dto.GeminiResponse;
import com.gateway.exception.GatewayException;
import com.gateway.service.PromptProvider;
import com.gateway.service.GeminiClient;

/**
 * Orchestrates document processing workflow:
 * 1. Get prompt template
 * 2. Inject document into prompt
 * 3. Send to Gemini API
 * 4. Return response
 */
public class GeminiDocumentProcessor implements DocumentProcessor {
    
    private final PromptProvider promptProvider;
    private final GeminiClient geminiClient;

    public GeminiDocumentProcessor(PromptProvider promptProvider, GeminiClient geminiClient) {
        this.promptProvider = promptProvider;
        this.geminiClient = geminiClient;
    }

    @Override
    public GeminiResponse process(ProcessRequest request) throws GatewayException {
        if (request.document() == null || request.document().isBlank()) {
            throw new GatewayException("Document cannot be empty");
        }

        // Step 1: Get prompt with document injected
        final var prompt = promptProvider.getPrompt(request.document());

        // Step 2: Send to Gemini API
        final var response = geminiClient.sendRequest(prompt, request.getModelName());

        return response;
    }
}
