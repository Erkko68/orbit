# The assistant's server (ADR 0007). Built and run by compose.yaml, next to the model.
FROM python:3.13-slim

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY assistant.py models.py main.py ./
COPY prompts/ prompts/

# Not root: the server needs no privileges.
USER nobody
EXPOSE 8080
# Long timeout: a small model on a CPU can take a while to answer.
CMD ["gunicorn", "--bind", "0.0.0.0:8080", "--workers", "2", "--timeout", "180", "main:create_app()"]
