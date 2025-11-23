# Financial Assistant

> AI-powered financial assistant leveraging RAG (Retrieval-Augmented Generation) with regulatory documents to provide accurate, up-to-date financial guidance.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-blue.svg)](https://kotlinlang.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.1.0-blue.svg)](https://spring.io/projects/spring-ai)

## Overview

A RAG-powered financial assistant that turns Turkish regulatory documents into conversational knowledge. Ask questions in natural language and get accurate, source-backed answers from official tax guides, BDDK reports, and financial regulations - without manually searching through hundreds of pages of PDFs.

## Features

- **RAG-Powered Responses**: Retrieves relevant information from official documents before generating responses
- **Conversation Memory**: Maintains context across multiple messages using JDBC-backed chat history
- **Document Ingestion**: Automatically loads and processes PDF documents from the resources folder at startup
- **Vector Search**: Efficient semantic search using pgvector with HNSW indexing
- **Turkish Financial Context**: Specialized in Turkish financial regulations, tax codes, and official guidelines
- **Type-Safe Configuration**: Configuration properties with defaults and environment variable support

## Tech Stack

- **Backend Framework**: Spring Boot 3.5.6 with Kotlin 2.1.0
- **AI Integration**: Spring AI 1.1.0 (currently configured with OpenAI GPT-4o)
- **Vector Database**: PostgreSQL 17 with pgvector extension
- **Document Processing**: Spring AI PDF Document Reader with Token Text Splitter
- **Build Tool**: Gradle 8.14.3 with Kotlin DSL
- **Java Version**: OpenJDK 21

## Prerequisites

Before running this application, ensure you have:

- **Java 21** or higher installed
- **Docker** for running PostgreSQL with pgvector
- **OpenAI API Key** ([get your key](https://platform.openai.com/api-keys))
- **Git** for version control

## Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/AhmetErenLapanta/financial-assistant.git
cd financial-assistant
```

### 2. Start PostgreSQL with pgvector

```bash
cd docker
docker-compose up -d
```

This starts a Docker container running PostgreSQL 17 with the pgvector extension on port 5432.

### 3. Set Environment Variable

```bash
export MY_OPENAI_KEY="your-openai-api-key-here"
```

Or on Windows:
```cmd
set MY_OPENAI_KEY=your-openai-api-key-here
```

### 4. Build the Application

```bash
./gradlew clean build
```

### 5. Run the Application

```bash
./gradlew bootRun
```

The application will start on `http://localhost:8080` and automatically:
- Load PDF documents from `src/main/resources/rag/`
- Generate embeddings with split chunks and store them in pgvector
- Be ready to answer questions

## Architecture Overview

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │ POST /chat
       ▼
┌──────────────────┐
│  ChatController  │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐         ┌──────────────────┐
│   ChatService    ├────────►│   ChatClient     │
└──────────────────┘         └────────┬─────────┘
                                      │
              ┌───────────────────────┼───────────────────────┐
              ▼                       ▼                       ▼
    ┌─────────────────┐   ┌──────────────────────┐   ┌──────────────┐
    │   ChatMemory    │   │ QuestionAnswerAdvisor│   │   AI Model   │
    │ (JDBC-backed)   │   │  (RAG from pgvector) │   │              │
    └─────────────────┘   └──────────┬───────────┘   └──────────────┘
                                     │
                                     ▼
                            ┌────────────────┐
                            │  VectorStore   │
                            │   (pgvector)   │
                            └────────┬───────┘
                                     ▲
                                     │
                     ┌───────────────┴──────────────┐
                     │  DocumentEmbeddingService    │
                     │   (Loads PDFs on startup)    │
                     └──────────────────────────────┘
```

### Component Responsibilities

- **ChatController**: REST endpoint for handling chat requests
- **ChatService**: Orchestrates chat flow with advisors
- **DocumentEmbeddingService**: Loads, processes, and embeds PDF documents at startup
- **ChatMemory**: JDBC-backed conversation history keyed by conversation ID
- **QuestionAnswerAdvisor**: Retrieves relevant document chunks using vector similarity search
- **VectorStore**: PostgreSQL with pgvector for efficient semantic search

## Configuration

### Environment Variables

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `MY_OPENAI_KEY` | OpenAI API key | Yes | - |

### Application Configuration

Key configuration in `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:aiservice}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
  ai:
    openai:
      api-key: ${MY_OPENAI_KEY}
      chat:
        options:
          model: gpt-4o
    vectorstore:
      pgvector:
        initialize-schema: true
        index-type: HNSW
        distance-type: COSINE_DISTANCE
        dimensions: 1536
```

**Note**: Database configuration supports environment variables (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`) with defaults shown above for local development.

## Usage Examples

### Starting a New Conversation

```bash
curl -X POST http://localhost:8080/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "What is the tax base and how is it calculated?",
    "conversationId": "your-conversation-id"
  }'
```

The response will be returned as plain text. If you didn't provide a `conversationId`, the server generates a UUID internally. To continue the conversation, you'll need to provide the same `conversationId` in subsequent requests.

### Example Questions

**English:**
- "What are the income tax brackets for 2025 in Turkey?"
- "How is rental income taxed according to GİB regulations?"
- "How should I declare my freelance income?"
- "What taxes do I need to pay when trading stocks?"
- "What are the SPK rules for investing in mutual funds?"

**Turkish:**
- "2025 yılı gelir vergisi dilim oranları nedir?"
- "Kira geliri nasıl vergilendirilir?"
- "Freelance gelirlerimi nasıl beyan etmeliyim?"
- "Borsada işlem yaparken hangi vergileri ödemem gerekir?"
- "SPK'ya göre yatırım fonlarına nasıl yatırım yapabilirim?"

## Development Guide

### Project Structure

```
financial-assistant/
├── src/main/kotlin/com/ai/financial_assistant/
│   ├── config/          # Spring configuration classes
│   ├── controller/      # REST controllers
│   ├── service/         # Business logic services
│   └── domain/
│       └── dto/
│           └── request/  # Request DTOs
├── src/main/resources/
│   ├── rag/            # PDF documents for RAG (37MB currently)
│   ├── prompts/        # System prompts for AI
│   └── application.yml # Application configuration
├── src/test/kotlin/    # Unit tests
└── docker/             # Docker Compose for PostgreSQL
```

### Adding New Documents

1. Place your PDF files in `src/main/resources/rag/`
2. Restart the application
3. Documents will be automatically processed and embedded

### Running Tests

```bash
./gradlew test
```

### Building

```bash
./gradlew clean build
```

The executable JAR will be in `build/libs/`.

## Contribution Guidelines

1. **Fork the repository** and create a feature branch
2. **Follow the code conventions**:
   - Use Kotlin data classes for DTOs
   - Follow constructor-based dependency injection
   - Use meaningful variable names (no abbreviations)
   - Add validation annotations to request DTOs
3. **Write tests**: Add unit tests using MockK for new services
4. **Test your changes**: Run `./gradlew test` and `./gradlew build`
5. **Submit a Pull Request**: Provide a clear description of the changes

Run `./gradlew ktlintCheck` to verify code style, or `./gradlew ktlintFormat` to auto-format.

## Troubleshooting

### Application Fails to Start

**Problem**: `Connection refused` error

**Solution**: Ensure PostgreSQL is running:
```bash
cd docker && docker-compose ps
```

### No Documents Found Warning

**Problem**: `No PDF documents found` in logs
**Solution**: Verify PDFs exist in `src/main/resources/rag/`

### OpenAI API Errors

**Problem**: `401 Unauthorized` or `429 Rate Limit`
**Solution**:
- Verify your OpenAI API key is set correctly
- Check your OpenAI account has available credits
- Monitor rate limits on your OpenAI dashboard


## Monitoring

The application exposes Actuator endpoints for monitoring:

- **Health**: `http://localhost:8080/actuator/health`
- **Info**: `http://localhost:8080/actuator/info`
- **Metrics**: `http://localhost:8080/actuator/metrics`
- **HikariCP**: `http://localhost:8080/actuator/hikaricp`

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
