# Gemini Emulator Mode

The Java Gateway supports an emulator mode for testing and development without requiring a real Gemini API key.

## Usage

### Enable Emulator

To run the gateway in emulator mode, set the `USE_GEMINI_EMULATOR` environment variable before starting:

```bash
export USE_GEMINI_EMULATOR=true
./run.sh
```

Or run directly:

```bash
USE_GEMINI_EMULATOR=true java -cp "target/classes:target/dependency/*" com.gateway.GatewayApplication
```

### Disabling the API Key Requirement

When emulator mode is enabled, the `GEMINI_API_KEY` environment variable is not required. The application will simply use the emulator client instead of making real API calls.

## How the Emulator Works

The `EmulatorGeminiClient` class provides mock responses that simulate the Gemini API behavior. It includes intelligent response generation based on the prompt content:

- **Summarize requests**: Returns a mock summary response
- **Extract requests**: Returns mock extracted information in a structured format
- **Analysis requests**: Returns mock analysis results
- **Translation requests**: Returns a mock translation
- **Default**: Returns a generic response for any other prompt type

## Example Request

```bash
curl -X POST http://localhost:8080/process \
  -H "Content-Type: application/json" \
  -d '{
    "document": "This is a test document for summarization."
  }'
```

## Response Structure

The emulator returns responses in the same format as the real Gemini API:

```json
{
  "candidates": [
    {
      "content": {
        "parts": [
          {
            "text": "Mock response text..."
          }
        ],
        "role": "assistant"
      },
      "finishReason": "STOP"
    }
  ]
}
```

## Benefits

1. **No API Key Required**: Test the application without setting up API authentication
2. **Instant Responses**: Get immediate responses without network latency
3. **Development Friendly**: Perfect for local development and testing
4. **Cost-Free**: Avoid API usage costs during development
5. **Consistent Behavior**: Always returns the same mock responses for predictable testing

## Switching Between Real API and Emulator

Simply toggle the environment variable:

```bash
# Use emulator
export USE_GEMINI_EMULATOR=true

# Use real API
export USE_GEMINI_EMULATOR=false
# or unset it
unset USE_GEMINI_EMULATOR
```

When switching to the real API, ensure `GEMINI_API_KEY` is properly set.
