# Java Gemini Gateway

A lightweight, framework-free REST API service built with Java 24 that processes documents with the Google Gemini API.

## Architecture

The application is built with a clean separation of concerns using interfaces and abstractions:

- **API Layer** (`api/`): HTTP request handling via the built-in `HttpServer`
- **Service Layer** (`service/`): Core service interfaces and implementations
  - `PromptProvider`: Loads and injects documents into prompt templates
  - `GeminiClient`: Communicates with the Gemini API
- **Business Logic** (`processor/`): `DocumentProcessor` orchestrates the workflow
- **Configuration** (`config/`): Centralized configuration management
- **Data Transfer** (`api/dto/`): Request/Response POJOs for JSON serialization

## Prerequisites

- Java 24 (or Java 25 when available)
- Maven 3.6+
- A Gemini API key from [Google AI Studio](https://aistudio.google.com)

## Setup

1. **Clone/Navigate to the project:**
   ```bash
   cd /Users/daniel/projects/java-gateway
   ```

2. **Set the Gemini API key:**
   ```bash
   export GEMINI_API_KEY="your-api-key-here"
   ```

3. **Build the project:**
   ```bash
   mvn clean compile
   ```

4. **Package the application:**
   ```bash
   mvn package
   ```

## Running the Server

Start the server with:

```bash
java -cp "target/classes:target/dependency/*" com.gateway.GatewayApplication
```

Or use Maven:

```bash
mvn exec:java -Dexec.mainClass="com.gateway.GatewayApplication"
```

The server starts on `http://localhost:8080` by default.

## Configuration

Set environment variables to customize behavior:

| Variable | Default | Description |
|----------|---------|-------------|
| `GEMINI_API_KEY` | *required* | Google Gemini API key |
| `SERVER_PORT` | `8080` | HTTP server port |
| `PROMPT_FILE_PATH` | `src/main/resources/prompt.txt` | Path to prompt template |
| `THREAD_POOL_SIZE` | `10` | Number of concurrent request handlers |

Example:
```bash
export GEMINI_API_KEY="sk-..."
export SERVER_PORT=9000
export PROMPT_FILE_PATH="/path/to/custom_prompt.txt"
java -cp "target/classes:target/dependency/*" com.gateway.GatewayApplication
```

## API Usage

### Endpoint: POST /process

Send a JSON request with a document:

```bash
curl -X POST http://localhost:8080/process \
  -H "Content-Type: application/json" \
  -d '{
    "document": "Your document text here...",
    "modelName": "gemini-2.0-flash"
  }'
```

**Request Body:**
```json
{
  "document": "The text to be analyzed",
  "modelName": "gemini-2.0-flash"  // optional, defaults to gemini-2.0-flash
}
```

**Success Response (200):**
```json
{
  "candidates": [
    {
      "content": {
        "role": "model",
        "parts": [
          {
            "text": "Analysis results from Gemini..."
          }
        ]
      },
      "finishReason": "STOP"
    }
  ]
}
```

**Error Response (400/500):**
```json
{
  "error": "Error message describing what went wrong"
}
```

## Prompt Template

The system reads prompts from `src/main/resources/prompt.txt`. The file should contain a `{document}` placeholder that will be replaced with the actual document text:

```
You are a helpful assistant that analyzes documents.

Please analyze the following document:

{document}

Provide a summary and key insights.
```

## Project Structure

```
java-gateway/
├── src/main/java/com/gateway/
│   ├── GatewayApplication.java          # Main entry point
│   ├── api/
│   │   ├── RequestHandler.java          # HTTP request handler
│   │   └── dto/
│   │       ├── ProcessRequest.java      # Request model
│   │       └── GeminiResponse.java      # Response model
│   ├── service/
│   │   ├── PromptProvider.java          # Interface
│   │   ├── FilePromptProvider.java      # Implementation
│   │   ├── GeminiClient.java            # Interface
│   │   └── HttpGeminiClient.java        # Implementation
│   ├── processor/
│   │   ├── DocumentProcessor.java       # Interface
│   │   └── GeminiDocumentProcessor.java # Implementation
│   ├── config/
│   │   └── Config.java                  # Configuration
│   └── exception/
│       └── GatewayException.java        # Custom exception
├── src/main/resources/
│   └── prompt.txt                       # Prompt template
└── pom.xml                              # Maven configuration
```

## Key Design Decisions

1. **No Frameworks**: Uses Java's built-in `HttpServer` to avoid framework overhead
2. **Interfaces First**: All major components define interfaces for testability and flexibility
3. **Dependency Injection**: Services are injected through constructors
4. **Minimal Dependencies**: Only Jackson for JSON serialization
5. **Thread Safety**: Built-in `HttpServer` with configurable thread pool for concurrent requests
6. **Clean Separation**: Clear layers (API, Service, Business Logic, Configuration)

## Error Handling

- **400 Bad Request**: Invalid JSON or missing required fields
- **405 Method Not Allowed**: Non-POST requests to `/process`
- **500 Internal Server Error**: Gemini API failures, file I/O errors, or unexpected exceptions

## Development

### Adding a New Endpoint

1. Create a new handler implementing `HttpHandler`
2. Register it in `GatewayApplication.createContext()`
3. Inject required services through the constructor

### Customizing Prompt Logic

1. Create a custom implementation of `PromptProvider`
2. Update the injection in `GatewayApplication`

### Changing Gemini Client

1. Implement the `GeminiClient` interface
2. Update the injection in `GatewayApplication`

## Testing

Manual integration test:

```bash
# Terminal 1: Start the server
export GEMINI_API_KEY="your-key"
java -cp "target/classes:target/dependency/*" com.gateway.GatewayApplication

# Terminal 2: Test the endpoint
curl -X POST http://localhost:8080/process \
  -H "Content-Type: application/json" \
  -d '{"document":"Analyze this: The quick brown fox jumps over the lazy dog."}'
```

## License

MIT
