# The assistant server

Firebase is the backend for everything but the assistant: Authentication, and Firestore with its rules. The assistant runs on our own server: a small Python HTTP server in Docker, next to the model it asks ([ADR 0007](../adr/0007-assistant-on-our-own-server.md)). The app never calls an AI provider directly.

## Where it is

```
server/
├── main.py              the HTTP server (http.server): answers in the callable shape
├── auth.py              the check of the caller's Firebase ID token
├── assistant.py         the assistant and the check of what the model answers, testable without the server
├── models.py            the models it can ask (Gemma 4 on Ollama, Gemini), chosen by configuration
├── net.py               JSON over HTTP
├── proposals.py         the catalogue's schemas, loaded whole for the model
├── prompts/             the prompts, versioned as files
├── schemas/             what the assistant may propose, as versioned JSON Schemas, and samples/ of each
├── tests/               pytest, with a fake model and a fake token check
├── Dockerfile           the server's image (port 8080); installs nothing
├── compose.yaml         the server and Ollama, side by side
└── requirements-dev.txt pytest, pytest-cov, jsonschema: the tests' only dependencies

The server is Python's standard library only: no web framework, no Firebase or Google SDK. Its image installs nothing, so there are no dependencies to pin, patch or trust at build time.
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

- Every request carries the caller's Firebase ID token (`Authorization: Bearer <token>`). The server asks Firebase Authentication whether it is valid (Identity Toolkit's `accounts:lookup`, with `FIREBASE_API_KEY`), and rejects the call before the model without a valid one.
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

The request's `data` carries every answer so far, `{"answers": [{questionId, selected, text}]}`: the server keeps no state between calls (#64). A plan comes back as `{"result": {"plan": …}}` once #66 builds one, in the shape of [the catalogue](#the-catalogue). The refusals still to come use these statuses, which the app already reads:

| Status | The app shows |
|---|---|
| `RESOURCE_EXHAUSTED` | The quota is spent (#67): the manual path only |
| `FAILED_PRECONDITION` | The assistant is switched off (#68): the manual path only |
| `UNAVAILABLE` | Not answering: try again, or the manual path |

### The app's side

`ServerAssistantRepository` (`shared/…/data/repository/`) posts to `ASSISTANT_SERVER_URL` with the user's ID token, through Ktor, and waits up to three minutes for a small model. No connection reads as a network error, and a reply that is not the callable shape (a proxy's error page) as `UNAVAILABLE`. `ASSISTANT_SERVER_URL` is a placeholder (`https://assistant.orbit.invalid`) until the server has its domain; set it there.

## The catalogue

What the assistant may propose, and nothing else (#64). Each proposal is a JSON Schema in `server/schemas/`, and each maps to something the app already does by hand, so every proposal has its manual path:

| Proposal | Schema | What confirming it does in the app |
|---|---|---|
| A new space | `space.v1.json`: `name`, `accent` (0 to 5, which of the six space colours) | `SpaceRepository.createSpace(name, accent)` |
| An item | `item.v1.json`: `title`, `description`, `modules` | `ItemRepository.create(spaceId, item)` with a `NewItem`, checked by `validateItem` |
| The plan | `plan.v1.json`: `version`, `space` (only for a new space), `items` | The review board (#75) shows each proposal, and only what the user confirms is saved (#76) |

An item's modules are the ones the app has today, each at most once, in the shape the item stores them ([schema](../data/firestore-schema.md#spacesspaceiditemsitemid)) except for two things the app fills in when it saves:

| Module | Fields | Differs from what is stored |
|---|---|---|
| `schedule` | `date`, `time`, `endDate`, `durationMinutes` | Nothing |
| `assignment` | `mode`: `member` with `assignees`, or `anyone` | `assignees` are the members' handles in the request (`m1`, `m2`), never uids: the app maps them back ([#238](https://github.com/Erkko68/orbit/issues/238)). A new space has only the user, so its plan assigns to the user or to anyone |
| `checklist` | `entries`: a list of `text` and `quantity` | A list in order, without ids or ticks: the app gives each entry its id and order |
| `reminder` | `offsetsMinutes` | Nothing. Only with a schedule |

The schemas say the shape: the fields, their types, the date and time formats, the handles. What depends on more than one field (no `time` with an `endDate`, an `endDate` after the `date`, a reminder only with a schedule, assignees who are in the request) the schemas cannot say to every model, so the server checks it before answering (#65) and the app checks it again with `validateItem` before saving.

`proposals.py` loads the plan's schema with the space and item schemas written out in place (`PLAN_SCHEMA`), because a model is given one schema and cannot open the others. #66 hands it to the model at the end of the interview.

### Versions

The plan says which version it follows (`"version": 1`), and `PLAN_VERSION` is that number on both sides: in `proposals.py` and in the app's `ProposalDocuments.kt`. A plan in a version the app does not read is not used: the interview shows it as `UNAVAILABLE`, with the manual path. A change an older app could misread, such as a new module, is a new version: new `*.v2.json` files next to the old ones, and the app's types with them. Recurrence, rotation, cost and effort join this way as each lands.

### Samples

`server/schemas/samples/` holds sample plans in Spanish, Catalan and English. Both sides parse the same files: `tests/test_proposals.py` checks them against the schemas, and the app's `ProposalSamplesTest` reads them into its `@Serializable` types and writes them back unchanged, so a field one side does not know fails a test. A change to a schema updates its samples, and the app's types, in the same PR.

## Configuration

| Variable | Default | |
|---|---|---|
| `FIREBASE_API_KEY` | | An `orbit-app-dev` API key allowed to call the Identity Toolkit API, to check ID tokens. Make one for the server in Google Cloud (Credentials → API key → restrict to Identity Toolkit API): the apps' keys are restricted to the apps. In `.env`, never committed |
| `HOST`, `PORT` | `0.0.0.0`, `8080` | Where the server listens |
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

The Firestore rules and indexes still deploy from `firebase/`: `npx firebase deploy --only firestore:rules,firestore:indexes`.

## Tests and coverage

pytest covers the server with a fake model and a fake token check, so the tests never call an AI or Firebase. CI runs them in the `Assistant server tests` job, and `sonarqube.yml` sends their coverage to SonarCloud, which needs 80% on new code like the Kotlin side.

```sh
cd server
python -m venv venv && source venv/bin/activate   # venv\Scripts\activate on Windows
pip install -r requirements-dev.txt
pytest --cov
```
