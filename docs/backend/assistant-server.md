# The assistant server

Firebase is the backend for everything but the assistant: Authentication, and Firestore with its rules. The assistant runs on our own server: a small Python HTTP server in Docker, next to the model it asks ([ADR 0007](../adr/0007-assistant-on-our-own-server.md)). The app never calls an AI provider directly.

## Where it is

```
server/
├── assistant/           the code, one package (`python -m assistant` runs it)
│   ├── __init__.py      where the prompts and schemas are
│   ├── __main__.py      the entry point: serves with the environment's configuration
│   ├── api.py           the HTTP server (http.server): answers in the callable shape
│   ├── auth.py          the check of the caller's Firebase ID token
│   ├── interview.py     the interview and the check of what the model answers, testable without the server
│   ├── providers.py     the model it asks: Gemma 4 on Ollama, its size by configuration
│   ├── quota.py         each user's calls a day and a month, counted in SQLite
│   ├── net.py           JSON over HTTP
│   └── proposals.py     the catalogue's schemas, loaded whole for the model
├── prompts/             the prompts, as versioned files: question.v2.md, plan.v2.md
├── schemas/             what the assistant may propose, as JSON Schemas, and samples/ of each (the app reads them too)
├── tests/               pytest, one file per module, with a fake model and a fake token check
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
| Validation of items and proposals | `validateItem` runs before every write and on every proposal; the server checks proposals first (#65) |
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
- Every answer carries `X-Content-Type-Options: nosniff`, `Cache-Control: no-store` and `Strict-Transport-Security` (`SECURITY_HEADERS` in `api.py`, #324, from the OWASP ZAP scan).

## The interview

`POST /interview` speaks the shape of a Firebase callable: `{"data": …}` in, `{"result": …}` or `{"error": {status, message, details}}` out. It runs the whole interview (#66), one step per call.

The server keeps no state between calls (#64), so each call brings everything the next step needs:

```json
{"data": {
  "answers": [{"questionId": "q1", "question": "Who lives there?", "selected": ["Family"], "text": "two kids"}],
  "language": "es", "today": "2026-11-16", "newSpace": false,
  "members": [{"name": "Ana"}, {"name": "Ben"}],
  "items": ["Groceries"]
}}
```

- **Members go by name only**, never uid or email. The server gives each a handle by its place in the list (`m1`, `m2`), which is all the model sees of them, and the app maps a plan's handles back to uids in the same order. The user comes first, so `m1` is always the one talking to the assistant. A new space (`newSpace`) lists only the user; an existing one also sends its name (`space`), the first clue to what it is about, and `summary`, what the assistant learned about it before.
- **The space's summary.** Every plan also writes `summary`: what the assistant knows about the space now, its last `summary` with what this conversation added, at most 1000 characters (`checked_plan` cuts a longer one and drops one that is not text, keeping the plan). The review shows it in the confirmation, and confirming saves it on the space (`assistantSummary`) with the items, so each conversation starts from the last one. The app writes it, not the server: the server has no Firebase credentials of its own and never learns whether the user confirmed.
- **`language`** is the phone's (`es`, `ca`, `it`, `en`; anything else is English), and `today` its date, from which the plan's dates start.
- **The steps.** The first question is always the same, asked without the model: a hello and "What do you want to organise?", in the user's words. While fewer than `QUESTION_BUDGET` (5, from the environment) questions are answered, the model is then asked for the next question with `prompts/question.v2.md`: the details of what the user asked for (for a trip: how many, where, how, when), then how to share the work. It can answer `ready` instead; then, or once the budget is spent, it writes the plan with `prompts/plan.v2.md` and `PLAN_SCHEMA`, which `checked_plan` checks before it leaves. A new space's plan must name the space, and assigns only to the user or anyone; an existing space's never proposes one.
- **The prompts** explain every module with examples, and walk through an example conversation and example items. The plan covers only what the user asked to organise, never the rest of their life, and shares the work as they said. The plan's prompt lays out the next 14 days with their weekday numbers, and its examples' dates are filled in from `today`, so a small model reads dates instead of working them out and never copies one in the past.
- **Skip, Decide for me and Back (#74).** Each question carries a `suggestion`: what the model would answer, one of its options or a few words. "Decide for me" answers with it in the app and marks the answer `decided`, which the next prompt shows as "(chosen by the assistant)". A skipped answer has nothing selected nor written, and reads "(skipped)". Back happens in the app alone: the server keeps no state, so the next call simply sends fewer answers.
- **The prompts are files** in `server/prompts/`, versioned in their names: a change that changes what the model is asked for gets a new version.

| Call | HTTP | Body |
|---|---|---|
| Signed in, the next question | 200 | `{"result": {id, text, options, multiSelect, freeText, position, budget, suggestion}}`; `suggestion` only when there is one |
| Signed in, the plan | 200 | `{"result": {"plan": …}}`, in the shape of [the catalogue](#the-catalogue) |
| `data` is not an interview | 400 | `{"error": {"status": "INVALID_ARGUMENT", …}}`, before the model |
| No token, or not a valid one | 401 | `{"error": {"status": "UNAUTHENTICATED", …}}` |
| The caller's quota is spent ([the quota](#the-quota)) | 429 | `{"error": {"status": "RESOURCE_EXHAUSTED", …, "details": {"reason": "quota", "resetsAt": "2026-11-18T00:00:00Z"}}}`, before the model |
| The assistant is switched off ([the kill switch](#the-kill-switch)) | 412 | `{"error": {"status": "FAILED_PRECONDITION", …, "details": {"reason": "disabled"}}}`, before the model, whoever calls |
| The model failed or answered something invalid | 503 | `{"error": {"status": "UNAVAILABLE", …, "details": {"reason": "unavailable"}}}` |

`GET /health` answers `{"ok": true}`.

The refusals use these statuses, which the app reads (`InterviewMapper`):

| Status | The app shows |
|---|---|
| `RESOURCE_EXHAUSTED` | The quota is spent (#67): the day it is back (`resetsAt`, on the phone's calendar) and the manual path only |
| `FAILED_PRECONDITION` | The assistant is switched off (#68): the manual path only |
| `UNAVAILABLE` | Not answering: try again, or the manual path |

### The kill switch

`ASSISTANT_ENABLED=false` in the server's `.env` (or the Portainer stack's variables) turns the assistant off for everyone (#68). `POST /interview` then answers `FAILED_PRECONDITION` before the model is asked, and the app's interview says it is switched off and leaves the manual path, which is `AssistantError.DISABLED` (not retryable: asking again changes nothing). `GET /health` still answers, so the server is seen to be up. Unset, or anything but `false`, `0`, `no` or `off`, leaves it on.

The server reads the environment when it starts, so after changing it recreate the container (`docker compose up -d`, or redeploy the stack); a restart alone keeps the old value. It logs a warning at start while it is off.

### The quota

Each signed-in call to `POST /interview` counts against the caller's quota (#67): `ASSISTANT_DAILY_QUOTA` calls a UTC day and `ASSISTANT_MONTHLY_QUOTA` a UTC month. One interview takes a call per question plus one for the plan, so at most `QUESTION_BUDGET` + 1. The server keeps the counts itself, in SQLite (`QUOTA_DB`, on the `quota` volume in compose), out of every client's reach: it cannot sign in to Firestore, and a count the app could write is no limit.

The count is checked after the token and before the model. A caller over either limit gets `RESOURCE_EXHAUSTED` with `resetsAt`, the start of the next UTC day or month, and the refused call does not count. A call the model then fails still counts: it reached the model. A caller without a valid token is never counted. Limits per plan come with #120.

### The app's side

`ServerAssistantRepository` (`shared/…/data/repository/`) posts to `ASSISTANT_SERVER_URL` with the user's ID token, through Ktor, and waits up to three minutes for a small model. It reads the space's members and item titles from the repositories for each call (the user alone for a new space), and a failed read is a network error. No connection reads as a network error, and a reply that is not the callable shape (a proxy's error page) as `UNAVAILABLE`. `ASSISTANT_SERVER_URL` is `https://orbit.biri.es`, the Cloudflare tunnel to our server.

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
| `recurrence` | `unit` (`day`, `week`, `month`), `every`, `weekdays` for `week`, `dayOfMonth` for `month`, `until` | Nothing. Only with a schedule |
| `assignment` | `mode`: `member` with `assignees`, `rotation` with the `order` they take turns in (at least two; the first has it now), or `anyone` | `assignees` and `order` are the members' handles in the request (`m1`, `m2`), never uids: the app maps them back ([#238](https://github.com/Erkko68/orbit/issues/238)). A new space has only the user, so its plan assigns to the user or to anyone |
| `checklist` | `entries`: a list of `text` and `quantity` | A list in order, without ids or ticks: the app gives each entry its id and order |
| `reminder` | `offsetsMinutes` | Nothing. Only with a schedule |
| `effort` | `points`, 1 to 5 | Nothing. Credited to whoever completes it (#50) |

The schemas say the shape: the fields, their types, the date and time formats, the handles. What depends on more than one field (no `time` with an `endDate`, an `endDate` after the `date`, a reminder or a recurrence only with a schedule, an `until` not before the `date`, assignees who are in the request) the schemas cannot say to every model, so both sides check it, as described under [Checking proposals](#checking-proposals).

`proposals.py` loads the plan's schema with the space and item schemas written out in place (`PLAN_SCHEMA`), because a model is given one schema and cannot open the others. #66 hands it to the model at the end of the interview.

### Checking proposals

A model can still break a schema, or follow it and propose something the app would refuse by hand. So every proposal is checked twice, and one that fails is dropped, never shown (#65):

- **The server**, before it answers: `checked_plan(plan, handles)` in `proposals.py` checks each item against the item schema and then against the rules across its fields above, with `handles` the members the request names. It drops an item that fails and logs why (`Dropped proposal 2 of the plan: modules.cost is not allowed`). The runtime has only the standard library, so `schema_errors` checks the part of JSON Schema that `schemas/` uses, and the pytest checks it agrees with the `jsonschema` package on every sample and every invalid plan.
- **The app**, when it reads the reply: `planOf` (`ProposalMapper.kt`) reads each item on its own, with the strict `Json` so a field it does not know (an unknown module) makes that item unreadable. It maps each handle to its member's uid and checks the item with `validateItem`, as manual input is. `InterviewStep.PlanReady` carries only the items that pass, as `NewItem`s for the review board (#75). A dropped item is logged with Kermit.

A plan that is not one is not used at all, on either side: another version, no `items`, or a `space` without a name or with an accent outside the six colours. `checked_plan` raises a `ValueError`, which the interview (#66, which calls it on the model's plan) answers as `UNAVAILABLE`, and the app reads it the same way, with the manual path.

### Versions

The plan says which version it follows (`"version": 1`), and `PLAN_VERSION` is that number on both sides: in `proposals.py` and in the app's `ProposalDocuments.kt`. A plan in a version the app does not read is not used: the interview shows it as `UNAVAILABLE`, with the manual path. Until the assistant ships, version 1 changes in place: a new module (recurrence joined this way) goes into `item.v1.json`, its samples and the app's types in the same change. Once apps that read plans are released, a change an older app could misread, such as a new module, is a new version: new `*.v2.json` files next to the old ones, and the app's types with them.

### Samples

`server/schemas/samples/` holds sample plans in Spanish, Catalan and English. Both sides parse the same files: `tests/test_proposals.py` checks them against the schemas, and the app's `ProposalSamplesTest` reads them into its `@Serializable` types and writes them back unchanged, so a field one side does not know fails a test. A change to a schema updates its samples, and the app's types, in the same PR.

## Configuration

| Variable | Default | |
|---|---|---|
| `FIREBASE_API_KEY` | | An `orbit-app-dev` API key allowed to call the Identity Toolkit API, to check ID tokens. Make one for the server in Google Cloud (Credentials → API key → restrict to Identity Toolkit API): the apps' keys are restricted to the apps. In `.env`, never committed |
| `PUBLIC_URL` | | The server's public address (`https://…`). Sent as `Referer` on the token check, so the key can be restricted to that website |
| `HOST`, `PORT` | `0.0.0.0`, `8080` | Where the server listens |
| `ASSISTANT_MODEL` | `gemma4:e2b` | The Gemma tag in Ollama. The stack pulls it when it starts. `gemma4:e2b` is Gemma 4's smallest, made for phones, so it runs on a CPU in little memory; `gemma4:e4b` is the next size up |
| `ASSISTANT_REASONING_EFFORT` | `none` | How long the model thinks before it answers, sent as `reasoning_effort`. `none` turns thinking off, so a question takes seconds instead of minutes on a CPU; `low`, `medium` or `high` turn it back on |
| `ASSISTANT_ENABLED` | `true` | The kill switch: `false` (or `0`, `no`, `off`) turns the assistant off for everyone, and the app shows the manual path. See [the kill switch](#the-kill-switch) |
| `ASSISTANT_DAILY_QUOTA`, `ASSISTANT_MONTHLY_QUOTA` | `50`, `500` | Each user's calls a UTC day and a UTC month. `0` refuses every call. See [the quota](#the-quota) |
| `QUOTA_DB` | `quota.sqlite3` | The SQLite file with the counts. Compose sets `/data/quota.sqlite3`, on the `quota` volume, so they survive a redeploy |
| `MODEL_URL` | `http://ollama:11434/v1` | Ollama's OpenAI-compatible endpoint |
| `ASSISTANT_PORT` | `8080` | The host port compose publishes the server on |

## Deploying

On the server, from `server/`:

```sh
docker compose up -d --build
```

The `model` service pulls `ASSISTANT_MODEL` into Ollama and exits, and the assistant starts only after it. The first start downloads the model, so it takes a few minutes; later ones only check it.

On our server it runs as a Portainer stack from this repository (compose path `server/compose.yaml`, `FIREBASE_API_KEY` and `ASSISTANT_PORT` as the stack's variables), behind a Cloudflare tunnel. The published port needs HTTPS in front of it (a reverse proxy or a tunnel): the app only talks HTTPS. Ollama is not published, so only the server reaches the model.

The Firestore rules and indexes still deploy from `firebase/`: `npx firebase deploy --only firestore:rules,firestore:indexes`.

## Tests and coverage

pytest covers the server with a fake model and a fake token check, so the tests never call an AI or Firebase. CI runs them in the `Assistant server tests` job, and `sonarqube.yml` sends their coverage to SonarCloud, which needs 80% on new code like the Kotlin side.

```sh
cd server
python3 -m venv .venv && source .venv/bin/activate   # .venv\Scripts\activate on Windows
pip install -r requirements-dev.txt
pytest --cov
```

To run it on your machine against Ollama there (`ollama pull gemma4:e2b` first):

```sh
FIREBASE_API_KEY=… MODEL_URL=http://localhost:11434/v1 python -m assistant
```
