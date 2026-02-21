package com.gateway.service;

import com.gateway.exception.GatewayException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Loads prompts from a file and substitutes document placeholders
 */
public class FilePromptProvider implements PromptProvider {
    
    private final String filePath;
    private String cachedTemplate;

    public FilePromptProvider(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public String getPrompt(String document) throws GatewayException {
        try {
            // Load template on first use, then cache it
            if (cachedTemplate == null) {
                cachedTemplate = Files.readString(Paths.get(filePath));
            }
            
            // Replace {document} placeholder with actual document
            return cachedTemplate.replace("{document}", document);
        } catch (IOException e) {
            throw new GatewayException(
                "Failed to load prompt template from: " + filePath,
                e
            );
        }
    }
}
