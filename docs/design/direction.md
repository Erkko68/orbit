# Visual direction

Status: applied by #242, simplified by #241 ([ADR 0010](../adr/0010-standard-navigation-and-one-item-form.md)); colour replaced by #293 ([ADR 0012](../adr/0012-three-colours-and-a-flat-page.md)). Where this page and an ADR differ, the ADR wins. The [prototype](https://claude.ai/artifact/1XjocjcaXPPbmwpz3Zxoma) shows every screen below in light and dark, and you can tap through it.

**In one line:** three colours on a flat page, soft panels on it inside a screen gutter, and type is big and bold.

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

**Colour.** Three key colours, each used as tones: a cool neutral for the page, panels and text, the brand blue for the accent (the add button, colour headers, the selected tab, the assistant) and a warm coral beside it (the turn card's shape, warnings). The page is one flat neutral. A space has no colour of its own: its people tell it apart. Outside the three there is only error and the balance pair, teal to get back and coral owed. Ink is navy (`#0E1230`), the primary action is ink on light and paper on dark ([ADR 0012](../adr/0012-three-colours-and-a-flat-page.md)).

**Type.** Big and bold. [Jost](https://fonts.google.com/specimen/Jost) for everything, a Futura descendant, the family [used on the Apollo 11 plaque](https://www.typeroom.eu/articlehow-apollo-11-launched-futura-moon). Martian Mono for amounts, times and dates. Headlines at 42, section heads at 19, rows at 18. Both are open licence and render Catalan and Spanish correctly. Space Mono was rejected: its `€` is undersized.

**Depth.** Tones of the neutral, no outlines. A panel is white on the grey page in light, one step up in dark, with a shadow you only just see; a field in it is one step deeper, and a tonal button is a blue container. Panels keep a gutter to the screen edges on every screen (edge-to-edge panels looked wrong on a real device) and stack with 8dp gaps.

**Shape.** Panels 24, controls 14, avatars and the add button full circles. Lists and chores mix bigger shapes: see below.

**Space.** A little: the Orbit mark, and a chore's rotation drawn as members around a disc. Words stay plain: the glossary still says space and member.

**Motion.** A ring closes when something is done. `OrbitMotion` springs as today.

### Rules this adds

- **One add.** A floating button in the accent, in the same place everywhere. It opens the item screen on a new item. On Expenses it goes straight to the form.
- **One row of controls.** A destination has one row of filters on the page and no more. No section header repeats it.
- **Dashed means proposed.** What the assistant suggests has a dashed ring until someone confirms it.
- **One check.** A circle that morphs into a cookie when done, the same in Calendar, Tasks and lists.
- **Fills are decorative on the page** (under 3:1 there). A selected tab or a done row also changes ink or shows a tick, and a balance has its sign and words, so colour never carries the meaning alone.

### Rules from #241

The look stays; these keep the structure plain, the way phone apps people already know work.

- **Standard navigation.** A place the user goes is a screen on the stack, with a back arrow, and system back and swipe-back do the same. Sheets are only for a short task (who, when, the reminder, an invitation, a role) and dialogs for what cannot be undone.
- **Edit in the body.** A colour header shows. The one exception is the title of an item or an expense, typed in place with a pencil beside it. Everything else that changes is a labelled row or field in the body.
- **One text per idea.** An item has a title and a description.
- **Lists are lists.** Things of the same kind are rows in one vertical list. No tiles, carousels or sideways scrollers for content.
- **One layout per screen.** A screen does not change shape with what it holds. Sections keep their order, and an empty one shows its add row.
- **Still, not animated.** Motion only for feedback: a press, a check, a page change. Faces and shapes never drift on their own.

## Layout

```
Spaces (from the title)
  └── New space  create one or join with an invite
Last space opened
  ├── Calendar  headline, then the month or the list by day (members + settings in the top bar)
  ├── Tasks     filters, lists, chores, bills, events, tasks
  └── Expenses  what you owe, who owes what, ledger

Floating add → a menu: a new task or the assistant; an expense on Expenses
Over the bar: item, list, expense form, interview, review board
```

| Screen | How it works |
|---|---|
| Opening | The last space, not the list. The list is one tap away on the title |
| Spaces | A panel per space led by the faces of the people in it, or its initials on a shape while it is only yours: the name, what matters ("2 for you today") |
| New space | A page of its own: the mark, a big title, then a panel to name a space and a panel to join with a code or link. Either way lands in the space. The guided setup comes later |
| Calendar | Headline counts what is yours today. Two chips switch between the month and the list. The month is a panel like the date picker: days with open items are filled, today has a ring, and the day picked lists its items under it. The list goes Overdue, then each day to the end of the week. The members' faces and a settings button in the top bar open the settings |
| Add menu | On Calendar and Tasks the add button turns into a close button with two pills above it: New task (by hand) and Plan with the assistant. A tap outside closes it. On Expenses it adds an expense straight away |
| Empty space | The assistant is the first thing, "Add something yourself" is one tap away |
| Tasks | Filters in one row. Your turn on top, then To do and Done (folded to its count) as one list: every item is the same row, the check, the title, a line of its description, when and how far along, and the faces at the end. The check of a list opens it |
| Item | The same form for every item: a colour block with the title typed in place (the pencil says so), then Who and When rows, and Reminder once it has a date, the description as tall as its text, the checklist with its add row last, and the history: "Ana marked it done", "You reopened it". One button at the bottom: Create, or Mark done on an item without entries; ticking the last entry finishes a list. On scroll the title shrinks away and the row with the back arrow stays; it comes back at the top |
| Add | Type a title and Create. Who, when and the checklist can be filled first or later; nothing is written before Create |
| Expenses | Balances first, as sentences with each face: the whole row vivid teal for who gets money back and coral for who owes it, with a + or − before the amount. Then Settle up, only the payments you are in: "You owe Ana" with Pay, and "Bea owes you" with nothing to press, since who owes says when they paid, then the ledger, where a reimbursement reads "Bea paid Ana" with both faces on a tinted row |
| Expense form | The item's colour header, with the title typed in place. Amount first, then Paid by and Date as rows with a sheet. Split between says how much is assigned; everyone is a row with their share at the end, typed there for custom amounts: tap to leave someone out. An automatic share is its amount greyed in an empty field; typing fixes it, and the undo icon inside a fixed one makes it automatic again |
| Settings | A screen like the account: Edit name (a sheet) and Invite people (a sheet) for an owner or admin, then the members with their roles, then Leave or Delete space after a dialog, each group as rows |
| Member | A screen with a colour header: their face, name and role, then what they have and did lately. For the owner a Role row opens a sheet: Member or Admin, and "Make owner" for an admin, after a dialog |
| Interview | One question per screen, and "Do it by hand instead" is always there |
| Review board | Proposals grouped by kind, all kept by default. Tap to leave one out, pencil to edit, one button to add. "Nothing is saved until you confirm" |

## Lists and chores

This is the part of the app that gets touched every day, so it is where the look is loudest. The rest of the app stays calm around it. Each choice comes from a tactic in the Expressive post.

| Tactic | What Orbit does |
|---|---|
| Hero moment | "Your turn" is the one card that breaks the pattern: inverse colour, a burst in the partner colour, big type and one big Done button in the vivid one. Once done it moves on to the next turn. The post asks for one or two of these in a product, so nothing else gets this treatment |
| Rich colour | The item's and the expense's header are solid fills in the accent's vivid tone. Text on them is always navy, at least 4.5:1 |
| Containment | Rows of a group have 3dp gaps, tight corners (8) where they meet and big ones (28) at the ends. A ticked entry stays a tinted panel |
| Variety of shapes | A row's icon sits on a cookie, a clover or a sunny. Each member has a shape as well as a colour, so people can be told apart without relying on colour |
| Motion | A pressed row rounds off. The check goes circle, squircle while pressed, cookie when done. Springs, not fades |
| Typography | Section heads at 24, an item's name big in its header |

The list keeps a plain vertical list on purpose. The research behind Expressive found that replacing a familiar list with something more unusual hurt usability, so the personality goes around the rows and not into how they work.

## Tried and dropped

- **Printed, stamped look** (ink outlines, hard shadows, orange): strong but cartoonish.
- **Flat rows with hairlines only:** integrated but too flat, and it looked like any clean web app.
- **A curved horizon** between header and list, and thin orbit lines in the header: dropped, the disc does the job.
- **Deep Field** (always dark, glow) and **Star Chart** (one blue ink, serif): both narrower than what the app needs.
- **Faces that float with physics**, lists as staggered tiles and settings as a sheet with pages (#242 to #262): distinctive, but each taught its own rule. Replaced by #241.
