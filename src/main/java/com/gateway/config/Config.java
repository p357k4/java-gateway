package com.gateway.config;

/**
 * Configuration for the Gemini Gateway application.
 * Reads settings from environment variables.
 */
public class Config {

    /**
     * Gemini API key from environment variable GEMINI_API_KEY
     */
    public static String getGeminiApiKey() {
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "GEMINI_API_KEY environment variable not set. " +
                            "Please set it before running the application.");
        }
        return apiKey;
    }

    /**
     * HTTP server port (default: 8080)
     */
    public static int getServerPort() {
        String port = System.getenv("SERVER_PORT");
        try {
            return port != null ? Integer.parseInt(port) : 8080;
        } catch (NumberFormatException e) {
            return 8080;
        }
    }

    /**
     * Path to prompt template file (default: src/main/resources/prompt.txt)
     */
    public static String getPromptFilePath() {
        String path = System.getenv("PROMPT_FILE_PATH");
        return path != null ? path : "src/main/resources/prompt.txt";
    }

    /**
     * Gemini API endpoint
     */
    public static String getGeminiApiEndpoint() {
        return "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";
    }

    /**
     * Thread pool size for handling concurrent requests.
     * 
     * @deprecated Virtual threads are used instead. This method is no longer
     *             needed.
     */
    @Deprecated(since = "1.1.0", forRemoval = true)
    public static int getThreadPoolSize() {
        String size = System.getenv("THREAD_POOL_SIZE");
        try {
            return size != null ? Integer.parseInt(size) : 10;
        } catch (NumberFormatException e) {
            return 10;
        }
    }

    /**
     * Whether to use the Gemini emulator instead of the real API.
     * Set USE_GEMINI_EMULATOR=true to enable emulator mode (useful for testing
     * without API key).
     * Default: false (uses real API)
     */
    public static boolean useGeminiEmulator() {
        String useEmulator = System.getenv("USE_GEMINI_EMULATOR");
        return useEmulator != null && (useEmulator.equalsIgnoreCase("true") || useEmulator.equalsIgnoreCase("1"));
    }
}
