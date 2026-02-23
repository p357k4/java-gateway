package com.gateway.processor;

import com.gateway.api.dto.ProcessRequest;
import com.gateway.api.dto.GeminiResponse;

/**
 * Interface for processing documents with Gemini
 */
public interface DocumentProcessor {

    /**
     * Process a document: read prompt, inject document, send to Gemini
     *
     * @param request the process request containing the document
     * @return the response from Gemini API
     */
    GeminiResponse process(ProcessRequest request);
}
