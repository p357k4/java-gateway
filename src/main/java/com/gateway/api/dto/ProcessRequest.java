package com.gateway.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for document processing
 */
public record ProcessRequest(
    @JsonProperty("document") String document,
    @JsonProperty("modelName") String modelName
) {
    public ProcessRequest(String document) {
        this(document, "gemini-2.0-flash");
    }

    public ProcessRequest {
        if (modelName == null) {
            modelName = "gemini-2.0-flash";
        }
    }

    public String getModelName() {
        return modelName != null ? modelName : "gemini-2.0-flash";
    }
}
