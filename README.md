# MCP Application

This Spring Boot application integrates OpenAI's API with MCP (Model Context Protocol) to provide an intelligent document search and Q&A system using vector databases.

## Architecture

The application follows this processing flow:

```
[User Question Input]
       ↓
[Convert Question to Vector] ← OpenAI Embedding API
       ↓
[Search Similar Documents] ← Qdrant Vector DB
       ↓
[Build Context-Based Prompt] ← Templates from resources
       ↓
[Call OpenAI GPT API with MCP Tools]
       ↓
[Process Response] ← Formatting according to response type
       ↓
[Return Response to User]
```

## MCP Tools Integration

This application integrates with the following MCP tools:

1. **Filesystem MCP Tool**: For reading and writing files
2. **Brave Search MCP Tool**: For web search capabilities 
3. **PostgreSQL MCP Tool**: For structured data access

## Key Components

- **Vector Embedding**: Converting text to vector representations
- **Vector Database (Qdrant)**: Storing and searching document vectors
- **MCP Client/Server**: Interfacing with external tools and resources
- **OpenAI Integration**: Leveraging GPT models for intelligent responses

## Setup and Deployment

### Prerequisites

- JDK 17 or later
- Docker and Docker Compose
- OpenAI API key
- Brave Search API key (optional)

### Environment Variables

Set the following environment variables:
- `OPENAI_API_KEY`: Your OpenAI API key
- `BRAVE_SEARCH_API_KEY`: Your Brave Search API key (if using Brave Search)

### Running with Docker Compose

```bash
# Build and start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop all services
docker-compose down
```

### Accessing the API

The main API is available at:
- Chat API: `POST http://localhost:8080/api/chat`
- Document Upload: `POST http://localhost:8080/api/documents/upload`
- Document Listing: `GET http://localhost:8080/api/documents`

## Development

### Project Structure

- `config/`: Application configuration
- `controller/`: REST API endpoints
- `model/`: Domain models
- `repository/`: Data access interfaces
- `service/`: Business logic
- `mcp/`: MCP tool configurations
- `util/`: Utility classes
- `resources/`: Templates and configuration files

### Building the Project

```bash
./gradlew clean build
```

## Customization

- Modify prompt templates in `resources/templates/prompts/`
- Configure vector database settings in `application.properties`
- Add additional MCP tools by implementing new MCP Server configurations
