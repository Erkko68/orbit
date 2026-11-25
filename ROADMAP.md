# Roadmap: Sprint 3

Sprint 3 runs from **November 25** to **December 22, 2026**. It was planned in #330.

This page says what changed, what each track builds and in which order. Dates, estimates and status live on the [project board](https://github.com/users/Erkko68/projects/3).

## What changed

- **The focus:** a smarter assistant, filters and an interactive calendar, receipt scanning, real roles, the quality-of-life "Should" features, and stabilisation.
- **Monetisation moved to the backlog** (#117–#122, #97). #81 closes as the spike, and ADR 0017 keeps what it found.
- **Push notices on Android only** (#102, #90, #103). The project has no paid Apple Developer Program, so iOS has no APNs key. On iOS, reminders still catch up when the app opens.
- **Roles become space permissions** (#331): the owner and admins decide what members may do. The child profile (#109) waits in the backlog to become a preset of them.
- **Closed as not planned:** #113 Sign in with Apple and #126 store readiness (no Apple program, no store launch), and #92 editable space profile (the assistant keeps no memory since #329).
- **Rescoped:** #103 drops quiet hours (the phone's Do Not Disturb does that), #85 is small because item writes are already per field, and #96 finds its candidates with rules on the device, without the model.
- **Every issue has "Done when" items, an estimate and a priority.** Read them before you start.

## Who builds what

Take any free issue in a track. Assign yourself the day you start.

| Track | Issues | Hours |
|---|---|---|
| **Carried over** | #51, #81, #78, #102 | 18 |
| **Assistant** | #89, #93, #88, #86, #112, #87, #96 | 32 |
| **Receipts** | #98, #99, #100 | 13 |
| **Calendar and filters** | #107, #270, #332, #333 | 15 |
| **Roles and assignments** | #331, #91, #90 | 17 |
| **Notifications** | #101, #103 | 6 |
| **History and undo** | #83, #84, #85 | 13 |
| **Space and settings** | #111, #114, #105, #116, #108, #115 | 21 |
| **Stabilisation** | #123, #125, #127, #124 | 18 |
| **iOS** (Èric) | #334 | 4 |

That is 157 h, plus 2 h of planning (#330). Of those, 132 h are P0 and P1, the commitment. The 25 h of P2 (#87, #96, #100, #103, #108, #115, #124, #333) are the reserve: they go in only if unplanned work leaves room.

## Order

**Week 1, to December 1: finish and unblock**

1. The carried-over issues: #51, #81, #78, #102. #102 starts with its ADR: how the assistant server signs FCM messages and finds the recipients.
2. #331: the permissions ADR and rules first, because #84, #89 and #91 follow them.
3. #83: the activity history. This unblocks #84 and #85, and labels the assistant's changes.
4. #123: end-to-end tests of the main flows, at the start of the sprint as planned.

**Weeks 2 and 3, to December 15: the features**

- Every track in "Blocked by" order, P0 first: #89, #112, #101, #84, #85, #111, #114.
- Then P1: the assistant (#93, #88, #86), receipts (#98, #99), calendar (#107, #332, #270), roles (#91, #90), space (#105, #116).

**Last week, to December 22: stabilisation**

- #125 accessibility, #127 documentation, and fixing what the end-to-end tests and the two-device check find.
- P2 only if there is room.

## Working this sprint

- **Android first, iOS on the simulator.** Issues are verified on Android. Èric checks them on the iOS simulator as they merge (#334).
- **The assistant server needs tests too.** New endpoints ship with pytest tests, and the app's manual path for every assistant flow stays (F8.13).
- **A rules change carries its docs.** Permissions, history, acceptance and currency all touch `firestore.rules`: update `docs/data/` and the rules tests in the same PR.
- **New issues are estimated on entry**, and committed work comes first.
