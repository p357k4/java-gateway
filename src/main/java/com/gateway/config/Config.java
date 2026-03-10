package com.gateway.config;

/**
 * Configuration loader for the Gemini Gateway application.
 * Uses ServerConfiguration sealed type hierarchy to provide strongly-typed
 * config.
 *
 * @deprecated Use ServerConfiguration.load() directly instead
 */
@Deprecated(since = "2.0.0", forRemoval = true)
public class Config {

    private Config() {
        // Utility class
    }

    /**
     * Load configuration from environment variables
     * 
     * @return strongly-typed ServerConfiguration
     */
    public static ServerConfiguration getConfiguration() {
        return ServerConfiguration.load();
    }

    // Keep legacy methods for gradual migration
    public static String getGeminiApiKey() {
        return System.getenv("GEMINI_API_KEY");
    }

    public static int getServerPort() {
        String port = System.getenv("SERVER_PORT");
        try {
            return port != null ? Integer.parseInt(port) : 8080;
        } catch (NumberFormatException e) {
            return 8080;
        }
    }

    public static String getPromptFilePath() {
        String path = System.getenv("PROMPT_FILE_PATH");
        return path != null ? path : "src/main/resources/prompt.txt";
    }

    public static String getGeminiApiEndpoint() {
        return "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";
    }

    public static boolean useGeminiEmulator() {
        return System.getenv("USE_GEMINI_EMULATOR") != null;
    }
}
