# Project automation

The GitHub project board, its roadmap, the burn charts and the dashboard are generated from the issues. Nobody moves cards or types
dates. This page explains what is computed, from what, and how to change the rules.

## What people maintain

Everything else is derived from these, so they are the only things worth keeping right.

| On each issue | Where | Used for |
|---|---|---|
| Milestone | Issue | Which sprint the issue belongs to, and the earliest day it can start |
| "Blocked by" links | Issue, Relationships | The order of the plan |
| `Estimate` (hours) | Project field | How long its bar is, and what its deviation is measured against |
| `size:` label or `Size` | Issue label or project field | Stands in for the estimate when there is none |
| `Priority` | Project field | Who goes first when two issues are ready at the same time |
| Assignee | Issue | Marks the day the work started |
| `Closes #<issue>` in the PR | PR description | Marks the day the work ended |
| `Actual` (hours) | Project field | The hours really spent, for the actual cost. **Required**: the `Actual hours` check fails the PR without it |

## The workflows

| Workflow | Script | Writes to | Runs on |
|---|---|---|---|
| `project-sync.yml` | `.github/scripts/project_sync.py` | Project fields: Status, `Start date`, `Target date`, `Deviation (days)` | Issue and PR events, every hour, by hand |
| `burn-charts.yml` | `.github/scripts/burn_charts.py` | The pinned "Burn charts" issue | Issue events, every night, by hand |
| `dashboard.yml` | `.github/scripts/dashboard.py` | The dashboard on GitHub Pages, <https://erkko68.github.io/orbit/> | Issue events, pushes to the scripts or the page, every hour, by hand |
| `pr-conventions.yml` (`Actual hours` job) | `.github/scripts/actual_hours.py` | Fails the PR while an issue it closes has no `Actual`, with a comment on the PR saying how to log it (deleted once it is) | PR opened, edited, pushed to |

All are plain Python with no dependencies. The first three take `--print` to show what they would write without
writing it:

```
GITHUB_REPOSITORY=Erkko68/orbit python3 .github/scripts/project_sync.py --print
python3 .github/scripts/burn_charts.py --print
GITHUB_REPOSITORY=Erkko68/orbit python3 .github/scripts/dashboard.py --print
```

To see the dashboard locally, build it and serve the folder:

```
GITHUB_REPOSITORY=Erkko68/orbit python3 .github/scripts/dashboard.py && python3 -m http.server -d site
```

Each script checks its own logic before it does anything (`check()` at the bottom of the file).

## Project sync: the life of an issue

Every run reads the whole board, works out what each issue's fields should be, and writes only the ones that differ.
The event that started the run carries no information, so a missed or cancelled run loses nothing: the next one
catches up.

| State of the issue | Status | `Start date` | `Target date` | `Deviation (days)` |
|---|---|---|---|---|
| Open, nobody assigned | Untouched (back to Planned if it was In progress) | Planned start | Planned end | |
| Open, assigned | In progress | Day of the first assignment | Start plus its duration | |
| A PR closes it | Untouched (GitHub's own project workflows handle In progress and Done) | Day of the first assignment | Day the PR was opened | Real end minus expected end |

A negative deviation means the issue ended early, a positive one late. The Roadmap view draws one bar per issue from
`Start date` to `Target date`, so a bar is the plan until someone takes the issue, the expectation while they work on
it, and what really happened once the PR is open.

## How the plan is computed

1. **Duration.** `Estimate` hours divided by 2 hours a day, rounded up to whole days. 8 h is four days, 3 h is two.
   Without an estimate the size is used: XS 1 h, S 3 h, M 6 h, L 12 h, XL 20 h. With neither, the issue counts as an S.
2. **Earliest start.** The day after its last blocker ends, never before its sprint starts and never in the past.
   A sprint starts the day the previous one is due. Blockers that are closed or have a PR open do not hold anything
   back. Blockers in progress hold until their expected end, or until today if that has passed.
3. **Capacity.** The team has four members, so at most four issues run at once. An issue that is ready takes the
   member who became free most recently; if nobody is free, it waits for the first one. Issues in progress keep
   their member busy until their expected end.
4. **Order.** Issues are placed sprint by sprint, higher priority first (P0, P1, P2, none), then older issue first.
   A blocker is always placed before what it blocks, whatever its priority.

The plan is rebuilt on every run, so a late issue pushes everything that depends on it, and an early one pulls it in.
A sprint whose last bar ends after its milestone marker does not fit as planned.

### Changing the rules

The constants are at the top of `project_sync.py`.

| Constant | Meaning | Now |
|---|---|---|
| `HOURS_PER_DAY` | Hours one member puts in per day, weekends included | 2 |
| `TEAM` | Members working in parallel | 4 |
| `SIZE_HOURS` | Hours each size stands for | XS 1, S 3, M 6, L 12, XL 20 |
| `PROJECT` | Number of the project in the owner's account | 3 |

### What the plan does not know

- Members are interchangeable. It does not plan per person, so it cannot tell that one member is overloaded.
- Every day is a working day of two hours. It knows nothing about weekends, holidays or exams.
- Whole days only. A one-hour issue takes a member's full day.
- Issues without a milestone are not planned.
- An issue with neither estimate nor size is a guess (an S).

## Hours spent

The actual cost and the time tables need the hours each issue really took. They come from, in order:

1. **`Actual`**, the hours logged on the project. Required before review (`Actual hours` check), and the only source
   that is not a guess. `python3 .github/scripts/actual_hours.py --set <issue> <hours>` logs it from the terminal.
2. **Measured**: the time from the first assignment to the closing PR being opened, at most 2 h a day.
3. **The issue's work hours** (`Estimate`, else its size) when the measure cannot be believed. These issues are
   flagged on the dashboard under "Issues to check":

| Flag | When | What it means |
|---|---|---|
| Left running | Measured 6 h or more, and at least twice the estimate (or no estimate) | Assigned days before the work started, so the clock ran idle |
| Assigned at PR | A closed issue measured under 1 h | Assigned when the PR opened, so the clock never ran |
| No measure | No assignment or no closing PR | Nothing to measure from |

Issues not started count 0 h; work in progress counts its measure up to today.

## Burn charts

One section per sprint and one for the whole project, in the pinned "Burn charts" issue, with a link to the
dashboard:

- Burn-up (scope and done) and burn-down (remaining and the ideal line to the due date), in hours of work: each
  issue's `Estimate`, else its size.
- A time line: planned hours, hours done, and how far ahead or behind the sprint is against its due date.
- A table per finished issue: estimate, actual hours and where they come from (see "Hours spent"), and the
  deviation. Flagged rows are left out of the total.

## Dashboard

A static page on GitHub Pages (`.github/dashboard/index.html`) that draws the `data.json` built by `dashboard.py`.
The ☰ menu switches between views, and the scope buttons between the whole project and one sprint:

| View | Shows |
|---|---|
| Overview | The earned value indicators, and the PV, EV and AC curves against the budget |
| Burn charts | Burn-up and burn-down in hours |
| Roadmap | The board as it stood on any day, picked from the calendar: real bars for the work that had started, the plan of that day for the rest. A sprint zooms to its own issues |
| Cost | Planned against actual cost, by resource |
| Time tracking | Hours by member (planned and unplanned issues), how many hours are logged, estimate against actual, and the flagged issues |

The roadmap replays `project_sync.py`'s own plan for each past day, from the issues as they stood that day
(created, assigned, PR opened, closed). Estimates, milestones and "Blocked by" links are today's.

### Earned value

Measured against the Sprint 0 cost plan, whose numbers are constants at the top of `dashboard.py`:

| Constant | Meaning | Now |
|---|---|---|
| `RATE` | € per hour of team effort | 15.69 |
| `DEPRECIATION` | € per month of equipment (computers and test devices) | 138.20 |
| `BAC` | Budget at completion: the Sprint 0 total | 9,164.28 |
| `PLAN` | Hours and months of equipment per sprint | 160 h and 1 month per development sprint, 60 h and half a month for the final one |
| `PLANNED_CASH` | Cash the plan expects, at the start of the sprint that needs it | Apple and Play accounts in Sprint 1, cloud AI and the Firebase reserve in Sprint 2, the domain in Sprint 3 |
| `SPENT` | Cash really spent, as (day, item, €) | Empty. **Add a row when the team pays for something** |

- **PV** (planned value): each sprint's labour and depreciation, spread evenly from its start to its due date,
  plus its planned cash from its first day.
- **EV** (earned value): per sprint, the `Estimate` of its closed issues over its planned hours, times its budget.
  Issues without an `Estimate` earn nothing: they are unplanned work, so estimate an issue at planning for it to
  count.
- **AC** (actual cost): hours spent × `RATE`, plus depreciation so far, plus `SPENT`.
- **CPI** EV / AC, **SPI** EV / PV, **CV** EV − AC, **SV** EV − PV, **EAC** BAC / CPI, **ETC** EAC − AC,
  **VAC** BAC − EAC. The dashboard also shows AC + (BAC − EV), the EAC if the overrun so far does not repeat.

## Setup

- **`PROJECT_TOKEN` secret.** A classic personal access token of the project owner with only the `project` scope.
  The default `GITHUB_TOKEN` cannot read or write a project that belongs to a user account. If it expires, the
  sync and the dashboard fail, the `Actual hours` check fails every PR, and the burn charts show "No estimates".
- **Project fields.** `Status` (with In progress and Planned), `Priority`, `Size`, `Estimate`, `Actual`,
  `Start date`, `Target date`, `Deviation (days)`. The scripts look them up by name, so renaming one breaks them.
- **GitHub Pages.** Settings → Pages → Source: GitHub Actions. Only `main` can deploy.
- **Required check.** Add `Actual hours` to the `main` ruleset's required checks, so a PR cannot merge without it.
- **Roadmap view.** Layout Roadmap, date fields `Start date` and `Target date`, markers Milestones. Add the
  `Deviation (days)` column to see it next to the bars.

## Limits to know about

- Changing `Estimate`, `Size`, `Priority` or `Actual` on the board, or a "Blocked by" link, triggers nothing. The
  hourly runs pick it up, or run the workflow by hand from the Actions tab. After logging `Actual`, re-run the
  `Actual hours` check on the PR (or push, or edit its description).
- The dashboard's past roadmaps use today's estimates, milestones and blockers, so an issue moved to another sprint
  shows in its new sprint on every day.
- An issue that is unassigned and assigned again keeps the day of its first assignment as its start.
