package com.gateway.api.handler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Executes pipeline stages with timeout and error handling.
 *
 * Encapsulates the repetitive pattern of:
 * - Logging stage start/completion
 * - Submitting work to executor pool with timeout
 * - Handling various exception types (timeout, execution errors)
 *
 * Eliminates nested try-catch-return blocks in request handlers.
 */
public class PipelineStageExecutor {

    private static final Logger LOGGER = Logger.getLogger(PipelineStageExecutor.class.getName());

    private final ExecutorService executorService;
    private final PipelineTimeout defaultTimeout;

    public PipelineStageExecutor(ExecutorService executorService, PipelineTimeout defaultTimeout) {
        this.executorService = executorService;
        this.defaultTimeout = defaultTimeout;
    }

    /**
     * Execute a stage with a specific timeout configuration.
     *
     * @param stageName descriptive name for logging
     * @param stageFunc the function to execute
     * @param timeout   specific timeout for this stage
     * @param <T>       return type of the stage
     * @return result from the stage execution
     * @throws PipelineException if stage fails (timeout or execution error)
     */
    public <T> T execute(String stageName, StageFunction<T> stageFunc, PipelineTimeout timeout) {
        try {
            LOGGER.fine(() -> "Starting " + stageName);

            final var result = executorService.submit(stageFunc::execute)
                    .get(timeout.duration(), timeout.unit());

            LOGGER.fine(() -> "Completed " + stageName + " successfully");
            return result;

        } catch (TimeoutException e) {
            LOGGER.log(Level.WARNING, stageName + " timeout after " + timeout.duration() + " " + timeout.unit());
            throw new PipelineException.TimeoutException(stageName + " timeout", e);

        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING, stageName + " interrupted", e);
            Thread.currentThread().interrupt();
            throw new PipelineException.ExecutionException(stageName + " interrupted", e);

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, stageName + " failed: " + e.getMessage(), e);
            throw new PipelineException.ExecutionException(stageName + " failed: " + e.getMessage(), e);
        }
    }

    /**
     * Execute a stage with default timeout
     */
    public <T> T execute(String stageName, StageFunction<T> stageFunc) {
        return execute(stageName, stageFunc, defaultTimeout);
    }

    /**
     * Functional interface for stage execution
     */
    @FunctionalInterface
    public interface StageFunction<T> {
        T execute() throws Exception;
    }
}
