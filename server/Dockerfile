# The assistant's server (ADR 0007). Built and run by compose.yaml, next to the model.
# Standard library only: nothing is installed, so nothing unverified runs at build time.
FROM python:3.13-slim

WORKDIR /app
COPY assistant/ assistant/
COPY prompts/ prompts/
COPY schemas/*.json schemas/

# Not root: the server needs no privileges.
USER nobody
EXPOSE 8080
CMD ["python", "-m", "assistant"]
