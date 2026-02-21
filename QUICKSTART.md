# Quick Start Guide

## 1. Get a Gemini API Key

Visit [Google AI Studio](https://aistudio.google.com) and create a free API key.

## 2. Build the Project

```bash
cd /Users/daniel/projects/java-gateway
mvn clean package -DskipTests
```

## 3. Set Your API Key

```bash
export GEMINI_API_KEY="your-api-key-from-step-1"
```

## 4. Start the Server

**Option A: Using the run script**
```bash
./run.sh
```

**Option B: Using Maven**
```bash
mvn exec:java -Dexec.mainClass="com.gateway.GatewayApplication"
```

**Option C: Using Java directly**
```bash
java -cp "target/classes:target/dependency/*" com.gateway.GatewayApplication
```

You should see:
```
Starting Gemini Gateway on port 8080
Prompt file: src/main/resources/prompt.txt
API endpoint: https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent
Server started successfully!
POST http://localhost:8080/process
```

## 5. Test the API

Open another terminal and run:

```bash
curl -X POST http://localhost:8080/process \
  -H "Content-Type: application/json" \
  -d '{
    "document": "The quick brown fox jumps over the lazy dog. This is a simple sentence demonstrating alphabetic characters.",
    "modelName": "gemini-2.0-flash"
  }'
```

You should get a JSON response with Gemini's analysis of your document.

## 6. Customize the Prompt

Edit `src/main/resources/prompt.txt` to change how Gemini analyzes documents. The `{document}` placeholder will be replaced with the document from your request.

## Example Prompts

### Summary
```
Summarize the following document in 3 bullet points:

{document}
```

### Sentiment Analysis
```
Analyze the sentiment of the following text. Return: positive, negative, or neutral.

{document}
```

### Entity Extraction
```
Extract all named entities (people, places, organizations) from the following text:

{document}
```

## Troubleshooting

**"GEMINI_API_KEY environment variable not set"**
- Make sure you exported the API key: `export GEMINI_API_KEY="your-key"`

**"Failed to load prompt template"**
- Verify `src/main/resources/prompt.txt` exists
- Run from the project root directory

**Connection refused on port 8080**
- The port may already be in use
- Set a different port: `export SERVER_PORT=9000`

**Maven build fails**
- Make sure you have Java 24+ installed: `java -version`
- Run `mvn clean install` to download dependencies

## Customization

### Change Server Port
```bash
export SERVER_PORT=9000
./run.sh
```

### Change Prompt File
```bash
export PROMPT_FILE_PATH="/path/to/your/prompt.txt"
./run.sh
```

### Adjust Thread Pool Size
```bash
export THREAD_POOL_SIZE=20
./run.sh
```

## Next Steps

- Modify `src/main/resources/prompt.txt` for different use cases
- Add more endpoints by creating new handlers in the `api/` package
- Implement custom `PromptProvider` for dynamic prompt generation
- Add response caching or request validation as needed
