package com.gateway.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.api.dto.ProcessRequest;
import com.gateway.exception.GatewayException;
import com.gateway.processor.DocumentProcessor;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

/**
 * Netty HTTP handler for processing /process endpoint requests
 */
public class NettyRequestHandler extends ChannelInboundHandlerAdapter {

    private final DocumentProcessor documentProcessor;
    private final ObjectMapper objectMapper;

    public NettyRequestHandler(DocumentProcessor documentProcessor) {
        this.documentProcessor = documentProcessor;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            handleRequest(ctx, (FullHttpRequest) msg);
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private void handleRequest(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        try {
            // Only accept POST requests
            if (request.method() != HttpMethod.POST) {
                sendResponse(ctx, 405, "{\"error\":\"Method not allowed. Use POST.\"}");
                return;
            }

            // Only accept /process path
            if (!request.uri().equals("/process")) {
                sendResponse(ctx, 404, "{\"error\":\"Path not found. Use POST /process\"}");
                return;
            }

            // Parse request body
            final var content = request.content().toString(CharsetUtil.UTF_8);
            final var processRequest = objectMapper.readValue(content, ProcessRequest.class);

            if (processRequest == null || processRequest.document() == null) {
                sendResponse(ctx, 400, "{\"error\":\"Invalid request. 'document' field is required.\"}");
                return;
            }

            // Process document
            final var response = documentProcessor.process(processRequest);

            // Return response
            final var responseJson = objectMapper.writeValueAsString(response);
            sendResponse(ctx, 200, responseJson);
        } catch (GatewayException e) {
            final var errorResponse = String.format(
                    "{\"error\":\"%s\"}",
                    escapeJson(e.getMessage()));
            sendResponse(ctx, 500, errorResponse);
        } catch (Exception e) {
            final var errorResponse = String.format(
                    "{\"error\":\"Internal server error: %s\"}",
                    escapeJson(e.getMessage()));
            sendResponse(ctx, 500, errorResponse);
        } finally {
            request.release();
        }
    }

    private void sendResponse(ChannelHandlerContext ctx, int statusCode, String responseBody) {
        final var responseBytes = responseBody.getBytes(CharsetUtil.UTF_8);
        final var response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.valueOf(statusCode),
                Unpooled.wrappedBuffer(responseBytes));

        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, responseBytes.length);
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);

        ctx.writeAndFlush(response).addListener(future -> ctx.close());
    }

    private String escapeJson(String text) {
        if (text == null)
            return "";
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
