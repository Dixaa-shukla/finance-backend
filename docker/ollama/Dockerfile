# Custom Ollama image for the Finance Management backend.
# Wraps the official ollama/ollama image with an entrypoint that
# auto-pulls the configured chat + embedding models on first start --

FROM ollama/ollama:latest

# Chat model --
ARG OLLAMA_MODEL=llama3.2:1b
ENV OLLAMA_MODEL=${OLLAMA_MODEL}

# NEW: embedding model, needed for RAG (Module 11). nomic-embed-text is
# they only convert text to vectors, they don't generate text.
ARG OLLAMA_EMBEDDING_MODEL=nomic-embed-text
ENV OLLAMA_EMBEDDING_MODEL=${OLLAMA_EMBEDDING_MODEL}

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

EXPOSE 11434

ENTRYPOINT ["/entrypoint.sh"]