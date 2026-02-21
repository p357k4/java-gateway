package com.gateway;

import com.gateway.api.RequestHandlerInitializer;
import com.gateway.config.Config;
import com.gateway.processor.GeminiDocumentProcessor;
import com.gateway.service.FilePromptProvider;
import com.gateway.service.HttpGeminiClient;
import com.gateway.service.EmulatorGeminiClient;
import com.gateway.service.GeminiClient;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * Main entry point for the Gemini Gateway application.
 * Starts an HTTP server that processes documents with Gemini API.
 */
public class GatewayApplication {
    
    public static void main(String[] args) {
        try {
            // Load configuration
            final var port = Config.getServerPort();
            final var promptFilePath = Config.getPromptFilePath();
            final var threadPoolSize = Config.getThreadPoolSize();
            final var useEmulator = Config.useGeminiEmulator();

            System.out.println("Starting Gemini Gateway on port " + port);
            System.out.println("Prompt file: " + promptFilePath);

            // Initialize services
            final var promptProvider = new FilePromptProvider(promptFilePath);
            
            // Create Gemini client (use emulator or real API)
            final GeminiClient geminiClient;
            if (useEmulator) {
                System.out.println("Using Gemini EMULATOR mode (for testing/development)");
                geminiClient = new EmulatorGeminiClient();
            } else {
                final var apiKey = Config.getGeminiApiKey();
                final var apiEndpoint = Config.getGeminiApiEndpoint();
                System.out.println("Using real Gemini API");
                System.out.println("API endpoint: " + apiEndpoint);
                geminiClient = new HttpGeminiClient(apiKey, apiEndpoint);
            }
            
            final var documentProcessor = new GeminiDocumentProcessor(
                promptProvider,
                geminiClient
            );

            // Create Netty event loop groups
            final var bossGroup = new NioEventLoopGroup(1);
            final var workerGroup = new NioEventLoopGroup(threadPoolSize);

            try {
                // Create server bootstrap
                final var bootstrap = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new RequestHandlerInitializer(documentProcessor));

                // Bind to port
                final var future = bootstrap.bind(port).sync();

                // Add shutdown hook for graceful shutdown
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    System.out.println("\nShutting down server...");
                    bossGroup.shutdownGracefully();
                    workerGroup.shutdownGracefully();
                    System.out.println("Server stopped.");
                }));

                System.out.println("Server started successfully!");
                System.out.println("POST http://localhost:" + port + "/process");

                // Wait until server socket is closed
                future.channel().closeFuture().sync();
            } finally {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        } catch (Exception e) {
            System.err.println("Failed to start application: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
