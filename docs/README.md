# Orbit documentation

Orbit is a Kotlin Multiplatform app (Android and iOS) for groups that share responsibilities. This folder explains how it is built and why. The rules every change must follow are in [AGENTS.md](../AGENTS.md); these pages are the background to them.

| Section | Read it to | Pages |
|---|---|---|
| **Product** | Know what Orbit does, for whom, and the words the code uses | [Overview and glossary](product/overview.md) |
| **Architecture** | Find your way around the code and add a feature the way the others are built | [Overview](architecture/overview.md) · [State and ViewModels](architecture/state-and-viewmodels.md) · [Offline and sync](architecture/offline-and-sync.md) · [Authentication](architecture/authentication.md) · [Design system](architecture/design-system.md) |
| **Data** | Read or change what is stored, and who may change it | [Firestore schema](data/firestore-schema.md) · [Security rules and roles](data/security-rules.md) |
| **Backend** | Work on the Cloud Functions | [Cloud Functions](backend/cloud-functions.md) |
| **Process** | Take an issue to a merged pull request | [Workflow](process/workflow.md) · [Testing](process/testing.md) · [Builds and deployment](process/builds-and-deployment.md) · [Project automation](process/project-automation.md) |
| **Decisions** | See why something is the way it is, and what was rejected | [Architecture decision records](adr/README.md) |
| **Design** | Look up past design deliveries | [Sprint 1 Stitch mockups](design/sprint-1-stitch/handoff.md) (historical) |

## Where things are decided

Each fact has one home, and the other pages link to it instead of repeating it.

| Fact | Home |
|---|---|
| A rule a change must follow | [AGENTS.md](../AGENTS.md) |
| Collections, fields, queries and indexes as they are today | [Firestore schema](data/firestore-schema.md) |
| Who may read or write what | [Security rules and roles](data/security-rules.md), enforced by [`firebase/firestore.rules`](../firebase/firestore.rules) |
| Why a choice was made | An [ADR](adr/README.md) |
| Library versions | [`gradle/libs.versions.toml`](../gradle/libs.versions.toml) |
| Colors, type, shapes and spacing | `core/designsystem/theme/` in the code |
| What is being worked on | The [project board](https://github.com/users/Erkko68/projects/3) |

## Keeping these pages true

A pull request that changes what a page describes updates the page in the same PR: a schema change updates the schema page, a new pattern the architecture pages. A page that no longer matches the code is a bug: open an issue for it.
