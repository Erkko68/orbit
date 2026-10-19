# Product overview

## What Orbit is

Orbit organises any group that shares responsibilities: flatmates, families, couples, trips, sports teams, study groups. Each group is a **space**. Its members share tasks, rotating chores, bills, shopping lists, events and expenses, and every change syncs to the others in real time.

Setting a space up should not mean filling in forms. A cloud AI assistant asks a few short questions and proposes a plan for the space; the user reviews each proposal and confirms it. **Nothing the assistant proposes is saved without that confirmation**, and everything it can do can also be done by hand.

## Principles

These shape every feature, and AGENTS.md turns them into rules.

- **Shared by default.** Every member can create, edit and complete what is in the space. Roles only guard what changes the space itself (see [roles](../data/security-rules.md#roles)).
- **Works offline.** Reading and editing a space works without a connection; changes sync when it comes back ([offline and sync](../architecture/offline-and-sync.md)).
- **The assistant proposes, people decide.** AI output is always a proposal shown for review, and only a confirmation persists it.
- **The phone already knows.** Language, time zone and theme come from the phone's settings, never from settings inside the app.
- **One flexible item.** There is no separate "task", "bill" or "event" type: an item is a title plus the modules it needs. A preset is a starting set of modules.

## Scope by sprint

Each sprint is a GitHub milestone; the [project board](https://github.com/users/Erkko68/projects/3) has the issues.

| Sprint | Goal | Main pieces |
|---|---|---|
| 1 | A shared space that works on two phones | Accounts (email, Google), spaces and members with roles, invitations, items with Schedule, Assignment, Checklist and Reminder modules, presets, local reminders, security rules |
| 2 | Planning help and money | The assistant interview and plan review, agenda and month view, recurrence and rotating chores, expenses and balances, bills, localisation in Spanish, Catalan and English |
| 3 | A product people could pay for | Plans and paywall, ads with consent, receipt scanner, activity history and undo, templates and custom presets, child profiles, accessibility and performance passes |
| 4 | Delivery | Pitch, demo, landing page, final documentation and retrospective |

## Glossary

The words below mean the same thing in the code, the issues and these pages.

| Term | Meaning | In the code |
|---|---|---|
| **Space** | A group and everything it shares | `Space`, `spaces/{spaceId}` |
| **Member** | A user's membership of one space, with a role and a copy of their public profile | `Member`, `spaces/{id}/members/{uid}` |
| **Role** | Owner, admin or member: what a member may change in the space | `Role`, `Role.managesSpace` |
| **Owner** | The one member who can delete the space or hand it over | `Role.OWNER` |
| **Invitation** | A code (and link) that lets whoever holds it join a space, for a week | `Invitation`, `invitations/{code}` |
| **Item** | Anything the space keeps track of: a title, a status and modules | `Item`, `spaces/{id}/items/{itemId}` |
| **Module** | One capability of an item. Today: Schedule, Assignment, Checklist, Reminder | `Module`, `ItemModules` |
| **Preset** | A label and a starting set of modules: task, shopping list, event | `Preset`, `Preset.defaultModules` |
| **Proposal** | What the assistant suggests, shown for review before anything is saved | Sprint 2 |
| **Profile** | The user's name and photo, private in `users/{uid}` and copied into each membership | `User` |
