package com.gateway.api;

import com.gateway.processor.DocumentProcessor;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

/**
 * Initializes Netty pipeline for HTTP request handling
 */
public class RequestHandlerInitializer extends ChannelInitializer<SocketChannel> {
    
    private final DocumentProcessor documentProcessor;

    public RequestHandlerInitializer(DocumentProcessor documentProcessor) {
        this.documentProcessor = documentProcessor;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        final var pipeline = ch.pipeline();
        
        // HTTP codec for encoding/decoding
        pipeline.addLast(new HttpServerCodec());
        
        // Aggregate full HTTP requests
        pipeline.addLast(new HttpObjectAggregator(65536));
        
        // Custom request handler
        pipeline.addLast(new NettyRequestHandler(documentProcessor));
    }
}
