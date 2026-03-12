package com.gateway;

import com.gateway.api.RequestHandlerInitializer;
import com.gateway.config.ServerConfiguration;
import com.gateway.processor.DocumentProcessor;
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
 *
 * Uses functional composition for service creation, sealed types with
 * pattern matching for type-safe initialization, and virtual threads
 * for efficient request handling.
 */
public class GatewayApplication {

    private static final Logger LOGGER = Logger.getLogger(GatewayApplication.class.getName());

    private static final int NETTY_BOSS_THREADS = 1;

    public static void main(String[] args) {
        try {
            startServer();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start application", e);
            System.exit(1);
        }
    }

    private static void startServer() {
        // Load strongly-typed configuration from environment
        final ServerConfiguration config = ServerConfiguration.load();

        LOGGER.info(() -> "Starting Gemini Gateway on port " + config.port());
        LOGGER.info(() -> "Prompt file: " + config.promptFilePath());
        LOGGER.info("Using virtual threads for request handling");

        // Create service instances using factory composition
        final var promptProvider = new FilePromptProvider(config.promptFilePath());
        final var geminiClient = switch (config) {
            case ServerConfiguration.EmulatorConfig _ -> {
                LOGGER.info("Using Gemini EMULATOR mode (for testing/development)");
                yield new EmulatorGeminiClient();
            }
            case ServerConfiguration.ApiConfig apiConfig -> {
                LOGGER.info("Using real Gemini API");
                LOGGER.info(() -> "API endpoint: " + apiConfig.apiEndpoint());
                yield new HttpGeminiClient(apiConfig.apiKey(), apiConfig.apiEndpoint());
            }
        };
        final var documentProcessor = DocumentProcessor.create(promptProvider, geminiClient);

        // Initialize Netty infrastructure
        final var bossGroup = new NioEventLoopGroup(NETTY_BOSS_THREADS);
        final var workerGroup = new NioEventLoopGroup();
        final var virtualThreadEventGroup = createVirtualThreadEventGroup();

        try {
            // Bootstrap and start server
            startServerBootstrap(config, bossGroup, workerGroup, virtualThreadEventGroup, documentProcessor);

            registerShutdownHook(bossGroup, workerGroup, virtualThreadEventGroup);

            LOGGER.info("Server started successfully!");
            LOGGER.info(() -> "POST http://localhost:" + config.port() + "/process");

            // Block until server shutdown
            Thread.currentThread().join();

        } catch (InterruptedException e) {
            LOGGER.info("Server interrupted");
            Thread.currentThread().interrupt();
        } finally {
            shutdownEventGroups(bossGroup, workerGroup, virtualThreadEventGroup);
        }
    }

    /**
     * Bootstrap and bind HTTP server
     */
    private static void startServerBootstrap(
            ServerConfiguration config,
            EventLoopGroup bossGroup,
            EventLoopGroup workerGroup,
            DefaultEventExecutorGroup virtualThreadEventGroup,
            DocumentProcessor documentProcessor) {

        var bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new RequestHandlerInitializer(documentProcessor, virtualThreadEventGroup));

        try {
            var bindFuture = bootstrap.bind(config.port()).syncUninterruptibly();
            LOGGER.info(() -> "Server bound to port " + config.port());
        } catch (Exception e) {
            throw new RuntimeException("Failed to bind server to port " + config.port(), e);
        }
    }

    /**
     * Create virtual thread executor group for handling request processing
     */
    private static DefaultEventExecutorGroup createVirtualThreadEventGroup() {
        final var cpuPoolSize = Runtime.getRuntime().availableProcessors();
        final var virtualThreadFactory = Thread.ofVirtual().factory();
        LOGGER.info(() -> "Virtual thread executor group size: " + cpuPoolSize);
        return new DefaultEventExecutorGroup(cpuPoolSize, virtualThreadFactory);
    }

    /**
     * Register shutdown hook using virtual thread for graceful cleanup
     */
    private static void registerShutdownHook(
            EventLoopGroup bossGroup,
            EventLoopGroup workerGroup,
            DefaultEventExecutorGroup virtualThreadEventGroup) {

        Runtime.getRuntime().addShutdownHook(Thread.ofVirtual().unstarted(() -> {
            LOGGER.info("Shutting down server...");
            shutdownEventGroups(bossGroup, workerGroup, virtualThreadEventGroup);
            LOGGER.info("Server stopped.");
        }));
    }

    /**
     * Gracefully shutdown all event groups
     */
    private static void shutdownEventGroups(
            EventLoopGroup bossGroup,
            EventLoopGroup workerGroup,
            DefaultEventExecutorGroup virtualThreadEventGroup) {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        virtualThreadEventGroup.shutdownGracefully();
    }
}
