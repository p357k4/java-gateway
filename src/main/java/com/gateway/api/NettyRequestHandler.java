package com.gateway.api;

/**
 * DEPRECATED: This class has been replaced by a pipeline-based architecture.
 *
 * The monolithic request handler has been decomposed into specialized handlers:
 * - JsonDecodeHandler: parses HTTP body to ProcessRequest
 * - RequestValidationHandler: validates document presence and size
 * - DocumentProcessingHandler: calls DocumentProcessor (runs on virtual
 * threads)
 * - ResponseEncodingHandler: formats response as JSON HTTP response
 *
 * See RequestHandlerInitializer for the new pipeline configuration.
 * This class is retained for backward compatibility only and should be removed
 * in a future version.
 *
 * @deprecated Use the specialized handlers in com.gateway.api.handler package
 *             instead
 */
@Deprecated(forRemoval = true)
public class NettyRequestHandler {
}
