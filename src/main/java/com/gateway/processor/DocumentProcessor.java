package com.gateway.processor;

import com.gateway.api.dto.ProcessRequest;
import com.gateway.api.dto.GeminiResponse;
import com.gateway.exception.GatewayException;

/**
 * Interface for processing documents with Gemini
 */
public interface DocumentProcessor {
    
    /**
     * Process a document: read prompt, inject document, send to Gemini
     * 
     * @param request the process request containing the document
     * @return the response from Gemini API
     * @throws GatewayException if processing fails
     */
    GeminiResponse process(ProcessRequest request) throws GatewayException;
}
