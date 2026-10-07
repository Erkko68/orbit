# Project automation

The GitHub project board, its roadmap and the burn charts are generated from the issues. Nobody moves cards or types
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

## The two workflows

| Workflow | Script | Writes to | Runs on |
|---|---|---|---|
| `project-sync.yml` | `.github/scripts/project_sync.py` | Project fields: Status, `Start date`, `Target date`, `Deviation (days)` | Issue and PR events, every hour, by hand |
| `burn-charts.yml` | `.github/scripts/burn_charts.py` | The pinned "Burn charts" issue | Issue events, every night, by hand |

Both are plain Python with no dependencies, and both take `--print` to show what they would write without writing it:

```
GITHUB_REPOSITORY=Erkko68/orbit python3 .github/scripts/project_sync.py --print
python3 .github/scripts/burn_charts.py --print
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

## Burn charts

One section per sprint and one for the whole project, in the pinned "Burn charts" issue:

- Burn-up (scope and done) and burn-down (remaining and the ideal line to the due date), counted in issues.
- A time line: planned hours, hours done, and how far ahead or behind the sprint is against its due date.
- A table per finished issue: estimate, the time from first assignment to the PR being opened, and the deviation.

## Setup

- **`PROJECT_TOKEN` secret.** A classic personal access token of the project owner with only the `project` scope.
  The default `GITHUB_TOKEN` cannot read or write a project that belongs to a user account. If it expires, the
  sync fails and the burn charts show "No estimates".
- **Project fields.** `Status` (with In progress and Planned), `Priority`, `Size`, `Estimate`, `Start date`,
  `Target date`, `Deviation (days)`. The scripts look them up by name, so renaming one breaks them.
- **Roadmap view.** Layout Roadmap, date fields `Start date` and `Target date`, markers Milestones. Add the
  `Deviation (days)` column to see it next to the bars.

## Limits to know about

- Changing `Estimate`, `Size` or `Priority` on the board, or a "Blocked by" link, triggers nothing. The hourly run
  picks it up, or run the workflow by hand from the Actions tab.
- An issue that is unassigned and assigned again keeps the day of its first assignment as its start.
