"""Builds the project dashboard for GitHub Pages: earned value against the Sprint 0 cost plan, burn charts in hours,
the roadmap as it stood on any day, and the issues whose time is a fallback.

Writes <out>/data.json next to a copy of .github/dashboard/index.html, which draws it.
Run with --print to see the indicators without writing anything. docs/process/project-automation.md explains it.
"""
import json
import shutil
import sys
from datetime import date, datetime, timedelta, timezone
from pathlib import Path

import burn_charts as bc
import project_sync as ps

# The Sprint 0 cost plan (section 7.2 of the Sprint 0 document).
RATE = 15.69  # € per hour: employer cost of a junior developer in Spain
DEPRECIATION = 85.42 + 52.78  # € per month: development computers + test devices
BAC = 9164.28  # the plan's total as stated; its own rounding puts the rows 0.02 above it
PLAN = {"Sprint 1": (160, 1), "Sprint 2": (160, 1), "Sprint 3": (160, 1), "Sprint 4 (final)": (60, 0.5)}  # hours, months
# Sprint 0 does not say when the cash goes out, so each item is planned at the start of the sprint that needs it.
PLANNED_CASH = {"Sprint 1": [("Apple Developer Program", 99), ("Google Play Console", 22)],
                "Sprint 2": [("Cloud AI API", 65), ("Firebase reserve", 10)],
                "Sprint 3": [("Domain", 12)]}
# Cash really spent, as (day, item, €). Add a row when the team pays for something.
SPENT = []
STATES = ["done", "late", "progress", "overdue", "planned"]  # roadmap bar states, stored as their index


def day(text):
    return date.fromisoformat(text[:10])


def fraction(when, window):
    """How much of a sprint has passed by `when`, from 0 to 1."""
    start, due = day(window[0]), day(window[1])
    return min(max((when - start).days / max((due - start).days, 1), 0), 1)


def planned_cash(title):
    return sum(euros for _, euros in PLANNED_CASH.get(title, []))


def budget(title):
    hours, months = PLAN[title]
    return hours * RATE + months * DEPRECIATION + planned_cash(title)


def sprint_of(when, sprints):
    """The sprint a day of spending belongs to: the first one not yet due, so early purchases land in Sprint 1."""
    return next((t for t, (_, due) in sorted(sprints.items(), key=lambda s: s[1][1]) if when < day(due)), None)


def hours_by(issue, when, today):
    """Hours an issue has cost by `when`: all of it on the day it closed, work in progress only as of today."""
    if issue["closed"] and issue["closed"] <= when or not issue["closed"] and when >= today:
        return bc.spent(issue)[0]
    return 0


def indicators(pv, ev, ac, bac=BAC):
    cpi, spi = (ev / ac if ac else None), (ev / pv if pv else None)
    eac = bac / cpi if cpi else None
    return {"PV": pv, "EV": ev, "AC": ac, "BAC": bac, "CV": ev - ac, "SV": ev - pv, "CPI": cpi, "SPI": spi,
            "EAC": eac, "ETC": eac - ac if eac else None, "VAC": bac - eac if eac else None,
            "EAC_atypical": ac + bac - ev}


def values(issues, sprints, when, today):
    """Sprint title -> PV, EV, AC as of `when`, plus what they are made of."""
    out = {}
    for title, window in sprints.items():
        if title not in PLAN:
            continue
        hours, months = PLAN[title]
        passed = fraction(when, window)
        own = [i for i in issues if (i["milestone"] or {}).get("title") == title]
        earned = sum(i["estimate"] or 0 for i in own if i["closed"] and i["closed"] <= when)
        planned_h = sum(hours_by(i, when, today) for i in own if i["estimate"])
        unplanned_h = sum(hours_by(i, when, today) for i in own if not i["estimate"])
        cash = [(d, item, euros) for d, item, euros in SPENT if day(d) <= when and sprint_of(day(d), sprints) == title]
        out[title] = {
            "PV": (hours * RATE + months * DEPRECIATION) * passed + (planned_cash(title) if when >= day(window[0]) else 0),
            "EV": min(earned / hours, 1) * budget(title),
            "AC": (planned_h + unplanned_h) * RATE + months * DEPRECIATION * passed + sum(e for *_, e in cash),
            "passed": passed, "earned_h": earned, "planned_h": planned_h, "unplanned_h": unplanned_h,
            "depreciation": months * DEPRECIATION * passed, "cash": cash}
    return out


def snapshot(item, when):
    """The board item as it stood at the end of `when`, or None if it did not exist yet."""
    # ponytail: estimates, milestones and blockers are today's. Store daily snapshots if the plan's history matters.
    issue, cut = item["content"], when.isoformat()
    if issue["createdAt"][:10] > cut:
        return None
    timeline = [e for e in issue["timelineItems"]["nodes"] if e["createdAt"][:10] <= cut]
    prs = [p for p in issue["closedByPullRequestsReferences"]["nodes"] if p["createdAt"][:10] <= cut]
    closed = issue["closedAt"] and issue["closedAt"][:10] <= cut
    return {**item, "content": {**issue, "state": "CLOSED" if closed else "OPEN", "timelineItems": {"nodes": timeline},
                                "closedByPullRequestsReferences": {"nodes": prs},
                                "assignees": {**issue["assignees"], "totalCount": len(timeline)}}}


def roadmap(items, sprints, start, today):
    """Day -> bars [issue, first day, last day, state] as the board would have drawn them that day, in day offsets."""
    out = {}
    for n in range((today - start).days + 1):
        when = start + timedelta(n)
        board = [s for s in (snapshot(i, when) for i in items) if s]
        plan = ps.schedule(board, sprints, when.isoformat())
        bars = []
        for item in board:
            dates = ps.wanted(item, when.isoformat(), plan)
            first, last = dates.get("Start date"), dates.get("Target date")
            if not first:
                continue
            begun, ended = ps.progress(item)
            if ended:
                state = "late" if dates.get("Deviation (days)", 0) > 0 else "done"
            elif begun:
                state = "overdue" if last < when.isoformat() else "progress"
                last = max(last, when.isoformat())
            else:
                state = "planned"
            bars.append([item["content"]["number"], (day(first) - start).days, (day(last) - start).days,
                         STATES.index(state)])
        out[when.isoformat()] = bars
    return out


def breakdown(title, value):
    """Planned against actual cost of a sprint, by resource."""
    hours, months = PLAN[title]
    return [{"item": "Team effort, planned issues", "planned": hours * RATE, "planned_h": hours,
             "actual": value["planned_h"] * RATE, "actual_h": value["planned_h"]},
            {"item": "Team effort, unplanned issues", "planned": 0, "planned_h": 0,
             "actual": value["unplanned_h"] * RATE, "actual_h": value["unplanned_h"]},
            {"item": "Equipment depreciation", "planned": months * DEPRECIATION, "actual": value["depreciation"]}] + \
        [{"item": item, "planned": euros, "actual": sum(e for _, i, e in value["cash"] if i == item)}
         for item, euros in PLANNED_CASH.get(title, [])] + \
        [{"item": item, "planned": 0, "actual": euros} for _, item, euros in value["cash"]
         if item not in dict(PLANNED_CASH.get(title, []))]


def build(items, sprints, today):
    values_by = {i["content"]["number"]: {k: ps.value(i, k) for k in ("estimate", "actual", "size")} for i in items}
    now = datetime.now(timezone.utc)
    items = [i for i in items if i["content"]["title"] != bc.TITLE]
    issues = [{**bc.issue(i["content"], values_by, now), "url": i["content"]["url"]} for i in items]
    sprints = {t: w for t, w in sprints.items() if t in PLAN}
    start = min(day(w[0]) for w in sprints.values())
    end = max(day(w[1]) for w in sprints.values())
    now_values = values(issues, sprints, today, today)
    total = indicators(*(sum(v[k] for v in now_values.values()) for k in ("PV", "EV", "AC")))
    curve = {"days": [], "PV": [], "EV": [], "AC": []}
    for n in range((end - start).days + 1):
        when = start + timedelta(n)
        v = values(issues, sprints, when, today).values()
        curve["days"].append(when.isoformat())
        curve["PV"].append(round(sum(x["PV"] for x in v), 2))
        for key in ("EV", "AC"):
            curve[key].append(round(sum(x[key] for x in v), 2) if when <= today else None)

    def burn(own, due):
        """Burn data to today, with the days left to the due date empty so the ideal line reaches zero."""
        days, scope, done, ideal = bc.series(own, min(today, due) if due else today, due)
        if due and days[-1] < due:
            rest = [days[-1] + timedelta(n) for n in range(1, (due - days[-1]).days + 1)]
            span = max((due - days[0]).days, 1)
            ideal += [round(scope[-1] * max(1 - (d - days[0]).days / span, 0), 1) for d in rest]
            days, scope, done = days + rest, scope + [None] * len(rest), done + [None] * len(rest)
        return {"days": [d.isoformat() for d in days], "scope": scope, "done": done, "ideal": ideal}

    sprint_out = []
    for title, (first, due) in sorted(sprints.items(), key=lambda s: s[1][1]):
        own = [i for i in issues if (i["milestone"] or {}).get("title") == title]
        v = now_values[title]
        sprint_out.append({"title": title, "start": first, "due": due, "plan_h": PLAN[title][0],
                           "budget": budget(title), "passed": v["passed"], "earned_h": v["earned_h"],
                           **indicators(v["PV"], v["EV"], v["AC"], budget(title)),
                           "cost": breakdown(title, v), "burn": burn(own, day(due)) if own else None})
    return {"generated": now.isoformat(timespec="minutes"), "today": today.isoformat(), "rate": RATE, "total": total,
            "sprints": sprint_out, "curve": curve, "burn": burn(issues, None),
            "issues": [{**{k: i[k] for k in ("number", "title", "url", "estimate", "actual", "work", "measured", "who")},
                        "sprint": (i["milestone"] or {}).get("title"), "created": i["created"].isoformat(),
                        "closed": i["closed"] and i["closed"].isoformat(), "started": i["started"],
                        "spent": bc.spent(i)[0], "source": bc.spent(i)[1]} for i in issues],
            "roadmap": {"start": start.isoformat(), "states": STATES, "days": roadmap(items, sprints, start, today)}}


def main():
    _, _, items, sprints = ps.load()
    data = build(items, sprints, date.today())
    if "--print" in sys.argv:
        print(json.dumps({"total": data["total"], "sprints": [{k: s[k] for k in ("title", "PV", "EV", "AC", "CPI", "SPI")}
                                                               for s in data["sprints"]]}, indent=1))
        return
    out = Path(sys.argv[sys.argv.index("--out") + 1] if "--out" in sys.argv else "site")
    out.mkdir(parents=True, exist_ok=True)
    (out / "data.json").write_text(json.dumps(data, separators=(",", ":")))
    shutil.copy(Path(__file__).parent.parent / "dashboard" / "index.html", out / "index.html")


def check():
    sprints = {"S1": ("2026-10-06", "2026-10-27")}
    if fraction(date(2026, 10, 21), sprints["S1"]) != 15 / 21 or sprint_of(date(2026, 10, 1), sprints) != "S1":
        raise RuntimeError("fraction() or sprint_of() self-check failed")
    PLAN["S1"], PLANNED_CASH["S1"] = (160, 1), [("Apple", 99), ("Play", 22)]
    try:
        d = date(2026, 10, 21)

        def issue(estimate, closed, measured, actual=None):
            return {"milestone": {"title": "S1"}, "estimate": estimate, "actual": actual, "closed": closed,
                    "work": bc.work(estimate, None), "measured": measured, "started": True}

        # The Sprint 1 report on 21 October: 125 h earned of 160, 246.6 h spent, no cash out.
        issues = [issue(125, d, 132.6), issue(None, d, 0.5, actual=114)]
        v = values(issues, sprints, d, d)["S1"]
        if (round(v["PV"], 2), round(v["EV"], 2), round(v["AC"], 2)) != (2012.86, 2163.75, 3967.87):
            raise RuntimeError(f"values() self-check failed: {v}")
        if round(indicators(v["PV"], v["EV"], v["AC"])["CPI"], 2) != 0.55:
            raise RuntimeError("indicators() self-check failed")
    finally:
        del PLAN["S1"], PLANNED_CASH["S1"]
    item = {"content": {"createdAt": "2026-10-06T10:00:00Z", "closedAt": "2026-10-09T10:00:00Z",
                        "timelineItems": {"nodes": [{"createdAt": "2026-10-08T10:00:00Z"}]},
                        "closedByPullRequestsReferences": {"nodes": [{"createdAt": "2026-10-09T09:00:00Z"}]},
                        "assignees": {"totalCount": 1}}}
    before, during = snapshot(item, date(2026, 10, 7))["content"], snapshot(item, date(2026, 10, 8))["content"]
    if (snapshot(item, date(2026, 10, 5)) or before["timelineItems"]["nodes"] or before["assignees"]["totalCount"]
            or during["state"] != "OPEN" or during["closedByPullRequestsReferences"]["nodes"]
            or snapshot(item, date(2026, 10, 9))["content"]["state"] != "CLOSED"):
        raise RuntimeError("snapshot() self-check failed")


if __name__ == "__main__":
    check()
    main()
