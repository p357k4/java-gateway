package com.gateway.api;

import com.gateway.api.dto.ProcessRequest;
import com.gateway.api.dto.GeminiResponse;
import com.gateway.api.handler.PipelineException;
import com.gateway.api.handler.PipelineStageExecutor;
import com.gateway.api.handler.PipelineTimeout;
import com.gateway.api.handler.RequestProcessingContext;
import com.gateway.exception.GatewayException;
import com.gateway.processor.DocumentProcessor;
import com.gateway.util.JsonMapper;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Netty HTTP handler implementing hybrid virtual thread + CPU worker pool
 * architecture for document processing.
 *
 * Delegates to a 4-stage pipeline via PipelineStageExecutor which handles
 * timeout and error management, eliminating nested try-catch blocks.
 *
 * Request flow:
 * 1. Netty I/O thread: Extract payload from HTTP request
 * 2. Virtual thread: Execute 4-stage pipeline:
 * - Stage 1: Request validation (CPU-intensive)
 * - Stage 2: External API call (blocking I/O)
 * - Stage 3: Response transformation (CPU-intensive)
 * - Stage 4: Send response back to client
 */
public class NettyRequestHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = Logger.getLogger(NettyRequestHandler.class.getName());

    private static final PipelineTimeout VALIDATION_TIMEOUT = PipelineTimeout.seconds(2);
    private static final PipelineTimeout TRANSFORMATION_TIMEOUT = PipelineTimeout.seconds(2);

    private final DocumentProcessor documentProcessor;
    private final PipelineStageExecutor pipelineExecutor;

    public NettyRequestHandler(DocumentProcessor documentProcessor, ExecutorService cpuWorkerPool) {
        this.documentProcessor = documentProcessor;
        this.pipelineExecutor = new PipelineStageExecutor(cpuWorkerPool, VALIDATION_TIMEOUT);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof FullHttpRequest request) {
            // Extract payload on Netty thread before offloading (critical for buffer
            // safety)
            final String payload = request.content().toString(CharsetUtil.UTF_8);
            request.release();

            // Execute 4-stage pipeline on virtual thread
            executeRequestPipeline(ctx, payload);
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    /**
     * Execute the complete 4-stage request processing pipeline.
     *
     * Delegates stage execution to PipelineStageExecutor which provides
     * consistent timeout and error handling without nested try-catch.
     */
    private void executeRequestPipeline(ChannelHandlerContext ctx, String payload) {
        try {
            // Parse request JSON into typed object
            final ProcessRequest processRequest = parseRequest(payload);

            // Create context to pass through pipeline stages
            final RequestProcessingContext context = new RequestProcessingContext(processRequest);

            // Stage 1: Validate request (CPU-intensive, runs on cpuWorkerPool)
            pipelineExecutor.execute(
                    "Request Validation",
                    () -> performInputValidation(context),
                    VALIDATION_TIMEOUT);

            // Stage 2: Call Gemini API (blocking I/O, runs on virtual thread)
            pipelineExecutor.execute(
                    "Gemini API Call",
                    () -> performGeminiCall(context),
                    PipelineTimeout.seconds(30)); // API call may take longer

            // Stage 3: Transform response (CPU-intensive, runs on cpuWorkerPool)
            pipelineExecutor.execute(
                    "Response Transformation",
                    () -> performResponseTransformation(context),
                    TRANSFORMATION_TIMEOUT);

            // Stage 4: Send successful response to client
            sendResponse(ctx, 200, context.getTransformedResponse());

        } catch (PipelineException.TimeoutException e) {
            // Timeout during processing (could be any stage)
            sendErrorResponse(ctx, 408, "Request processing timeout: " + e.getMessage());
        } catch (PipelineException.ExecutionException e) {
            // Execution error - determine appropriate HTTP status
            handlePipelineExecutionError(ctx, e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error in request pipeline", e);
            sendErrorResponse(ctx, 500, "Internal server error");
        }
    }

    /**
     * Parse JSON payload into typed ProcessRequest object
     */
    private ProcessRequest parseRequest(String payload) {
        if (payload == null || payload.isEmpty()) {
            throw new GatewayException.ValidationException("Empty request body");
        }

        try {
            return JsonMapper.fromJson(payload, ProcessRequest.class);
        } catch (GatewayException.ProcessingException e) {
            throw new GatewayException.ValidationException("Invalid request format: " + e.getMessage());
        }
    }

    /**
     * Stage 1: Validate request content
     */
    private Void performInputValidation(RequestProcessingContext context) {
        final ProcessRequest request = context.getProcessRequest();

        if (request.document() == null || request.document().isBlank()) {
            throw new GatewayException.ValidationException(
                    "Document field is required and cannot be empty");
        }

        if (request.document().length() > 100_000) {
            throw new GatewayException.ValidationException(
                    "Document exceeds maximum length of 100,000 characters");
        }

        LOGGER.finer(() -> "Validated document: " +
                request.document().substring(0, Math.min(50, request.document().length())));

        return null;
    }

    /**
     * Stage 2: Call Gemini API and store response
     */
    private Void performGeminiCall(RequestProcessingContext context) {
        try {
            GeminiResponse response = documentProcessor.process(context.getProcessRequest());
            context.setGeminiResponse(response);
            return null;
        } catch (GatewayException e) {
            throw e;
        } catch (Exception e) {
            throw new GatewayException.ServiceException(
                    "Gemini API call failed: " + e.getMessage(), e);
        }
    }

    /**
     * Stage 3: Transform Gemini response to JSON string
     */
    private Void performResponseTransformation(RequestProcessingContext context) {
        GeminiResponse response = context.getGeminiResponse();
        String transformed = JsonMapper.toJson(response);

        LOGGER.finer(() -> "Transformed response (first 100 chars): " +
                transformed.substring(0, Math.min(100, transformed.length())));

        context.setTransformedResponse(transformed);
        return null;
    }

    /**
     * Determines appropriate HTTP status code for pipeline execution errors
     */
    private void handlePipelineExecutionError(ChannelHandlerContext ctx, PipelineException.ExecutionException e) {
        final String message = e.getMessage();

        if (message.contains("Validation") || message.contains("validation")) {
            sendErrorResponse(ctx, 400, "Validation failed: " + e.getMessage());
        } else if (message.contains("Gemini") || message.contains("service")) {
            sendErrorResponse(ctx, 502, "External service error: " + e.getMessage());
        } else if (message.contains("Transformation")) {
            sendErrorResponse(ctx, 500, "Response transformation error: " + e.getMessage());
        } else {
            sendErrorResponse(ctx, 500, "Request processing failed: " + e.getMessage());
        }
    }

    /**
     * Send successful HTTP response to client
     */
    private void sendResponse(ChannelHandlerContext ctx, int statusCode, String responseBody) {
        final byte[] responseBytes = responseBody.getBytes(CharsetUtil.UTF_8);
        final DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.valueOf(statusCode),
                Unpooled.wrappedBuffer(responseBytes));

        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, responseBytes.length);
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);

        ctx.writeAndFlush(response).addListener(future -> {
            if (!future.isSuccess()) {
                LOGGER.log(Level.WARNING, "Failed to send response", future.cause());
            }
            ctx.close();
        });
    }

    /**
     * Send error response to client with consistent format
     */
    private void sendErrorResponse(ChannelHandlerContext ctx, int statusCode, String errorMessage) {
        try {
            final String errorJson = JsonMapper.toJson(new ErrorResponseDTO(errorMessage));
            sendResponse(ctx, statusCode, errorJson);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to serialize error response", e);
            ctx.close();
        }
    }

    /**
     * Simple DTO for error responses (replaces ErrorResponse)
     */
    private record ErrorResponseDTO(String error) {
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.log(Level.SEVERE, "Unexpected exception in request handler", cause);
        sendErrorResponse(ctx, 500, "Internal server error: " + cause.getMessage());
    }
}
