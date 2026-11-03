# Cloud Functions

Firebase is the only backend: Authentication, Firestore and Cloud Functions. There are no other servers or REST APIs, and the app never calls an AI provider directly.

## Where it is

```
firebase/
├── firebase.json        rules, functions (Python 3.13) and emulator config
├── firestore.rules      see ../data/security-rules.md
├── functions/
│   ├── main.py          the callables and their Firebase wiring (params, Gemini on Vertex AI)
│   ├── assistant.py     the assistant and the check of what the model answers, testable without Firebase
│   ├── prompts/         the prompts, versioned as files
│   ├── tests/           pytest, with a fake model
│   ├── requirements.txt firebase-functions, firebase-admin, google-genai
│   └── requirements-dev.txt pytest, pytest-cov
├── tests/rules.test.js  rules tests (emulator)
└── package.json         Node tooling for the rules tests and the Firebase CLI
```

Run the Firebase CLI from `firebase/`.

## What runs here, and what does not

A function is worth it only for what the phone cannot or must not do. Everything else stays in the app, where it works offline and is covered by the Kotlin tests.

| Runs in a function | Why |
|---|---|
| The assistant (#43, #64–#76) | Provider keys must stay on the server, and quotas and the kill switch must be enforced there (#67, #68) |
| Entitlements and plan quotas (#117, #120) | The client cannot be trusted with what it is allowed |
| Push notices to other members (#102) | Only the server knows when someone else changed something |

| Stays on the device | Why |
|---|---|
| Validation of items and proposals | `validateItem` runs before every write; the function validates proposals again (#65) |
| Balances and debt simplification | A pure function of the ledger, offline ([ADR 0003](../adr/0003-balances-on-device.md)) |
| Joining a space | A security rule checks the invitation ([ADR 0002](../adr/0002-firestore-data-model.md)) |
| Local reminders | Each phone schedules its own |

## Rules for functions

- Callable functions check `request.auth` and reject unauthenticated calls.
- Provider keys live in the Cloud Functions secret manager, never in the repo or the app. Gemini needs none: it runs on Vertex AI as the function's service account.
- The assistant takes the model as a plain function (`generate(prompt, schema)`), and the model is configuration (`ASSISTANT_MODEL`, `ASSISTANT_LOCATION`), so #70 can change it in `main.py` without touching the assistant.
- What the provider answers is checked before it reaches the app. An answer that is not valid is an `unavailable` error, never passed on.

## The assistant

`interview` is a callable. For now it returns the first question of the interview (#43); #66 turns it into the whole interview.

| Call | Result |
|---|---|
| Signed in | `{id, text, options, multiSelect, freeText, position, budget}` |
| Signed out | `unauthenticated` error |
| The provider failed or answered something invalid | `unavailable` error, details `{"reason": "unavailable"}` |

The project needs the Blaze plan and the Vertex AI API enabled (`gcloud services enable aiplatform.googleapis.com`). The function's service account calls Vertex AI with its default role.
- What the assistant returns is a proposal. The function never writes it to Firestore; the app saves it only after the user confirms.
- Every assistant flow has a manual path in the app.

## Deploying

```sh
cd firebase
npx firebase deploy --only firestore:rules   # rules
npx firebase deploy --only functions         # functions (needs the Blaze plan)
```

## Tests and coverage

pytest covers the functions with a fake model, so the tests never call an AI. CI runs them in the `Cloud Functions tests` job, and `sonarqube.yml` sends their coverage to SonarCloud, which needs 80% on new code like the Kotlin side.

```sh
cd firebase/functions
python -m venv venv && source venv/bin/activate   # venv\Scripts\activate on Windows
pip install -r requirements.txt -r requirements-dev.txt
pytest --cov
```
