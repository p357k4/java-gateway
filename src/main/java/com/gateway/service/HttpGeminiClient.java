package com.gateway.service;

import com.gateway.api.dto.GeminiApiRequest;
import com.gateway.api.dto.GeminiRequest;
import com.gateway.api.dto.GeminiResponse;
import com.gateway.exception.GatewayException;
import com.gateway.util.JsonMapper;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

/**
 * HTTP-based client for communicating with Gemini API.
 *
 * Uses ObjectMapper for JSON serialization to eliminate manual string building
 * and ensure proper escaping/encoding. Validates responses and converts errors
 * to typed exceptions.
 */
public class HttpGeminiClient implements GeminiClient {

    private final String apiKey;
    private final String apiEndpoint;
    private final HttpClient httpClient;

    public HttpGeminiClient(String apiKey, String apiEndpoint) {
        this.apiKey = apiKey;
        this.apiEndpoint = apiEndpoint;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public GeminiResponse sendRequest(GeminiRequest request) {
        try {
            // Build typed request envelope using DTO (ObjectMapper handles serialization)
            final var apiRequest = GeminiApiRequest.create(request.systemInstruction(), request.document());
            final var requestBody = JsonMapper.toJson(apiRequest);

            // Create HTTP request
            final var httpRequest = HttpRequest.newBuilder()
                    .uri(new URI(apiEndpoint + "?key=" + apiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            // Send request and get response
            final var response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString());

            // Check HTTP status
            if (response.statusCode() != 200) {
                throw new GatewayException.ServiceException(
                        "Gemini API returned status " + response.statusCode() + ": " + response.body());
            }

            // Deserialize and validate response
            return validateResponse(response.body());

        } catch (GatewayException e) {
            throw e;
        } catch (Exception e) {
            throw new GatewayException.ServiceException("Failed to call Gemini API: " + e.getMessage(), e);
        }
    }

    /**
     * Validates the Gemini API response structure
     *
     * @param responseBody JSON string from the API
     * @return parsed and validated GeminiResponse
     * @throws GatewayException.ServiceException if response is invalid or empty
     */
    private GeminiResponse validateResponse(String responseBody) {
        final var geminiResponse = JsonMapper.fromJson(responseBody, GeminiResponse.class);

        if (geminiResponse.candidates() == null || geminiResponse.candidates().isEmpty()) {
            throw new GatewayException.ServiceException("Gemini API returned no candidates in response");
        }

        return geminiResponse;
    }
}
