package com.gateway.api.handler;

import com.gateway.api.dto.ProcessRequest;
import com.gateway.api.dto.GeminiResponse;
import com.gateway.processor.DocumentProcessor;
import io.netty.channel.ChannelHandlerContext;
import java.util.concurrent.Executor;
import java.util.logging.Level;

/**
 * Processes the validated request via the DocumentProcessor.
 *
 * Delegates blocking I/O operations (Gemini API calls) to a virtual thread
 * executor,
 * keeping the NIO event loop free for concurrent request handling. In Netty
 * 4.2+,
 * handlers manage their own executors rather than relying on deprecated
 * EventExecutorGroup binding.
 */
public class DocumentProcessingHandler extends AbstractPipelineHandler {

    private final DocumentProcessor documentProcessor;
    private final Executor blockingExecutor;

    public DocumentProcessingHandler(DocumentProcessor documentProcessor, Executor blockingExecutor) {
        this.documentProcessor = documentProcessor;
        this.blockingExecutor = blockingExecutor;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ProcessRequest request = ctx.channel().attr(PipelineAttributes.PROCESS_REQUEST).get();

        // If no request was parsed, skip this handler
        if (request == null) {
            ctx.fireChannelRead(msg);
            return;
        }

        // Schedule blocking operation on virtual thread executor (Netty 4.2+ pattern:
        // handlers manage their own executors for blocking work)
        blockingExecutor.execute(() -> processBlockingOperation(ctx, request, msg));
    }

    /**
     * Perform the blocking Gemini API call on the virtual thread executor.
     * This keeps the NIO event loop free for concurrent request handling.
     */
    private void processBlockingOperation(ChannelHandlerContext ctx, ProcessRequest request, Object msg) {
        try {
            // Process the document via Gemini API
            GeminiResponse response = documentProcessor.process(request);

            // Store response in channel context
            ctx.channel().attr(PipelineAttributes.GEMINI_RESPONSE).set(response);

            // Fire forward to next handler (on NIO event loop)
            ctx.executor().execute(() -> ctx.fireChannelRead(msg));

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Document processing failed", e);
            // Fire error back on NIO event loop
            ctx.executor().execute(() -> respondWithError(ctx, 500, e.getMessage()));
        }
    }
}
