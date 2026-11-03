# ADR 0007: The assistant runs on our own server, next to its model

- Status: Accepted
- Date: 2026-11-03
- Issue: #258 (follows #43)

## Context

The assistant (#43, #64–#76) was planned as Cloud Functions calling Gemini on Vertex AI. Both need the Blaze plan, which means linking a card to the Firebase project, and the team does not want to risk paying for a university project.

What stays fixed: the app never calls an AI provider directly, provider keys never reach the app or the repo, the server enforces the quota (#67) and the kill switch (#68), and AI output is only a proposal until the user confirms it.

## Decision

**The assistant's backend is a small Python HTTP server in Docker, on Èric's server, next to the model it asks. The default model is Gemma 4, served by Ollama on the same machine; Gemini's free API is the fallback, by configuration.**

- **No card, no bill.** Firebase stays on the free Spark plan. Verifying a Firebase ID token with `firebase-admin` needs no billing.
- **The server still decides.** It checks the caller's ID token on every request and keeps the quota and the kill switch on our side, as the Cloud Function would have.
- **The callable shape stays.** `POST /interview` takes `{"data": …}` and answers `{"result": …}` or `{"error": {status, message, details}}`, with the same statuses (`UNAUTHENTICATED`, `UNAVAILABLE`), so #44 calls it the way it would call a callable.
- **The model is configuration.** `ASSISTANT_PROVIDER=local` asks any OpenAI-compatible endpoint (Ollama with Gemma 4); `ASSISTANT_PROVIDER=gemini` asks Gemini with `GEMINI_API_KEY` from the server's environment. #70 compares them with the scenarios of #69.
- **The code from #43 carries over.** The question, its validation and their tests do not depend on where they run; only the wrapper changed, from `firebase-functions` to Flask.

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
- A small model may plan worse than Gemini in Spanish, Catalan and English. If #69's scenarios show it, the switch is `ASSISTANT_PROVIDER=gemini`. Gemini's free tier may use the data to improve Google's products, so the AI consent (#71) has to say so if it becomes the model.
- Deploying is Docker on Èric's server (`server/compose.yaml`), not `firebase deploy`.
- AGENTS.md's "Firebase only" now has one exception: this server, for the assistant only.
