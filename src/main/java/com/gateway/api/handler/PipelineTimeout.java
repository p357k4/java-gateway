package com.gateway.api.handler;

import java.util.concurrent.TimeUnit;

/**
 * Configuration for request processing pipeline timeout behavior.
 *
 * Each stage can have different timeout requirements based on the operation.
 */
public record PipelineTimeout(
        long duration,
        TimeUnit unit) {

    public static PipelineTimeout seconds(long seconds) {
        return new PipelineTimeout(seconds, TimeUnit.SECONDS);
    }

    public static PipelineTimeout millis(long millis) {
        return new PipelineTimeout(millis, TimeUnit.MILLISECONDS);
    }

    public long toMillis() {
        return unit.toMillis(duration);
    }
}
