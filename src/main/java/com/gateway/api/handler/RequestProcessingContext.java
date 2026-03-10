package com.gateway.api.handler;

import com.gateway.api.dto.ProcessRequest;
import com.gateway.api.dto.GeminiResponse;

/**
 * Context object passed through the 4-stage request processing pipeline.
 *
 * Holds the mutable state as requests progress through validation,
 * Gemini call, and transformation stages.
 */
public class RequestProcessingContext {

    private final ProcessRequest processRequest;
    private GeminiResponse geminiResponse;
    private String transformedResponse;

    public RequestProcessingContext(ProcessRequest processRequest) {
        this.processRequest = processRequest;
    }

    // Getters and setters
    public ProcessRequest getProcessRequest() {
        return processRequest;
    }

    public GeminiResponse getGeminiResponse() {
        return geminiResponse;
    }

    public void setGeminiResponse(GeminiResponse geminiResponse) {
        this.geminiResponse = geminiResponse;
    }

    public String getTransformedResponse() {
        return transformedResponse;
    }

    public void setTransformedResponse(String transformedResponse) {
        this.transformedResponse = transformedResponse;
    }
}
