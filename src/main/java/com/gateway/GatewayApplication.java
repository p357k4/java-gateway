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

    private static void startServer() throws InterruptedException {
        final var config = loadConfiguration();

        LOGGER.info(() -> "Starting Gemini Gateway on port " + config.port());
        LOGGER.info(() -> "Prompt file: " + config.promptFilePath());

        final var promptProvider = new FilePromptProvider(config.promptFilePath());
        final var geminiClient = createGeminiClient(config);
        final var documentProcessor = new GeminiDocumentProcessor(promptProvider, geminiClient);

        final var bossGroup = new NioEventLoopGroup(1);
        final var workerGroup = new NioEventLoopGroup(config.threadPoolSize());

        try {
            final var future = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new RequestHandlerInitializer(documentProcessor))
                    .bind(config.port())
                    .sync();

            registerShutdownHook(bossGroup, workerGroup);

            LOGGER.info("Server started successfully!");
            LOGGER.info(() -> "POST http://localhost:" + config.port() + "/process");

            future.channel().closeFuture().sync();
        } finally {
            shutdownEventGroups(bossGroup, workerGroup);
        }
    }

    private static ApplicationConfig loadConfiguration() {
        final boolean useEmulator = Config.useGeminiEmulator();
        return new ApplicationConfig(
                Config.getServerPort(),
                Config.getPromptFilePath(),
                Config.getThreadPoolSize(),
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

    private static void registerShutdownHook(EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down server...");
            shutdownEventGroups(bossGroup, workerGroup);
            LOGGER.info("Server stopped.");
        }, "ShutdownHook"));
    }

    private static void shutdownEventGroups(EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    /**
     * Immutable configuration holder for the application
     */
    private record ApplicationConfig(
            int port,
            String promptFilePath,
            int threadPoolSize,
            boolean useEmulator,
            String apiKey, // null if using emulator
            String apiEndpoint // null if using emulator
    ) {
    }
}
