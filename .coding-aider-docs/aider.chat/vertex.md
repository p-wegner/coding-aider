# Vertex AI Integration with Aider

## Prerequisites

To use Aider with Google Vertex AI, you'll need to:

1. Install the [gcloud CLI](https://cloud.google.com/sdk/docs/install)
2. Login with a GCP account or service account with Vertex AI API permissions

## Configuration

### Environment Variables

Set the following environment variables:

- `GOOGLE_APPLICATION_CREDENTIALS`: Automatically set by gcloud CLI
- `VERTEXAI_PROJECT`: Your GCP project ID
- `VERTEXAI_LOCATION`: GCP region (e.g., `us-east5`)

#### Example .env File
```
VERTEXAI_PROJECT=my-project
VERTEXAI_LOCATION=us-east5
```

## Running Aider with Vertex AI

### Command Line
```bash
aider --model vertex_ai/claude-3-5-sonnet@20240620
```

### YAML Configuration
Create a `.aider.conf.yml` file:
```yaml
model: vertex_ai/claude-3-5-sonnet@20240620
```

## Important Notes

- Claude on Vertex AI is only available in specific GCP regions
- Check the [model card](https://console.cloud.google.com/vertex-ai/publishers/anthropic/model-garden/claude-3-5-sonnet) for region support
