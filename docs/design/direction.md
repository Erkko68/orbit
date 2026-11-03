# Visual direction

Status: applied by #242. Where this page and [ADR 0007](../adr/0007-one-item.md) differ, the ADR wins. The [prototype](https://claude.ai/artifact/1XjocjcaXPPbmwpz3Zxoma) shows every screen below in light and dark, and you can tap through it.

**In one line:** the space's colour is the page, soft panels sit on it inside a screen gutter, and type is big and bold.

## References

Screens from each app's App Store listing, viewed on 2026-11-02.

| App | What to take from it |
|---|---|
| [Flighty](https://apps.apple.com/us/app/flighty-live-flight-tracker/id1358823008) | Big numbers as the voice of the app. A row of people that filters the list |
| [Things 3](https://apps.apple.com/us/app/things-3/id904237743) | Quiet rows, not a card per entry. Calendar and to-dos together in Today |
| [Structured](https://apps.apple.com/us/app/structured-daily-planner-todo/id1499198946) | The day as one thing to read, with a check on every row |
| [Copilot](https://apps.apple.com/us/app/copilot-track-budget-money/id1447330651) | "You just approve": suggestions you confirm in a batch. The model for the review board |
| [Tricount](https://apps.apple.com/us/app/tricount-split-settle-bills/id349866256) | A balance as a sentence: person, then amount |
| [Splitwise](https://apps.apple.com/us/app/splitwise/id458023433) | Amount first in the expense form |
| [Co-Star](https://apps.apple.com/us/app/co-star-personalized-astrology/id1264782561), [CHANI](https://apps.apple.com/us/app/chani-your-astrology-guide/id1532791252) | Space without neon or stars: a space theme can be light and grown-up |
| [Tody](https://apps.apple.com/us/app/tody/id595339588), [Sweepy](https://apps.apple.com/us/app/sweepy-home-cleaning-schedule/id1498897320) | What a recurring chore needs to say: how it stands and whose it is |
| [OurHome](https://apps.apple.com/us/app/ourhome-by-elusios/id6753957205) | What to avoid: the same app with a default look, one bar per screen |

Lists and chores also follow Material 3 Expressive: the [launch post](https://m3.material.io/blog/building-with-m3-expressive) with its seven tactics, the [research](https://design.google/library/expressive-material-design-google-research) behind it, and the promos [Introducing: Material 3 Expressive](https://www.youtube.com/watch?v=n17dnMChX14) and the [September 2025 Pixel Drop](https://www.youtube.com/watch?v=yPG0LCQF87s).

## The direction

**Colour.** A space's accent tints the whole page and a soft planet disc behind the header, so each space feels like a different place. Panels stay neutral, so text does not depend on the accent. Six accents: the four we have, softened, plus blue and pink, in the same order so a stored `accent` keeps its hue. Ink is navy (`#0E1230`), the primary action is ink on light and paper on dark. Violet belongs to the assistant: its screens take over the page in violet.

**Type.** Big and bold. [Jost](https://fonts.google.com/specimen/Jost) for everything, a Futura descendant, the family [used on the Apollo 11 plaque](https://www.typeroom.eu/articlehow-apollo-11-launched-futura-moon). Martian Mono for amounts, times and dates. Headlines at 42, section heads at 19, rows at 18. Both are open licence and render Catalan and Spanish correctly. Space Mono was rejected: its `€` is undersized.

**Depth.** Soft panels, no outlines. A panel is white on the tint in light, one step up in dark, with a shadow you only just see. Panels keep a gutter to the screen edges on every screen (edge-to-edge panels looked wrong on a real device) and stack with 8dp gaps.

**Shape.** Panels 24, controls 14, avatars and the add button full circles. Lists and chores mix bigger shapes: see below.

**Space.** A little: the planet behind the header, the Orbit mark in the space's colour, and a chore's rotation drawn as members around a disc. Words stay plain: the glossary still says space and member.

**Motion.** A ring closes when something is done. `OrbitMotion` springs as today.

### Rules this adds

- **One add.** A floating button in the space's colour, in the same place everywhere. It opens the item screen on a new item. On Expenses it goes straight to the form.
- **One row of controls.** A destination has one row of filters on the page and no more. No section header repeats it.
- **Dashed means proposed.** What the assistant suggests has a dashed ring until someone confirms it.
- **One check.** A circle that morphs into a cookie when done, the same in Calendar, Tasks and lists.
- **Accents are decorative on the light canvas** (under 3:1 there). A selected tab or a done row also changes ink or shows a tick, so colour never carries the meaning alone.

## Layout

```
Spaces (from the title)
  └── New space  create one or join with an invite
Last space opened
  ├── Calendar  headline, agenda, what you owe, assistant (members + settings in the top bar)
  ├── Tasks     filters, lists, chores, bills, events, tasks
  └── Expenses  what you owe, who owes what, ledger

Floating add → a new item, or an expense on Expenses
Over the bar: item, list, expense form, interview, review board
```

| Screen | How it works |
|---|---|
| Opening | The last space, not the list. The list is one tap away on the title |
| Spaces | Each row says what matters: "2 for you today · you owe €18.40" |
| New space | A page of its own: the mark, a big title, then a panel to name a space and pick its colour, which the page takes on as a preview, and a panel to join with a code or link. Either way lands in the space. The guided setup comes later |
| Calendar | Headline counts what is yours today. Overdue, Today and This week are panels. The members orbit in a cluster in the top bar, and any of them opens the settings (#259) |
| Empty space | The assistant is the first thing, "Add something yourself" is one tap away |
| Tasks | Filters on the page. Your turn on top, then lists as tiles and every task as a card with a big title |
| List | An item with a checklist: a colour block with the name and a wavy progress line. One list with what is left first and a row at the top that adds. Ticking the last entry finishes it |
| Item | A colour block with the title, a small summary and "Mark as done". Under it, rows for the date and the reminder, then the details filling the screen ([ADR 0008](../adr/0008-summary-details-and-checklists.md)) |
| Add | Type a title, done. Dates and people can wait |
| Expenses | The amount is the headline, with Settle up beside it. Balances as sentences, then the ledger |
| Expense form | Amount first. Everyone in one line, with their share under their name. Tap to leave someone out |
| Settings | A sheet: invite, manage the space (members grouped by role, then leave or delete), name and colour. A member opens their page in the same sheet (#262) |
| Member | A step of the settings sheet whose top edge turns their colour, the way a screen's colour header grows from the top: their face, name and role, then what they have and did lately, which fill in under it. The owner changes the role from there: "Make admin", "Make member", and "Make owner" for an admin, after a dialog (#261, #260) |
| Interview | One question per screen in violet, and "Do it by hand instead" is always there |
| Review board | Proposals grouped by kind, all kept by default. Tap to leave one out, pencil to edit, one button to add. "Nothing is saved until you confirm" |

## Lists and chores

This is the part of the app that gets touched every day, so it is where the look is loudest. The rest of the app stays calm around it. Each choice comes from a tactic in the Expressive post.

| Tactic | What Orbit does |
|---|---|
| Hero moment | "Your turn" is the one card that breaks the pattern: inverse colour, a burst, big type and one big Done button. Done becomes Undo and says who is next. The post asks for one or two of these in a product, so nothing else gets this treatment |
| Rich colour | Lists are solid fills, not white cards: the space's vivid colour and a partner colour next to it. Text on them is always navy, at least 4.5:1 |
| Containment | Tiles that touch have tight corners (10) where they meet and big ones (32) outside, so a pair reads as one thing. Rows do the same: 3dp gaps, 8 inside, 28 at the ends of a group |
| Variety of shapes | A list's icon sits on a cookie or a clover. Each member has a shape as well as a colour, so people can be told apart without relying on colour |
| Motion | A pressed row or tile rounds off. The check goes circle, squircle while pressed, cookie when done. Springs, not fades |
| Typography | Section heads at 24, a list's name at 46 in its own screen |

The list keeps a plain vertical list on purpose. The research behind Expressive found that replacing a familiar list with something more unusual hurt usability, so the personality goes around the rows and not into how they work.

## Tried and dropped

- **Printed, stamped look** (ink outlines, hard shadows, orange): strong but cartoonish.
- **Flat rows with hairlines only:** integrated but too flat, and it looked like any clean web app.
- **A curved horizon** between header and list, and thin orbit lines in the header: dropped, the disc does the job.
- **Deep Field** (always dark, glow) and **Star Chart** (one blue ink, serif): both narrower than what the app needs.
