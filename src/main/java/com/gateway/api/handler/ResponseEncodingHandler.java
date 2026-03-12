package com.gateway.api.handler;

import com.gateway.api.dto.GeminiResponse;
import com.gateway.util.JsonMapper;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Encodes successful processing results as a JSON HTTP response.
 * Handles both success responses and error events fired from upstream handlers.
 *
 * Extracts text from the Gemini API response and formats it as JSON,
 * then writes the HTTP response to the client.
 */
public class ResponseEncodingHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = Logger.getLogger(ResponseEncodingHandler.class.getName());

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            GeminiResponse response = ctx.channel().attr(PipelineAttributes.GEMINI_RESPONSE).get();

            // If we have a response from document processing, encode it as JSON
            if (response != null) {
                String text = extractText(response);
                sendSuccessResponse(ctx, text);
                return;
            }

            // Otherwise, pass through
            ctx.fireChannelRead(msg);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Response encoding failed", e);
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
     * Extract text content from Gemini API response
     */
    private String extractText(GeminiResponse response) {
        var candidates = response.candidates();
        if (candidates == null || candidates.isEmpty()) {
            return "No content";
        }

        var content = candidates.get(0).content();
        var parts = (content != null) ? content.parts() : null;
        if (parts == null || parts.isEmpty()) {
            return "No content";
        }

        var text = parts.get(0).text();
        return (text != null && !text.isEmpty()) ? text : "No content";
    }

    /**
     * Send successful response as JSON
     */
    private void sendSuccessResponse(ChannelHandlerContext ctx, String content) {
        try {
            String json = JsonMapper.toJson(Map.of("response", content));
            sendResponse(ctx, 200, json);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to encode success response", e);
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
            LOGGER.log(Level.SEVERE, "Failed to encode error response", e);
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
            LOGGER.log(Level.SEVERE, "Failed to send response", e);
            ctx.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.log(Level.SEVERE, "Response encoding exception", cause);
        sendErrorResponse(ctx, 500, "Server error");
    }
}
