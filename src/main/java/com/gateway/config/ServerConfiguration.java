package com.gateway.config;

import com.gateway.exception.GatewayException;
import java.util.Objects;

/**
 * Sealed type hierarchy for server configuration.
 * Supports emulator mode for testing and real API mode for production.
 * Replaces static Config methods with immutable, testable configuration
 * objects.
 */
public sealed interface ServerConfiguration {

    int port();

    String promptFilePath();

    /**
     * Emulator configuration for testing/development (no API calls needed)
     */
    record EmulatorConfig(
            int port,
            String promptFilePath) implements ServerConfiguration {

        public EmulatorConfig {
            if (port <= 0 || port > 65535) {
                throw new IllegalArgumentException("Invalid port: " + port);
            }
            Objects.requireNonNull(promptFilePath, "promptFilePath cannot be null");
        }
    }

    /**
     * Real API configuration for production
     */
    record ApiConfig(
            int port,
            String promptFilePath,
            String apiKey,
            String apiEndpoint) implements ServerConfiguration {

        public ApiConfig {
            if (port <= 0 || port > 65535) {
                throw new IllegalArgumentException("Invalid port: " + port);
            }
            Objects.requireNonNull(promptFilePath, "promptFilePath cannot be null");
            Objects.requireNonNull(apiKey, "apiKey cannot be null");
            Objects.requireNonNull(apiEndpoint, "apiEndpoint cannot be null");
        }
    }

    /**
     * Load configuration from environment variables
     */
    static ServerConfiguration load() {
        boolean useEmulator = System.getenv("USE_GEMINI_EMULATOR") != null;
        int port = parsePort(System.getenv("SERVER_PORT"), 8080);
        String promptFilePath = System.getenv("PROMPT_FILE_PATH") != null
                ? System.getenv("PROMPT_FILE_PATH")
                : "src/main/resources/prompt.txt";

        if (useEmulator) {
            return new ServerConfiguration.EmulatorConfig(port, promptFilePath);
        }

        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new GatewayException.ConfigurationException(
                    "GEMINI_API_KEY environment variable not set");
        }

        String apiEndpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";
        return new ServerConfiguration.ApiConfig(port, promptFilePath, apiKey, apiEndpoint);
    }

    private static int parsePort(String portStr, int defaultPort) {
        if (portStr == null || portStr.isBlank()) {
            return defaultPort;
        }
        try {
            return Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            return defaultPort;
        }
    }
}
