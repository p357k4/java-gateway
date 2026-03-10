package com.gateway.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.exception.GatewayException;
import java.util.logging.Logger;

/**
 * Centralized JSON mapper for consistent serialization/deserialization.
 * Eliminates code duplication and provides a single point for JSON
 * configuration.
 *
 * Uses eager initialization to catch configuration errors early.
 */
public final class JsonMapper {

    private static final Logger LOGGER = Logger.getLogger(JsonMapper.class.getName());

    private static final ObjectMapper INSTANCE = createMapper();

    private JsonMapper() {
        // Utility class
    }

    /**
     * Get the shared ObjectMapper instance
     */
    public static ObjectMapper get() {
        return INSTANCE;
    }

    /**
     * Serialize object to JSON string
     */
    public static String toJson(Object obj) {
        try {
            return INSTANCE.writeValueAsString(obj);
        } catch (Exception e) {
            throw new GatewayException.ProcessingException(
                    "Failed to serialize to JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Deserialize JSON string to typed object
     */
    public static <T> T fromJson(String json, Class<T> type) {
        try {
            return INSTANCE.readValue(json, type);
        } catch (Exception e) {
            throw new GatewayException.ProcessingException(
                    "Failed to deserialize JSON for type " + type.getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    private static ObjectMapper createMapper() {
        LOGGER.fine("Initializing shared ObjectMapper");
        return new ObjectMapper();
    }
}
