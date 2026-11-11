# ADR 0007: The assistant runs on our own server, next to its model

- Status: Accepted, amended by #297 (Gemma only, no Gemini fallback)
- Date: 2026-11-03
- Issue: #258 (follows #43)

## Context

The assistant (#43, #64–#76) was planned as Cloud Functions calling Gemini on Vertex AI. Both need the Blaze plan, which means linking a card to the Firebase project, and the team does not want to risk paying for a university project.

What stays fixed: the app never calls an AI provider directly, provider keys never reach the app or the repo, the server enforces the quota (#67) and the kill switch (#68), and AI output is only a proposal until the user confirms it.

## Decision

**The assistant's backend is a small Python HTTP server in Docker, on Èric's server, next to the model it asks. The model is Gemma 4, served by Ollama on the same machine.**

- **No card, no bill.** Firebase stays on the free Spark plan. The server asks Firebase Authentication itself whether an ID token is valid (Identity Toolkit's `accounts:lookup`), which needs an API key but no billing.
- **Nothing to install.** The server is Python's standard library only, so its image runs no third-party code and has no dependencies to pin or patch.
- **The server still decides.** It checks the caller's ID token on every request and keeps the quota and the kill switch on our side, as the Cloud Function would have.
- **The callable shape stays.** `POST /interview` takes `{"data": …}` and answers `{"result": …}` or `{"error": {status, message, details}}`, with the same statuses (`UNAUTHENTICATED`, `UNAVAILABLE`), so #44 calls it the way it would call a callable.
- **The model's size is configuration.** `ASSISTANT_MODEL` names the Gemma tag, which the stack pulls into Ollama when it starts. #70 compares sizes with the scenarios of #69.
- **No cloud model.** Gemini's API was the fallback until #297. The team will only run Gemma on our server, so no provider key, no data sent to a third party, and nothing billed.
- **The code from #43 carries over.** The question, its validation and their tests do not depend on where they run; only the wrapper changed, from `firebase-functions` to `http.server`.

The code lives in `server/`; `firebase/` keeps what is deployed to Firebase (the rules).

## Alternatives considered

- **Cloud Functions on Blaze, with a spending cap.** The original plan, but it still needs a card, and on Blaze Gemini's API is billed from the first request.
- **Firebase AI Logic on Spark.** No card and no server, but the app would call the model itself (through Firebase), and the quota and the kill switch would live in the client, where they can be bypassed.
- **The app calling the model on Èric's server directly.** Ollama has no authentication, so anyone with the address could use it; putting the token check in front of it is this server.
- **A free hosting service for the backend (Render, Vercel…), calling Èric's model.** One more service to keep, idle instances that wake up slowly, and the assistant still depends on Èric's server for the model.
- **A model on the phone.** Ruled out by AGENTS.md ("No on-device LLM") and too heavy for modest phones.

## Consequences

- The assistant is as available as Èric's server: if it is down, the app shows the assistant as unavailable and the manual path remains (every flow has one).
- The server needs HTTPS in front of it (a reverse proxy on Èric's server) and its address in the app's configuration (#44).
- A small model may plan worse in Spanish, Catalan and English. If #69's scenarios show it, the switch is a bigger Gemma (`ASSISTANT_MODEL`), as far as Èric's server can run it.
- Deploying is Docker on Èric's server (`server/compose.yaml`), not `firebase deploy`.
- AGENTS.md's "Firebase only" now has one exception: this server, for the assistant only.
