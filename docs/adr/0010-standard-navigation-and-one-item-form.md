# ADR 0010: Places are screens on the stack, and an item is one fixed form

- Status: Accepted
- Date: 2026-11-04
- Issue: #241

## Context

By the end of sprint 2 each screen had a pattern of its own. The space settings were one sheet paging through six steps, so back and swipe-down did different things and nothing could be linked to. The item screen had a header that was also a form, two texts (ADR 0008's summary and details), and a layout that changed shape when a checklist was added. The Tasks tab mixed three looks: chips that scrolled sideways, lists as staggered tiles and tasks as cards, for what ADR 0007 made the same `Item`. Members' faces drifted with physics in the top bar and on the space tiles, where nothing said they could be tapped.

The proposal [Orbit: simpler navigation and screens](https://claude.ai/artifact/HidPvguvGcXonciy3QVPw7) listed these and was agreed with these changes: the item keeps its colour header with the title typed in place, the turn card stays the one hero moment, a checklist still finishes its item, and the faces in the top bar get a settings button beside them.

## Decision

**A place the user goes is a screen on the navigation stack, and an item is the same form whatever it holds.**

- **Places are routes.** Space settings (`SpaceSettingsRoute`) and a member (`MemberRoute`) are pushed screens, with a back arrow, system back and swipe-back that all go back. Sheets are only for a short task (who, when, the reminder, an invitation, a role) and dialogs for what cannot be undone (leaving, deleting, handing over).
- **One item form.** The colour header with the title typed in place (the pencil says so), then Who, When and Reminder rows that show their value or what tapping adds, one Description, the checklist always there with its add row last, and the history. One button stays at the bottom.
- **One description.** `description` is the item's one text. `details` is retired: on read an item that has both shows them joined by a blank line, and the next save of the description writes the result and deletes `details`. No migration and no rules change.
- **Create, then autosave.** A new item keeps everything on the device until the user taps Create, which writes it in one go and closes the screen. A written item saves each change as it is made, as before.
- **A checklist still finishes its item** (ADR 0008). Mark done shows only on an item without entries, and on Tasks the check of a list opens it.
- **Lists are lists.** Every item is the same row in Tasks: the check, the title, the first line of the description, when it is due and how far along a list is, and the faces at the end. Sections are To do and Done, Done folded to its count. The filters fit in one row.
- **Still faces.** Members show as a stack of up to four faces and "+N" (`OrbitFaceStack`), on the space cards, in the top bar and on rows. Nothing moves on its own.
- **The history says what happened.** Reopening is recorded like completing, in the same append-only collection with `action: 'reopen'`, so the item's history reads "Ana marked it done", "You reopened it", ending with who created it.

## Alternatives considered

- **Keep the settings sheet and fix its back.** Back inside a sheet stays a special case, and a sheet cannot be linked to.
- **A plain top bar and a Name field on the item** (the proposal's first version). Tried on a phone: the colour block with the title in place read better, as long as nothing else in it is edited.
- **Always show Mark done, ticking what is left.** One button for every item, but it finishes a list with entries left, which ADR 0008 rejected.
- **Infer reopenings from the completions.** Two completions in a row show that it was reopened in between, but not who did it or when.

## Consequences

- `OrbitFloatingFaces`, its physics, `OrbitCrew`, `OrbitListTile`, `OrbitSheetColorHeader`, `OrbitHeaderAction`, `OrbitWavyProgress` and the `header` slot of `OrbitSheet` are gone. `OrbitTaskCard` became `OrbitTaskRow`.
- Leaving a new item before Create keeps nothing, without asking.
- A history entry is either a completion or a reopening. Whoever reads the collection for completions (a member's "Lately", rotation and fairness from #49) skips `action: 'reopen'`.
- Supersedes ADR 0008's two texts and its checklist mode. Its rule that a checklist finishes its item stays.
