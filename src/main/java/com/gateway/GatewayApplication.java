package com.gateway;

import com.gateway.api.RequestHandlerInitializer;
import com.gateway.config.ServerConfiguration;
import com.gateway.processor.DocumentProcessor;
import com.gateway.service.FilePromptProvider;
import com.gateway.service.HttpGeminiClient;
import com.gateway.service.EmulatorGeminiClient;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
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
        LOGGER.info("Using virtual threads for blocking I/O operations");

        // Create service instances using factory composition
        final var documentProcessor = createDocumentProcessor(config);

        // Create virtual thread executor for blocking I/O (Netty 4.2+ pattern)
        final var blockingExecutor = createBlockingExecutor();

        // Initialize Netty infrastructure using factory methods to abstract away
        // deprecation
        final var bossGroup = createBossEventLoopGroup();
        final var workerGroup = createWorkerEventLoopGroup();

        try {
            // Bootstrap and start server
            startServerBootstrap(config, bossGroup, workerGroup, blockingExecutor, documentProcessor);

            registerShutdownHook(bossGroup, workerGroup, blockingExecutor);

            LOGGER.info("Server started successfully!");
            LOGGER.info(() -> "POST http://localhost:" + config.port() + "/process");

            // Block until server shutdown
            Thread.currentThread().join();

        } catch (InterruptedException e) {
            LOGGER.info("Server interrupted");
            Thread.currentThread().interrupt();
        } finally {
            shutdownEventGroups(bossGroup, workerGroup, blockingExecutor);
        }
    }

    /**
     * Create DocumentProcessor using factory pattern with appropriate Gemini client
     */
    private static DocumentProcessor createDocumentProcessor(ServerConfiguration config) {
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
        return DocumentProcessor.create(promptProvider, geminiClient);
    }

    /**
     * Bootstrap and bind HTTP server
     */
    private static void startServerBootstrap(
            ServerConfiguration config,
            EventLoopGroup bossGroup,
            EventLoopGroup workerGroup,
            java.util.concurrent.Executor blockingExecutor,
            DocumentProcessor documentProcessor) {

        var bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new RequestHandlerInitializer(documentProcessor, blockingExecutor));

        try {
            bootstrap.bind(config.port()).syncUninterruptibly();
            LOGGER.info(() -> "Server bound to port " + config.port());
        } catch (Exception e) {
            throw new RuntimeException("Failed to bind server to port " + config.port(), e);
        }
    }

    /**
     * Create a virtual thread executor for blocking I/O operations.
     * Uses Java 21+ virtual threads for efficient resource utilization.
     * This is the Netty 4.2+ pattern: handlers manage their own executors
     * rather than relying on deprecated EventExecutorGroup binding.
     */
    private static java.util.concurrent.Executor createBlockingExecutor() {
        final var totalThreads = Runtime.getRuntime().availableProcessors() * 2;
        final var virtualThreadFactory = Thread.ofVirtual().factory();
        LOGGER.info(() -> "Creating blocking executor with " + totalThreads + " virtual threads");
        return java.util.concurrent.Executors.newThreadPerTaskExecutor(virtualThreadFactory);
    }

    /**
     * Factory method for creating the boss event loop group.
     * NioEventLoopGroup is marked deprecated in Netty 4.2 but remains the correct
     * concrete implementation for NIO transport in Netty 4.2. This method abstracts
     * the deprecation away from call sites.
     */
    @SuppressWarnings("deprecation")
    private static EventLoopGroup createBossEventLoopGroup() {
        return new NioEventLoopGroup(NETTY_BOSS_THREADS);
    }

    /**
     * Factory method for creating the worker event loop group.
     * NioEventLoopGroup is marked deprecated in Netty 4.2 but remains the correct
     * concrete implementation for NIO transport in Netty 4.2. This method abstracts
     * the deprecation away from call sites.
     */
    @SuppressWarnings("deprecation")
    private static EventLoopGroup createWorkerEventLoopGroup() {
        return new NioEventLoopGroup();
    }

    /**
     * Register shutdown hook for graceful resource cleanup on JVM termination.
     * Properly shuts down both Netty event groups and the blocking executor.
     */
    private static void registerShutdownHook(
            EventLoopGroup bossGroup,
            EventLoopGroup workerGroup,
            java.util.concurrent.Executor blockingExecutor) {

        Runtime.getRuntime().addShutdownHook(Thread.ofVirtual().unstarted(() -> {
            LOGGER.info("Shutting down server...");
            try {
                // Initiate graceful shutdown of Netty event groups
                var bossShutdown = bossGroup.shutdownGracefully();
                var workerShutdown = workerGroup.shutdownGracefully();

                // Shutdown the blocking executor if it's an ExecutorService
                if (blockingExecutor instanceof java.util.concurrent.ExecutorService service) {
                    service.shutdown();
                }

                // Wait for groups to terminate
                bossShutdown.sync();
                workerShutdown.sync();

                LOGGER.info("Server shutdown complete.");
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "Shutdown interrupted", e);
                Thread.currentThread().interrupt();
            }
        }));
    }

    /**
     * Gracefully shutdown all event groups and blocking executor
     */
    private static void shutdownEventGroups(
            EventLoopGroup bossGroup,
            EventLoopGroup workerGroup,
            java.util.concurrent.Executor blockingExecutor) {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        if (blockingExecutor instanceof java.util.concurrent.ExecutorService service) {
            service.shutdown();
        }
    }
}
