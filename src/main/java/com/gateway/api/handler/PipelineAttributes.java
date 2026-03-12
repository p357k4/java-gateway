package com.gateway.api.handler;

import com.gateway.api.dto.ProcessRequest;
import com.gateway.api.dto.GeminiResponse;
import io.netty.util.AttributeKey;

/**
 * Shared AttributeKey constants for passing data through the Netty pipeline.
 * Handlers use these keys to store and retrieve context data from the channel.
 */
public final class PipelineAttributes {

    private PipelineAttributes() {
        // Utility class
    }

    /**
     * AttributeKey for storing the parsed ProcessRequest
     */
    public static final AttributeKey<ProcessRequest> PROCESS_REQUEST = AttributeKey.newInstance("processRequest");

    /**
     * AttributeKey for storing the Gemini API response
     */
    public static final AttributeKey<GeminiResponse> GEMINI_RESPONSE = AttributeKey.newInstance("geminiResponse");

    /**
     * AttributeKey for storing extracted response text
     */
    public static final AttributeKey<String> RESPONSE_TEXT = AttributeKey.newInstance("responseText");
}
