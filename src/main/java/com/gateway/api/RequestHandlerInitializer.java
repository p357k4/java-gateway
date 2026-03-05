package com.gateway.api;

import com.gateway.processor.DocumentProcessor;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.util.concurrent.EventExecutorGroup;

/**
 * Initializes Netty pipeline for HTTP request handling
 * Adds the custom request handler to a virtual thread executor for blocking
 * operations
 */
public class RequestHandlerInitializer extends ChannelInitializer<SocketChannel> {

    private final DocumentProcessor documentProcessor;
    private final EventExecutorGroup virtualThreadEventGroup;

    public RequestHandlerInitializer(DocumentProcessor documentProcessor, EventExecutorGroup virtualThreadEventGroup) {
        this.documentProcessor = documentProcessor;
        this.virtualThreadEventGroup = virtualThreadEventGroup;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        final var pipeline = ch.pipeline();

        // HTTP codec for encoding/decoding (runs on NIO event loop)
        pipeline.addLast(new HttpServerCodec());

        // Aggregate full HTTP requests (runs on NIO event loop)
        pipeline.addLast(new HttpObjectAggregator(65536));

        // Custom request handler runs on virtual thread executor (for blocking
        // operations)
        pipeline.addLast(virtualThreadEventGroup, new NettyRequestHandler(documentProcessor));
    }
}
