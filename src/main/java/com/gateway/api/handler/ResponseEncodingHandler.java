package com.gateway.api.handler;

import com.gateway.api.dto.GeminiResponse;
import com.gateway.util.JsonMapper;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import java.util.Map;
import java.util.logging.Level;

/**
 * Encodes successful processing results as a JSON HTTP response.
 * Handles both success responses and error events fired from upstream handlers.
 *
 * Extracts text from the Gemini API response and formats it as JSON,
 * then writes the HTTP response to the client.
 */
public class ResponseEncodingHandler extends AbstractPipelineHandler {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            GeminiResponse response = ctx.channel().attr(PipelineAttributes.GEMINI_RESPONSE).get();

            // If we have a response from document processing, encode it as JSON
            if (response != null) {
                String text = response.firstText().orElse("No content");
                sendSuccessResponse(ctx, text);
                return;
            }

            // Otherwise, pass through
            ctx.fireChannelRead(msg);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Response encoding failed", e);
            sendErrorResponse(ctx, 500, "Failed to encode response");
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        // Handle ErrorResponse events from upstream handlers
        if (evt instanceof ErrorResponse error) {
            sendErrorResponse(ctx, error.statusCode(), error.message());
            return;
        }

        // Pass other events through
        ctx.fireUserEventTriggered(evt);
    }

    /**
     * Send successful response as JSON
     */
    private void sendSuccessResponse(ChannelHandlerContext ctx, String content) {
        try {
            String json = JsonMapper.toJson(Map.of("response", content));
            sendResponse(ctx, 200, json);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to encode success response", e);
            sendErrorResponse(ctx, 500, "Internal error");
        }
    }

    /**
     * Send error response as JSON
     */
    private void sendErrorResponse(ChannelHandlerContext ctx, int code, String message) {
        try {
            String json = JsonMapper.toJson(Map.of("error", message));
            sendResponse(ctx, code, json);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to encode error response", e);
            ctx.close();
        }
    }

    /**
     * Send HTTP response with given status code and JSON body
     */
    private void sendResponse(ChannelHandlerContext ctx, int code, String body) {
        try {
            byte[] bytes = body.getBytes(CharsetUtil.UTF_8);
            var response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.valueOf(code),
                    Unpooled.wrappedBuffer(bytes));

            response.headers()
                    .set(HttpHeaderNames.CONTENT_TYPE, "application/json")
                    .set(HttpHeaderNames.CONTENT_LENGTH, bytes.length)
                    .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);

            ctx.writeAndFlush(response).addListener(f -> ctx.close());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to send response", e);
            ctx.close();
        }
    }
}
