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
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Main entry point for the Gemini Gateway application.
 * Starts an HTTP server that processes documents with Gemini API.
 */
public class GatewayApplication {

    private static final Logger LOGGER = Logger.getLogger(GatewayApplication.class.getName());

    public static void main(String[] args) {
        try {
            startServer();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start application", e);
            System.exit(1);
        }
    }

    private static void startServer() {
        final var config = loadConfiguration();

        LOGGER.info(() -> "Starting Gemini Gateway on port " + config.port());
        LOGGER.info(() -> "Prompt file: " + config.promptFilePath());
        LOGGER.info("Using virtual threads for request handling");

        final var promptProvider = new FilePromptProvider(config.promptFilePath());
        final var geminiClient = createGeminiClient(config);
        final var documentProcessor = new GeminiDocumentProcessor(promptProvider, geminiClient);

        final var bossGroup = new NioEventLoopGroup(1);
        final var workerGroup = new NioEventLoopGroup();
        final var virtualThreadFactory = Thread.ofVirtual().factory();
        // Use number of CPU cores as the executor group size
        // Virtual threads are lightweight, so this is just for thread pool scheduling
        final var virtualThreadEventGroup = new DefaultEventExecutorGroup(
                Runtime.getRuntime().availableProcessors(),
                virtualThreadFactory);

        try {
            final var future = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new RequestHandlerInitializer(documentProcessor, virtualThreadEventGroup))
                    .bind(config.port())
                    .syncUninterruptibly();

            registerShutdownHook(bossGroup, workerGroup, virtualThreadEventGroup);

            LOGGER.info("Server started successfully!");
            LOGGER.info(() -> "POST http://localhost:" + config.port() + "/process");

            future.channel().closeFuture().syncUninterruptibly();
        } finally {
            shutdownEventGroups(bossGroup, workerGroup, virtualThreadEventGroup);
        }
    }

    private static ApplicationConfig loadConfiguration() {
        final boolean useEmulator = Config.useGeminiEmulator();
        return new ApplicationConfig(
                Config.getServerPort(),
                Config.getPromptFilePath(),
                useEmulator,
                useEmulator ? null : Config.getGeminiApiKey(),
                useEmulator ? null : Config.getGeminiApiEndpoint());
    }

    private static GeminiClient createGeminiClient(ApplicationConfig config) {
        return config.useEmulator()
                ? createEmulatorClient()
                : createHttpClient(config);
    }

    private static GeminiClient createEmulatorClient() {
        LOGGER.info("Using Gemini EMULATOR mode (for testing/development)");
        return new EmulatorGeminiClient();
    }

    private static GeminiClient createHttpClient(ApplicationConfig config) {
        LOGGER.info("Using real Gemini API");
        LOGGER.info(() -> "API endpoint: " + config.apiEndpoint());
        return new HttpGeminiClient(config.apiKey(), config.apiEndpoint());
    }

    private static void registerShutdownHook(EventLoopGroup bossGroup, EventLoopGroup workerGroup,
            DefaultEventExecutorGroup virtualThreadEventGroup) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down server...");
            shutdownEventGroups(bossGroup, workerGroup, virtualThreadEventGroup);
            LOGGER.info("Server stopped.");
        }, "ShutdownHook"));
    }

    private static void shutdownEventGroups(EventLoopGroup bossGroup, EventLoopGroup workerGroup,
            DefaultEventExecutorGroup virtualThreadEventGroup) {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        virtualThreadEventGroup.shutdownGracefully();
    }

    /**
     * Immutable configuration holder for the application
     */
    private record ApplicationConfig(
            int port,
            String promptFilePath,
            boolean useEmulator,
            String apiKey, // null if using emulator
            String apiEndpoint // null if using emulator
    ) {
    }
}
