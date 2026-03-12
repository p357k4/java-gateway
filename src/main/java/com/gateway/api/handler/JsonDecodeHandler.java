package com.gateway.api.handler;

import com.gateway.api.dto.ProcessRequest;
import com.gateway.util.JsonMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.CharsetUtil;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Decodes incoming HTTP request body as JSON into ProcessRequest.
 *
 * Fires the request forward in the pipeline if successful,
 * or sends error response if JSON parsing fails.
 */
public class JsonDecodeHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = Logger.getLogger(JsonDecodeHandler.class.getName());

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof FullHttpRequest request)) {
            ctx.fireChannelRead(msg);
            return;
        }

        try {
            String payload = request.content().toString(CharsetUtil.UTF_8);
            request.release();

            if (payload.isEmpty()) {
                respondWithError(ctx, 400, "Empty request");
                return;
            }

            ProcessRequest processRequest = JsonMapper.fromJson(payload, ProcessRequest.class);

            // Store the parsed request in the channel context for downstream handlers
            ctx.channel().attr(PipelineAttributes.PROCESS_REQUEST).set(processRequest);

            // Fire the original message forward (keeps it as FullHttpRequest in pipeline)
            ctx.fireChannelRead(msg);

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "JSON decode failed", e);
            respondWithError(ctx, 400, "Invalid JSON: " + e.getMessage());
        }
    }

    private void respondWithError(ChannelHandlerContext ctx, int code, String msg) {
        ctx.fireUserEventTriggered(new ErrorResponse(code, msg));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.log(Level.SEVERE, "Decode exception", cause);
        respondWithError(ctx, 500, "Server error");
    }
}
