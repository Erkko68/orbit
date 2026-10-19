# Orbit Stitch Mockups — Sprint 1 Handoff

> **Historical.** This is the Sprint 1 design delivery, kept as it was handed over, with its mockups in [`mockups.zip`](mockups.zip). The app has moved on: the [design system](../../architecture/design-system.md) and the style guide in AGENTS.md ("UI and theme", from #198) are what the screens follow now. Where this page and the code differ, the code wins.

## 1. Delivery status and scope

This handoff contains the completed **Sprint 1 Orbit mockups** created in Google Stitch.

The original issue lists Sprint 1 first and then a second group of screens (Shared Calendar, Expenses, Live Shared List, Assistant, Receipt Scanner and Settings). Before implementation, the maintainer clarified that this delivery should cover **the Sprint 1 screens first**. Before handoff, the maintainer confirmed that the remaining screens will be handled in **a new issue**. They are therefore intentionally **outside this Sprint 1 handoff**.

Sprint 1 delivered here includes:

- Sign-in / onboarding (Sign in + Create account)
- Space home
- Create / join a space
- Item list
- Item detail for task, chore, bill, list and event

Every delivered screen has both a Light and Dark variant.

**Total delivered:** 10 screen types / 20 final mockups.

Google Stitch project:

https://stitch.withgoogle.com/projects/10551559262543958633

## 2. Package contents

```text
orbit_stitch_mockups/
├── DESIGN.md
├── HANDOFF.md
├── MANIFEST.json
└── screens/
    ├── sign_in_light_mode/
    │   ├── code.html
    │   └── screen.png
    ├── sign_in_dark_mode/
    │   ├── code.html
    │   └── screen.png
    ├── ...
    └── item_detail_event_dark_mode/
        ├── code.html
        └── screen.png
```

Each final screen folder contains:

- `screen.png` — the reviewed visual reference for that variant.
- `code.html` — the Stitch-generated browser reference for the same screen.

`MANIFEST.json` lists the exact 20 screen folders retained in this package.

### Reference viewport and screenshot pixel dimensions

`DESIGN.md` documents **360×800** as the general mobile reference viewport for the design system. During the Stitch review, the Sprint 1 screens were also inspected in approximately 390px-wide mobile preview frames, and Stitch produced capture files at different pixel scales.

These values are mockup/capture references, not production dimensions. **Do not infer a fixed production width or height from the raw PNG pixel dimensions.** Production implementation must remain responsive/mobile-first and follow the application design system.

Some detail/list `screen.png` files are **viewport captures, not full-scroll composites**. When a card continues below the bottom edge, the remaining content is intentionally available by scrolling and is documented in this handoff and/or the corresponding `code.html`; it should not be interpreted as missing content.

## 3. Sources of truth

The Stitch export is not the production design system.

Use the following order when implementing or resolving discrepancies:

1. **Production theme/code (`Color.kt`, `OrbitTheme`, design-system wrappers)** — authoritative for production colors, semantic roles and theme behavior.
2. **`DESIGN.md` in this package** — the approved mockup guidance aligned with the Orbit codebase; it documents layout, spacing, component and mockup rendering rules.
3. **Final `screen.png` for the specific screen** — authoritative for the reviewed layout/composition of that mockup.
4. **`code.html`** — implementation/reference aid only; useful for content structure, approximate measurements and limited preview behavior.
5. **Google Stitch project** — useful for visual inspection, but Stitch may retain generation history and may regenerate its own Material 3 tokens.

### Important Stitch / production boundary

During the mockup work it was confirmed with the maintainer that the main purpose of these Stitch screens is to provide the team with a **shared layout reference**. Stitch can generate or replace Material 3 colors/tokens internally, so exported Tailwind configuration, generated theme tokens and unused CSS are **not authoritative**.

Production implementation must use `Color.kt`, `OrbitTheme` and the existing design-system components instead of copying raw Stitch styling.

The mockups were still reviewed for consistency and obvious visual regressions, but exact production theme values should always come from the codebase.

## 4. Final screen inventory

### Sign in — Light / Dark

Purpose: authentication entry point for an existing user.

Includes:

- Orbit wordmark
- `Welcome back`
- Email field
- Password field with visibility control
- `Forgot password?`
- Primary `Sign in` action
- Google / Apple authentication actions
- `Create account` entry point

Folders:

- `screens/sign_in_light_mode/`
- `screens/sign_in_dark_mode/`

### Create account — Light / Dark

Purpose: new-user registration.

Includes:

- Email
- Password
- Confirm password
- `Use at least 8 characters.` helper text
- Primary `Create account` action
- Google / Apple authentication actions
- `Sign in` entry point for existing users

Folders:

- `screens/create_account_light_mode/`
- `screens/create_account_dark_mode/`

### Create / Join Space — Light / Dark

Purpose: first shared-space setup.

Includes two paths:

- `Create a new space` — example space name `Flat 4B`
- `Join a space` — invite code or link input

No bottom navigation is used on this onboarding/setup screen.

Folders:

- `screens/create_join_space_light_mode/`
- `screens/create_join_space_dark_mode/`

### Space Home — Light / Dark

Purpose: overview of the active shared space.

Context and content:

- Active space: `Flat 4B`
- Member avatars / identity accents
- Summary: `5` open tasks, `2` bills due, `1` event today
- `Take out recycling` — completed / Done
- `Electricity` — due in 2 days
- `Dinner at 20:00` — event
- `Book plumber` — urgent task
- Extended coral `New item` FAB

Bottom navigation order:

1. Home
2. Calendar
3. Expenses
4. Assistant
5. Settings

Calendar, Expenses, Assistant and Settings are shown here as part of the shared navigation shell. Their destination screens belong to the follow-up issue and are not part of this Sprint 1 package.

Folders:

- `screens/space_home_light_mode/`
- `screens/space_home_dark_mode/`

### Item List — Light / Dark

Purpose: list all item types in the active space.

Header:

- `Items`
- `Flat 4B`

Horizontal filters:

- All
- Tasks
- Chores
- Bills
- Lists
- Events

Example rows:

- `Take out recycling` — Chore / Done
- `Book plumber` — Task / Urgent
- `Electricity` — Bill / Due soon
- `Groceries` — List / 6 items / 2 checked
- `Dinner at 20:00` — Event / Tonight
- `Clean bathroom` — Chore / Tomorrow

Includes the coral `New item` FAB.

This nested list screen does not use bottom navigation.

Folders:

- `screens/item_list_light_mode/`
- `screens/item_list_dark_mode/`

### Task Detail — Light / Dark

Example:

- Title: `Book plumber`
- Helper: `Tap the checkbox to mark as done`
- Type: `Task`
- Status: `Urgent`
- Description: `Kitchen leak`

Details:

- Due date: `Oct 26, 2026`
- Created by: `AL`
- Created: `Oct 8, 2026`
- Space: `Flat 4B`
- Assigned to: `AL`

Folders:

- `screens/item_detail_task_light_mode/`
- `screens/item_detail_task_dark_mode/`

### Chore Detail — Light / Dark

Example:

- Title: `Take out recycling`
- Completed checkbox state
- Completed title with 2px strikethrough
- Helper: `Tap the checkbox to mark as open`
- Type: `Chore`
- Status: `Done`
- Description: `Take recycling to the building bins`

Details:

- Completed: `Oct 10, 2026`
- Frequency: `Weekly`
- Created by: `AL`
- Created: `Oct 1, 2026`
- Space: `Flat 4B`
- Assigned to: `SJ`

Folders:

- `screens/item_detail_chore_light_mode/`
- `screens/item_detail_chore_dark_mode/`

### Bill Detail — Light / Dark

Example:

- Title: `Electricity`
- Type: `Bill`
- Status: `Due soon`
- Amount due: `$84`
- Due: `Oct 12, 2026`
- Description: `October electricity bill`

Details:

- Due date: `Oct 12, 2026`
- Created by: `AL`
- Created: `Oct 1, 2026`
- Space: `Flat 4B`

Primary action:

- `Mark as paid`

Folders:

- `screens/item_detail_bill_light_mode/`
- `screens/item_detail_bill_dark_mode/`

### List Detail — Light / Dark

Example:

- Title: `Groceries`
- Type: `List`
- Progress: `2 of 6 checked`
- Progress bar
- Milk — checked
- Eggs — checked
- Pasta — unchecked
- Tomatoes — unchecked
- Coffee — unchecked
- Dish soap — unchecked
- `Add item` action

Details:

- Created by: `AL`
- Created: `Oct 5, 2026`
- Space: `Flat 4B`

The exported preview includes checklist toggling and progress updates for demonstration. Production state management must be implemented in the app architecture.

Folders:

- `screens/item_detail_list_light_mode/`
- `screens/item_detail_list_dark_mode/`

### Event Detail — Light / Dark

Example:

- Title: `Dinner at 20:00`
- Type: `Event`
- When: `Today · 20:00`
- Date: `Oct 10, 2026`
- Description: `Dinner together at home`

Details:

- Date: `Oct 10, 2026`
- Time: `20:00`
- Location: `Flat 4B`
- Created by: `AL`
- Created: `Oct 8, 2026`

Attendees:

- AL
- MK
- SJ

Folders:

- `screens/item_detail_event_light_mode/`
- `screens/item_detail_event_dark_mode/`

## 5. Shared layout and component notes

These mockups use Material 3 structure and Orbit conventions as visual guidance.

Key reviewed patterns include:

- Mobile Light + Dark variants for every delivered screen
- Mobile portrait layouts following the `DESIGN.md` reference; Stitch preview/capture sizes may differ from the design-system reference viewport
- Orbit spacing scale: 4, 8, 16, 24, 32
- 16dp card/input radius
- 8dp visible checkbox radius
- Minimum 48×48dp interactive targets where applicable
- Flat tonal layering; no gradients, glow or blur
- Nested detail/list screens without bottom navigation
- Bottom navigation order on Space Home: Home, Calendar, Expenses, Assistant, Settings
- Detail layouts follow the reviewed screen-specific pattern: Event Detail uses horizontal icon + label / right-aligned value rows, while Task, Chore, Bill and List use their stacked label/value treatment

### Colors

Production colors are defined by the application theme and must be read from `Color.kt` / `OrbitTheme`.

The approved `DESIGN.md` describes the intended mockup roles, but Stitch may inject generated/derived tokens into exported HTML configuration. Those generated values are export artifacts and must not be promoted into production tokens.

## 6. Interaction / prototype scope

These files are primarily **visual layout references**, not a production prototype.

Do not infer production requirements for:

- Routing/navigation architecture
- Backend/data behavior
- Persistence
- Validation architecture
- Production state management
- Final animations

Some exported HTML files contain limited preview interactions such as:

- Password visibility
- Checklist state changes
- Progress updates

These behaviors exist only to support the visual mockup/reference.

The artboards were not wired together into a clickable Stitch navigation prototype because that was not required for this Sprint 1 layout handoff.

## 7. Export cleanup and known limitations

The raw Stitch ZIP was not used unchanged. Stitch retained historical generation variants, so the raw export included more screens than the reviewed Sprint 1 set.

Cleanup performed for this package:

- Removed historical/duplicate screen variants
- Retained only the final Light/Dark pair for each Sprint 1 screen type
- Removed unrelated / obsolete generated design documents
- Replaced the Stitch-rewritten design guide with the approved `DESIGN.md` aligned to the codebase
- Normalized HTML page titles where needed
- Retained the corrected Item List and List Detail variants
- Used the final Stitch screenshots supplied and manually checked for this handoff as the packaged `screen.png` references
- Kept the exported HTML as a secondary implementation/reference aid only

Known export limitations:

- Generated Tailwind configuration may contain tokens that do not exist in the production Orbit theme
- Some HTML contains unused CSS from earlier Stitch generations
- PNG capture pixel dimensions can vary because of Stitch/export capture scaling
- Small outer capture borders/canvas remnants may be visible around some screenshots; they are outside the UI and are not part of the layout
- Some detail screens are vertically scrollable, so the PNG shows the visible viewport rather than a full-scroll composite
- SVG/icon rendering can differ slightly between captures while preserving the intended semantic action

None of these export artifacts should be copied into production code.

## 8. QA and repository follow-up

The final Sprint 1 set was reviewed screen-by-screen using both visual references and exported HTML.

The review covered:

- Correct Light/Dark pairing
- Screen content and ordering
- Main layout/composition
- Scrollable content/overflow behavior where needed; viewport PNGs are not expected to show the full scroll height
- Major Orbit theme-role regressions introduced by Stitch
- Checkbox visual states
- Completed-item strikethrough treatment
- Item List filter states
- Detail layouts checked against each screen's intended pattern (horizontal rows in Event Detail; stacked label/value treatment in Task, Chore, Bill and List)
- FAB / primary action placement
- Bottom navigation placement/order where applicable
- Removal of duplicate/obsolete exports

**Result: 20 / 20 Sprint 1 final mockups retained.**

This QA is a mockup/layout review, not a replacement for production accessibility/testing. Production contrast, behavior and component implementation must continue to come from the application theme/design system and normal app QA.

### Original issue follow-up

The original issue also lists screens after Sprint 1. Those screens are intentionally not included in this handoff because the maintainer confirmed that this delivery covers Sprint 1 and that the remaining screens will be tracked in **a new issue**.

The original issue also asks for mockup links/screenshots to be linked from any implementation issues waiting on design and for the `needs design` label to be removed where applicable. That repository bookkeeping is outside the ZIP itself and should be handled as part of PR/issue review for the relevant linked issues.
