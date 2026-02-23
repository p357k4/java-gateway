package com.gateway.processor;

import com.gateway.api.dto.ProcessRequest;
import com.gateway.api.dto.GeminiRequest;
import com.gateway.api.dto.GeminiResponse;
import com.gateway.exception.GatewayException;
import com.gateway.service.PromptProvider;
import com.gateway.service.GeminiClient;

/**
 * Orchestrates document processing workflow:
 * 1. Get system instruction template
 * 2. Create request with system instruction and document as separate parts
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
    public GeminiResponse process(ProcessRequest request) {
        if (request.document() == null || request.document().isBlank()) {
            throw new GatewayException("Document cannot be empty");
        }

        // Step 1: Get system instruction template
        final var systemInstruction = promptProvider.getPrompt();

        // Step 2: Create Gemini request with separate system instruction and document
        final var geminiRequest = new GeminiRequest(
                systemInstruction,
                request.document(),
                request.modelName());

        // Step 3: Send to Gemini API
        final var response = geminiClient.sendRequest(geminiRequest);

        return response;
    }
}
