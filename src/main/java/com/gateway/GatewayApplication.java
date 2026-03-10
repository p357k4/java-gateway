package com.gateway;

import com.gateway.api.RequestHandlerInitializer;
import com.gateway.config.ServerConfiguration;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Main entry point for the Gemini Gateway application.
 * Starts an HTTP server that processes documents with Gemini API.
 *
 * Uses strongly-typed ServerConfiguration (sealed interface) and pattern
 * matching for type-safe initialization without conditionals.
 */
public class GatewayApplication {

    private static final Logger LOGGER = Logger.getLogger(GatewayApplication.class.getName());

    private static final int NETTY_BOSS_THREADS = 1;
    private static final int CPU_POOL_SIZE = Runtime.getRuntime().availableProcessors();

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

        // Create service instances
        final var promptProvider = new FilePromptProvider(config.promptFilePath());
        final var geminiClient = createGeminiClient(config);
        final var documentProcessor = new GeminiDocumentProcessor(promptProvider, geminiClient);

        // Initialize Netty infrastructure
        final var bossGroup = new NioEventLoopGroup(NETTY_BOSS_THREADS);
        final var workerGroup = new NioEventLoopGroup();
        final var virtualThreadEventGroup = createVirtualThreadEventGroup();
        final var cpuWorkerPool = createCpuWorkerPool();

        try {
            // Bootstrap and start server
            final var bindFuture = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(
                            new RequestHandlerInitializer(documentProcessor, virtualThreadEventGroup, cpuWorkerPool))
                    .bind(config.port())
                    .syncUninterruptibly();

            registerShutdownHook(bossGroup, workerGroup, virtualThreadEventGroup, cpuWorkerPool);

            LOGGER.info("Server started successfully!");
            LOGGER.info(() -> "POST http://localhost:" + config.port() + "/process");

            // Block until server shutdown
            bindFuture.channel().closeFuture().syncUninterruptibly();

        } finally {
            shutdownEventGroups(bossGroup, workerGroup, virtualThreadEventGroup, cpuWorkerPool);
        }
    }

    /**
     * Create GeminiClient using pattern matching on configuration type.
     * Sealed ServerConfiguration enables statically exhaustive pattern matching.
     */
    private static GeminiClient createGeminiClient(ServerConfiguration config) {
        return switch (config) {
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
    }

    /**
     * Create virtual thread executor group for handling request processing
     */
    private static DefaultEventExecutorGroup createVirtualThreadEventGroup() {
        final var virtualThreadFactory = Thread.ofVirtual().factory();
        LOGGER.info(() -> "Virtual thread executor group size: " + CPU_POOL_SIZE);
        return new DefaultEventExecutorGroup(CPU_POOL_SIZE, virtualThreadFactory);
    }

    /**
     * Create CPU worker pool for CPU-intensive operations
     */
    private static ExecutorService createCpuWorkerPool() {
        LOGGER.info(() -> "CPU Worker Pool initialized with " + CPU_POOL_SIZE + " threads");
        return Executors.newFixedThreadPool(CPU_POOL_SIZE);
    }

    /**
     * Register shutdown hook using virtual thread (no explicit Thread creation)
     */
    private static void registerShutdownHook(
            EventLoopGroup bossGroup,
            EventLoopGroup workerGroup,
            DefaultEventExecutorGroup virtualThreadEventGroup,
            ExecutorService cpuWorkerPool) {

        Runtime.getRuntime().addShutdownHook(Thread.ofVirtual().unstarted(() -> {
            LOGGER.info("Shutting down server...");
            shutdownEventGroups(bossGroup, workerGroup, virtualThreadEventGroup, cpuWorkerPool);
            LOGGER.info("Server stopped.");
        }));
    }

    /**
     * Gracefully shutdown all event groups and executor pools
     */
    private static void shutdownEventGroups(
            EventLoopGroup bossGroup,
            EventLoopGroup workerGroup,
            DefaultEventExecutorGroup virtualThreadEventGroup,
            ExecutorService cpuWorkerPool) {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        virtualThreadEventGroup.shutdownGracefully();
        cpuWorkerPool.shutdownNow();
    }
}
