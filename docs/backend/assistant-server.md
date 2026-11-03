# The assistant server

Firebase is the backend for everything but the assistant: Authentication, and Firestore with its rules. The assistant runs on our own server: a small Python HTTP server in Docker, next to the model it asks ([ADR 0007](../adr/0007-assistant-on-our-own-server.md)). The app never calls an AI provider directly.

## Where it is

```
server/
├── main.py              the HTTP server (Flask): checks the caller's ID token, answers in the callable shape
├── assistant.py         the assistant and the check of what the model answers, testable without the server
├── models.py            the models it can ask (Gemma 4 on Ollama, Gemini), chosen by configuration
├── prompts/             the prompts, versioned as files
├── tests/               pytest, with a fake model and a fake token check
├── Dockerfile           the server's image (gunicorn on port 8080)
├── compose.yaml         the server and Ollama, side by side
├── requirements.txt     flask, gunicorn, firebase-admin, google-genai
└── requirements-dev.txt pytest, pytest-cov
```

## What runs here, and what does not

The server is worth it only for what the phone cannot or must not do. Everything else stays in the app, where it works offline and is covered by the Kotlin tests.

| Runs on the server | Why |
|---|---|
| The assistant (#43, #64–#76) | Provider keys must stay on the server, and quotas and the kill switch must be enforced there (#67, #68) |

| Stays on the device | Why |
|---|---|
| Validation of items and proposals | `validateItem` runs before every write; the server validates proposals again (#65) |
| Balances and debt simplification | A pure function of the ledger, offline ([ADR 0003](../adr/0003-balances-on-device.md)) |
| Joining a space | A security rule checks the invitation ([ADR 0002](../adr/0002-firestore-data-model.md)) |
| Local reminders | Each phone schedules its own |

Entitlements (#117, #120) and push notices to other members (#102) need a server too; where they run is decided when they land.

## Rules for the server

- Every request carries the caller's Firebase ID token (`Authorization: Bearer <token>`). The server checks it with `firebase-admin`, which needs only the project id, and rejects the call before the model without a valid one.
- Provider keys live in the server's environment (a `.env` next to `compose.yaml`), never in the repo or the app.
- The assistant takes the model as a plain function (`generate(prompt, schema)`), and the model is configuration, so #70 can change it without touching the assistant.
- What the model answers is checked before it reaches the app. An answer that is not valid is an `UNAVAILABLE` error, never passed on.
- What the assistant returns is a proposal. The server never writes it to Firestore; the app saves it only after the user confirms.
- Every assistant flow has a manual path in the app.

## The interview

`POST /interview` speaks the shape of a Firebase callable: `{"data": …}` in, `{"result": …}` or `{"error": {status, message, details}}` out. For now it returns the first question of the interview (#43); #66 turns it into the whole interview.

| Call | HTTP | Body |
|---|---|---|
| Signed in | 200 | `{"result": {id, text, options, multiSelect, freeText, position, budget}}` |
| No token, or not a valid one | 401 | `{"error": {"status": "UNAUTHENTICATED", …}}` |
| The model failed or answered something invalid | 503 | `{"error": {"status": "UNAVAILABLE", …, "details": {"reason": "unavailable"}}}` |

`GET /health` answers `{"ok": true}`.

## Configuration

| Variable | Default | |
|---|---|---|
| `FIREBASE_PROJECT_ID` | `orbit-app-dev` | Whose ID tokens are accepted |
| `ASSISTANT_PROVIDER` | `local` | `local` (an OpenAI-compatible endpoint, such as Ollama) or `gemini` |
| `ASSISTANT_MODEL` | `gemma4` (local), `gemini-3.5-flash` (gemini) | The model's name for that provider. For Ollama, the tag it was pulled with |
| `MODEL_URL` | `http://ollama:11434/v1` | The OpenAI-compatible endpoint, for `local` |
| `GEMINI_API_KEY` | | Gemini's API key from AI Studio, for `gemini` (the free tier needs no billing). In `.env`, never committed |

## Deploying

On the server, from `server/`:

```sh
docker compose up -d --build
docker compose exec ollama ollama pull gemma4   # once; the tag in ASSISTANT_MODEL
```

Port 8080 needs HTTPS in front of it (a reverse proxy): the app only talks HTTPS. Ollama is not published, so only the server reaches the model.

The Firestore rules still deploy from `firebase/`: `npx firebase deploy --only firestore:rules`.

## Tests and coverage

pytest covers the server with a fake model and a fake token check, so the tests never call an AI or Firebase. CI runs them in the `Assistant server tests` job, and `sonarqube.yml` sends their coverage to SonarCloud, which needs 80% on new code like the Kotlin side.

```sh
cd server
python -m venv venv && source venv/bin/activate   # venv\Scripts\activate on Windows
pip install -r requirements.txt -r requirements-dev.txt
pytest --cov
```
