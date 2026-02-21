package com.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.api.dto.GeminiResponse;
import com.gateway.exception.GatewayException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

/**
 * HTTP-based client for communicating with Gemini API
 */
public class HttpGeminiClient implements GeminiClient {
    
    private final String apiKey;
    private final String apiEndpoint;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HttpGeminiClient(String apiKey, String apiEndpoint) {
        this.apiKey = apiKey;
        this.apiEndpoint = apiEndpoint;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public GeminiResponse sendRequest(String prompt, String modelName) throws GatewayException {
        try {
            // Build Gemini request body
            final var requestBody = buildRequestBody(prompt);
            
            // Create HTTP request
            final var request = HttpRequest.newBuilder()
                .uri(new URI(apiEndpoint + "?key=" + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            // Send request and handle response
            final var response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                throw new GatewayException(
                    "Gemini API returned status " + response.statusCode() + ": " + response.body()
                );
            }

            // Parse response
            final var geminiResponse = objectMapper.readValue(
                response.body(),
                GeminiResponse.class
            );

            // Validate response
            if (geminiResponse.candidates() == null || geminiResponse.candidates().isEmpty()) {
                throw new GatewayException("Gemini API returned no candidates in response");
            }

            return geminiResponse;
        } catch (GatewayException e) {
            throw e;
        } catch (Exception e) {
            throw new GatewayException("Failed to call Gemini API: " + e.getMessage(), e);
        }
    }

    /**
     * Builds the JSON request body for Gemini API
     */
    private String buildRequestBody(String prompt) throws Exception {
        final var jsonRequest = String.format(
            "{\"contents\":[{\"role\":\"user\",\"parts\":[{\"text\":\"%s\"}]}]}",
            escapeJson(prompt)
        );
        return jsonRequest;
    }

    /**
     * Escapes special characters for JSON
     */
    private String escapeJson(String text) {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
