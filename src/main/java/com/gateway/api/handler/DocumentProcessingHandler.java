package com.gateway.api.handler;

import com.gateway.api.dto.ProcessRequest;
import com.gateway.api.dto.GeminiResponse;
import com.gateway.processor.DocumentProcessor;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Processes the validated request via the DocumentProcessor.
 *
 * Calls the Gemini API client (via DocumentProcessor) and stores the response
 * in the channel context for downstream handlers.
 *
 * This handler should be bound to the virtual thread executor group to allow
 * blocking I/O operations without blocking the NIO event loop.
 */
public class DocumentProcessingHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = Logger.getLogger(DocumentProcessingHandler.class.getName());

    private final DocumentProcessor documentProcessor;

    public DocumentProcessingHandler(DocumentProcessor documentProcessor) {
        this.documentProcessor = documentProcessor;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ProcessRequest request = ctx.channel().attr(PipelineAttributes.PROCESS_REQUEST).get();

        // If no request was parsed, skip this handler
        if (request == null) {
            ctx.fireChannelRead(msg);
            return;
        }

        try {
            // Process the document via Gemini API
            GeminiResponse response = documentProcessor.process(request);

            // Store response in channel context
            ctx.channel().attr(PipelineAttributes.GEMINI_RESPONSE).set(response);

            // Fire forward to next handler
            ctx.fireChannelRead(msg);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Document processing failed", e);
            respondWithError(ctx, 500, e.getMessage());
        }
    }

    private void respondWithError(ChannelHandlerContext ctx, int code, String msg) {
        ctx.fireUserEventTriggered(new ErrorResponse(code, msg));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.log(Level.SEVERE, "Processing exception", cause);
        respondWithError(ctx, 500, "Server error");
    }
}
