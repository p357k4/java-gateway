package com.gateway.api.handler;

import com.gateway.api.dto.ProcessRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for pipeline handlers that consolidates common error handling
 * patterns.
 *
 * Provides:
 * - Protected logger for subclasses
 * - Common respondWithError() for firing ErrorResponse events through the
 * pipeline
 * - Default exceptionCaught() implementation that logs and calls
 * respondWithError()
 *
 * Subclasses should only override channelRead() and userEventTriggered() as
 * needed.
 */
public abstract class AbstractPipelineHandler extends ChannelInboundHandlerAdapter {

    protected final Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Fire an ErrorResponse event to downstream handlers for error processing.
     * The event will bubble to ResponseEncodingHandler which formats and sends the
     * HTTP response.
     *
     * @param ctx     the channel context
     * @param code    HTTP status code
     * @param message error message
     */
    protected void respondWithError(ChannelHandlerContext ctx, int code, String message) {
        ctx.fireUserEventTriggered(new ErrorResponse(code, message));
    }

    /**
     * Handle uncaught exceptions from this handler.
     * Logs the exception and fires an error response event.
     *
     * @param ctx   the channel context
     * @param cause the exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.log(Level.SEVERE, "Handler exception", cause);
        respondWithError(ctx, 500, "Server error");
    }

    /**
     * Convenience method to get ProcessRequest from channel attributes.
     *
     * @param ctx the channel context
     * @return the ProcessRequest, or null if not present
     */
    protected ProcessRequest getProcessRequest(ChannelHandlerContext ctx) {
        return ctx.channel().attr(PipelineAttributes.PROCESS_REQUEST).get();
    }
}
