# Cloud Functions

Firebase is the only backend: Authentication, Firestore and Cloud Functions. There are no other servers or REST APIs, and the app never calls an AI provider directly.

## Where it is

```
firebase/
├── firebase.json        rules, functions (Python 3.13) and emulator config
├── firestore.rules      see ../data/security-rules.md
├── functions/
│   ├── main.py          the functions; today only initialize_app()
│   └── requirements.txt firebase-functions, firebase-admin
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
- Provider keys live in the Cloud Functions secret manager, never in the repo or the app.
- What the assistant returns is a proposal. The function never writes it to Firestore; the app saves it only after the user confirms.
- Every assistant flow has a manual path in the app.

## Deploying

```sh
cd firebase
npx firebase deploy --only firestore:rules   # rules
npx firebase deploy --only functions         # functions (needs the Blaze plan)
```

## Coverage

The Python code has no coverage report yet, so `sonar.coverage.exclusions` skips `firebase/**`. Add pytest with coverage when the first real function lands (#43), and remove the exclusion for `firebase/functions/**` then.
