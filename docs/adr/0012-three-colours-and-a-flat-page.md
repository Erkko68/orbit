# ADR 0012: Three colours on a flat page, and a space is told apart by its people

- Status: Accepted
- Date: 2026-11-11
- Issue: #293

## Context

The redesign (#241, #242, `docs/design/direction.md`) gave every space its own colour: six accents, each with eight hand-tuned variants (fill, vivid, partner, page, planet, halo, selected, done), a tinted page and a planet disc behind the header, plus a violet for the assistant and teal and coral for balances. A space picked its colour when it was created and in its settings.

The professor's review of the app (2026-11-10) asked for a tight palette of at most three base colours, used through their shades and tones; a solid, flat background with no gradients; depth from those tones, so what matters reads as in front; no setting for a space's colour; and a clearer way to tell spaces apart than their name or colour. On a flat page, a tonal button such as Reopen came out the same colour as the page.

## Decision

**Three key colours, each a tonal palette: a cool neutral, the brand blue and a warm coral. The page is one flat neutral. A space has no colour of its own: it is told apart by the people in it.**

- `Color.kt` holds the tones, named by their CIELAB L* (`Neutral92`, `Blue80`, `Coral80`). Two tones 50 apart are at least 4.5:1, so a tone says whether text can sit on a fill.
- Depth is the neutral's tones: the page (`surface`), a panel on it (`surfaceBright`), a field or chip in the panel (`surfaceContainer`). A tonal button is a blue container (`secondaryContainer`), so it reads as something to tap on the page and on a panel. `ComponentContrastTest` checks these steps, not only text.
- What the accent paints (`OrbitTheme.accent`: the add button, colour headers, vivid fills, the selected tab, a done row) is the same in every space. The assistant is blue, not violet. The coral is the turn card's partner and the warning colour.
- Outside the three: error, the system's own red, and the balance pair, teal `credit` to get back and coral `debit` owed, as in every money app. A balance also says it with its sign and its words.
- A space tile is a neutral panel led by the faces of the people the user shares it with, as a group chat without a photo is. A space with only the user shows its initials on a shape. `Space.accent` stays, now the index of that badge's shape and fill, taken automatically: a new space takes the one after the user's other spaces. Neither creating a space nor its settings asks for it.

## Alternatives considered

- **Keep a colour per space on its tile and header only.** Fewer colours on screen than a tinted page, but still six hues across the app, and still a setting the review called unnecessary.
- **Generate the palettes at runtime** with Google's colour utilities (MaterialKolor). Exact HCT tones, but a new dependency for values that never change; the tones are computed once and written in `Color.kt`.
- **Number the spaces, or let the user pick a badge.** A number means nothing and changes with the order; a picked badge is the colour setting again under another name.
- **A space kind (home, trip, team) as an icon.** Meaningful, but it needs a new field and a choice. It can come with space templates (#105).
- **Balances in blue and coral, inside the three.** Blue for money back reads less clearly than the green-teal every money app uses, for the one figure people read at a glance.

## Consequences

- Fewer colours to keep accessible: one accent instead of six, and the contrast test now covers surfaces as well as text.
- `OrbitSpaceColor`, `OrbitSpaceColorPicker` and `OrbitColorPicker` are gone, and so is `accent` in `updateSpace`, now `renameSpace`. Screens no longer keep the space's colour in their state.
- Spaces look alike until someone joins them; then their people tell them apart. A space kind (#105) or a cover photo can add to that later.
- Supersedes the colour section of `docs/design/direction.md` (the accent as the page, the planet, the violet assistant).
