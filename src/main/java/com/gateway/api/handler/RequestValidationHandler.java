package com.gateway.api.handler;

import com.gateway.api.dto.ProcessRequest;
import io.netty.channel.ChannelHandlerContext;

/**
 * Validates the parsed ProcessRequest for correctness.
 *
 * Checks:
 * - Document is present and not blank
 * - Document does not exceed 100KB
 *
 * Fires event forward if valid, or sends error response if invalid.
 */
public class RequestValidationHandler extends AbstractPipelineHandler {

    private static final int MAX_DOCUMENT_BYTES = 100_000;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ProcessRequest request = getProcessRequest(ctx);

        // If no request was parsed, skip this handler
        if (request == null) {
            ctx.fireChannelRead(msg);
            return;
        }

        // Validate document presence
        if (request.document() == null || request.document().isBlank()) {
            respondWithError(ctx, 400, "Document required");
            return;
        }

        // Validate document size
        if (request.document().length() > MAX_DOCUMENT_BYTES) {
            respondWithError(ctx, 400, "Document exceeds 100KB limit");
            return;
        }

        // Valid request - fire forward
        ctx.fireChannelRead(msg);
    }
}
