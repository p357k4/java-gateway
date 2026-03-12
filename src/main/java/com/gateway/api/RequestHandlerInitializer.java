package com.gateway.api;

import com.gateway.api.handler.*;
import com.gateway.processor.DocumentProcessor;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import java.util.concurrent.Executor;

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
    private final Executor blockingExecutor;

    public RequestHandlerInitializer(DocumentProcessor documentProcessor, Executor blockingExecutor) {
        this.documentProcessor = documentProcessor;
        this.blockingExecutor = blockingExecutor;
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

        // Document processor - call Gemini API (delegates blocking I/O to virtual
        // thread executor)
        // Netty 4.2+ pattern: handlers manage their own executors for blocking
        // operations
        // instead of relying on deprecated EventExecutorGroup binding
        pipeline.addLast(new DocumentProcessingHandler(documentProcessor, blockingExecutor));

        // Response encoder - format response as JSON HTTP (NIO thread)
        pipeline.addLast(new ResponseEncodingHandler());
    }
}
