package com.gateway.api;

import com.gateway.api.handler.*;
import com.gateway.processor.DocumentProcessor;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.util.concurrent.EventExecutorGroup;

/**
 * Initializes Netty pipeline for HTTP request handling.
 *
 * Pipeline stages (inbound):
 * 1. HttpServerCodec - decodes HTTP frames (NIO thread)
 * 2. HttpObjectAggregator - aggregates parts into full requests (NIO thread)
 * 3. JsonDecodeHandler - parses JSON to ProcessRequest (NIO thread)
 * 4. RequestValidationHandler - validates document presence/size (NIO thread)
 * 5. DocumentProcessingHandler - calls Gemini API (virtual thread)
 * 6. ResponseEncodingHandler - formats response as JSON HTTP (NIO thread)
 *
 * Errors at any stage trigger ErrorResponse event which ResponseEncodingHandler
 * catches
 * and formats as error JSON response.
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

        // HTTP codec - decode incoming HTTP frames (NIO thread)
        pipeline.addLast(new HttpServerCodec());

        // HTTP aggregator - combine partial frames into complete requests (NIO thread)
        // Set max content length to 120KB to support up to 100KB documents plus JSON
        // overhead
        pipeline.addLast(new HttpObjectAggregator(120 * 1024));

        // JSON decoder - parse HTTP body to ProcessRequest (NIO thread)
        pipeline.addLast(new JsonDecodeHandler());

        // Request validator - check document presence and size (NIO thread)
        pipeline.addLast(new RequestValidationHandler());

        // Document processor - call Gemini API (virtual thread, blocking)
        pipeline.addLast(virtualThreadEventGroup, new DocumentProcessingHandler(documentProcessor));

        // Response encoder - format response as JSON HTTP (NIO thread)
        pipeline.addLast(new ResponseEncodingHandler());
    }
}
