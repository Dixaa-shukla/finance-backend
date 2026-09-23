#!/bin/sh
set -e

# Start the Ollama server in the background so we can talk to it below.
ollama serve &
OLLAMA_PID=$!

echo "Waiting for Ollama server to become ready..."
until ollama list >/dev/null 2>&1; do
  sleep 1
done

pull_if_missing() {
  MODEL_NAME="$1"
  if ollama list | grep -q "${MODEL_NAME}"; then
    echo "Model '${MODEL_NAME}' already present -- skipping pull."
  else
    echo "Pulling model '${MODEL_NAME}' (first run only, this may take a while)..."
    ollama pull "${MODEL_NAME}"
  fi
}

# Chat model (existing).
pull_if_missing "${OLLAMA_MODEL}"

# NEW: embedding model, needed for RAG (Module 11)
pull_if_missing "${OLLAMA_EMBEDDING_MODEL}"

echo "Ollama is ready -- serving chat model '${OLLAMA_MODEL}' and embedding model '${OLLAMA_EMBEDDING_MODEL}' on port 11434."

# Bring the background server process to the foreground so the
# container keeps running instead of exiting immediately.
wait $OLLAMA_PID