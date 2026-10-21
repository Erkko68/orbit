"""Rebuilds the burn-up and burn-down charts in the "Burn charts" issue from the repo's issues.

One pair of charts per milestone (sprint) plus one for the whole project, in hours, each with a time report:
estimated hours against the time the issues really took. The full dashboard is built by dashboard.py.
Run with --print to see the markdown without touching GitHub.
"""
import json
import math
import os
import subprocess
import sys
from datetime import date, datetime, time, timedelta, timezone

from project_sync import SIZE_HOURS

TITLE = "Burn charts"
DASHBOARD = "https://erkko68.github.io/orbit/"
PROJECT = 3  # the repo owner's "orbit" project, where the Estimate and Actual fields (hours) live
HOURS_PER_DAY = 2  # the most a day counts for, as project_sync.py plans it
GREEN, BLUE, ORANGE = "#1baf7a", "#2a78d6", "#eb6834"  # match the 🟩 🟦 🟧 legends under the charts
QUERY = """query($owner: String!, $name: String!, $endCursor: String) {
  repository(owner: $owner, name: $name) {
    issues(first: 100, after: $endCursor) {
      pageInfo { hasNextPage endCursor }
      nodes {
        number title createdAt closedAt
        milestone { title dueOn }
        labels(first: 20) { nodes { name } }
        assignees(first: 5) { nodes { login } }
        timelineItems(itemTypes: ASSIGNED_EVENT, first: 1) { nodes { ... on AssignedEvent { createdAt } } }
        closedByPullRequestsReferences(first: 10) { nodes { createdAt } }
      }
    }
  }
}"""

PROJECT_QUERY = """query($owner: String!, $number: Int!, $endCursor: String) {
  user(login: $owner) {
    projectV2(number: $number) {
      items(first: 100, after: $endCursor) {
        pageInfo { hasNextPage endCursor }
        nodes {
          content { ... on Issue { number } }
          estimate: fieldValueByName(name: "Estimate") { ... on ProjectV2ItemFieldNumberValue { number } }
          actual: fieldValueByName(name: "Actual") { ... on ProjectV2ItemFieldNumberValue { number } }
          size: fieldValueByName(name: "Size") { ... on ProjectV2ItemFieldSingleSelectValue { name } }
        }
      }
    }
  }
}"""


def gh(*args, stdin=None, env=None):
    return subprocess.run(["gh", *args], input=stdin, check=True, capture_output=True, text=True, env=env).stdout


def fields():
    """Issue number -> its Estimate, Actual and Size on the project. Empty when the token cannot read the project."""
    # GITHUB_TOKEN cannot read a user-owned project, so CI passes a PAT with the project scope as PROJECT_TOKEN.
    # Plain GraphQL, because `gh project` hides the real error (bad credentials, missing scope) behind "unknown owner type".
    token = os.environ.get("PROJECT_TOKEN")
    try:
        pages = json.loads(gh("api", "graphql", "--paginate", "--slurp", "-F", "owner={owner}", "-F", f"number={PROJECT}",
                              "-f", f"query={PROJECT_QUERY}", env={**os.environ, "GH_TOKEN": token} if token else None))
    except subprocess.CalledProcessError as error:
        print(error.stdout, error.stderr, file=sys.stderr)
        return {}
    items = [n for p in pages for n in p["data"]["user"]["projectV2"]["items"]["nodes"]]
    return {i["content"]["number"]: {name: next(iter((i[name] or {}).values()), None) for name in ("estimate", "actual", "size")}
            for i in items if i["content"]}


def worked(node, until=None):
    """Hours from the first assignment to the closing PR being opened (or `until`, for work in progress), at most
    HOURS_PER_DAY per day, or None when either end is missing. An issue left assigned for days counts as the days'
    budget, not as 24 h each."""
    assigned = [e["createdAt"] for e in node["timelineItems"]["nodes"]]
    opened = [p["createdAt"] for p in node["closedByPullRequestsReferences"]["nodes"]]
    if not assigned or not (opened or until):
        return None
    start = datetime.fromisoformat(assigned[0].replace("Z", "+00:00"))
    end = datetime.fromisoformat(min(opened).replace("Z", "+00:00")) if opened else until
    if end < start:
        return None
    total = 0
    # ponytail: days split at UTC midnight. Use the team's time zone if late-evening work starts to skew it.
    while start < end:
        midnight = datetime.combine(start.date() + timedelta(1), time(), start.tzinfo)
        total += min((min(midnight, end) - start).total_seconds() / 3600, HOURS_PER_DAY)
        start = midnight
    return round(total, 1)


def work(estimate, size):
    """Hours an issue stands for: its Estimate, else its size, else an S, as project_sync.py plans it."""
    return estimate or SIZE_HOURS.get(size) or SIZE_HOURS["S"]


def spent(issue):
    """(hours, source): the logged Actual, else the measured time when it is believable, else the issue's work hours
    and why the measure was not used. The measure is elapsed time, so it breaks both ways."""
    if issue["actual"] is not None:
        return issue["actual"], "logged"
    if not issue["closed"] and not issue["started"]:
        return 0, "not started"
    hours, estimate = issue["measured"], issue["estimate"]
    if hours is None:
        return issue["work"], "no measure"
    if hours >= 6 and (not estimate or hours >= 2 * estimate):
        return issue["work"], "left running"  # assigned days before the work started
    if hours < 1 and issue["closed"]:
        return issue["work"], "assigned at PR"  # the work happened before the assignment
    return hours, "measured"


def deviation(estimate, actual):
    return f"{actual - estimate:+.1f} h ({(actual - estimate) / estimate:+.0%})"


def time_report(issues, today, start=None, due=None, table=True):
    """Planned against done hours, how far the sprint is from its plan, and estimate against actual per issue."""
    planned = sum(i["estimate"] or 0 for i in issues)
    if not planned:
        return ""
    done = sum(i["estimate"] or 0 for i in issues if i["closed"])
    out = f"Planned {planned:g} h, done {done:g} h ({done / planned:.0%})."
    if due:
        passed = min(max((today - start).days / max((due - start).days, 1), 0), 1)
        gap = done - planned * passed
        out += (f" {passed:.0%} of the sprint has passed, so {planned * passed:.0f} h were expected by now:"
                f" {abs(gap):.0f} h {'ahead' if gap >= 0 else 'behind'}.")
    rows = [(i, *spent(i)) for i in issues if i["closed"] and i["estimate"]]
    return out + (issue_table(rows, table) if rows else "\n\n")


def issue_table(rows, with_rows=True):
    """Estimate against actual per finished issue, with a total over the issues whose actual can be trusted."""
    known = [(i, hours) for i, hours, source in rows if source in ("logged", "measured")]
    out = "\n\n| Issue | Assignee | Estimate (h) | Actual (h) | Source | Deviation |\n|---|---|---|---|---|---|\n"
    for i, hours, source in (rows if with_rows else []):
        trusted = source in ("logged", "measured")
        out += (f"| #{i['number']} | {i['who']} | {i['estimate']:g} | {f'{hours:g}' if trusted else '–'} |"
                f" {source if trusted else '⚠️ ' + source} | {deviation(i['estimate'], hours) if trusted else '–'} |\n")
    if known:
        estimate, actual = sum(i["estimate"] for i, _ in known), sum(hours for _, hours in known)
        out += f"| **Total** ({len(known)} of {len(rows)}) | | {estimate:g} | {actual:g} | | {deviation(estimate, actual)} |\n"
    return out + (f"\nActual is the `Actual` field on the project. Without it, the time from the first assignment to"
                  f" the closing PR being opened, at most {HOURS_PER_DAY} h a day. ⚠️ rows have neither a log nor a"
                  f" believable measure and are left out of the total.\n\n")


def series(issues, today, due=None):
    """Per day from the first issue to today, in work hours: (days, scope, done, ideal remaining)."""
    start = min(i["created"] for i in issues)
    days = [start + timedelta(n) for n in range((today - start).days + 1)]
    scope = [sum(i["work"] for i in issues if i["created"] <= d) for d in days]
    done = [sum(i["work"] for i in issues if i["closed"] is not None and i["closed"] <= d) for d in days]
    span = max((due - start).days, 1) if due else None
    ideal = [round(scope[-1] * max(1 - (d - start).days / span, 0), 1) for d in days] if due else None
    return days, scope, done, ideal


def xychart(title, x, top, plots):
    """A Mermaid xychart; each plot is (kind, colour, values), coloured in the order they are declared."""
    palette = ", ".join(colour for _, colour, _ in plots)
    lines = "".join(f"\n  {kind} {json.dumps([v if v % 1 else int(v) for v in (round(v, 1) for v in values)])}" for kind, _, values in plots)
    return (f'```mermaid\n%%{{init: {{"themeVariables": {{"xyChart": {{"plotColorPalette": "{palette}"}}}}}}}}%%\n'
            f'xychart-beta\n  title "{title}"\n  x-axis {x}\n  y-axis "Hours" 0 --> {top}{lines}\n```\n')


def charts(name, issues, today, due=None):
    days, scope, done, ideal = series(issues, today, due)
    remaining = [s - d for s, d in zip(scope, done)]
    x = json.dumps([d.strftime("%d/%m") for d in days])
    top = int(max(scope) * 1.1) + 1
    closed = sum(i["closed"] is not None for i in issues)
    up = xychart(f"{name}: burn-up", x, top, [("bar", GREEN, done), ("line", BLUE, scope)])
    down = xychart(f"{name}: burn-down", x, top, [("bar", BLUE, remaining)] + ([("line", ORANGE, ideal)] if ideal else []))
    return (f"## {name}\n\n{closed} of {len(issues)} issues done: {done[-1]:g} of {scope[-1]:g} h of work.\n\n"
            f"{up}🟩 done · 🟦 scope (Estimate, else size)\n\n{down}🟦 remaining" + (" · 🟧 ideal to the due date" if ideal else "") + "\n\n")


def day(stamp):
    return date.fromisoformat(stamp[:10]) if stamp else None


def issue(node, board, now):
    """One issue in the shape the charts, the time report and dashboard.py work with."""
    values = board.get(node["number"], {})
    size = values.get("size") or next((l["name"][6:] for l in node["labels"]["nodes"] if l["name"].startswith("size: ")), None)
    return {"number": node["number"], "title": node["title"], "created": day(node["createdAt"]),
            "closed": day(node["closedAt"]), "milestone": node["milestone"], "estimate": values.get("estimate"),
            "actual": values.get("actual"), "work": work(values.get("estimate"), size),
            "started": bool(node["timelineItems"]["nodes"]),
            "measured": worked(node, None if node["closedAt"] else now),
            "who": ", ".join(a["login"] for a in node["assignees"]["nodes"])}


def load():
    """(issues, board issue number, whether the project could be read)."""
    pages = json.loads(gh("api", "graphql", "--paginate", "--slurp", "-F", "owner={owner}", "-F", "name={repo}",
                          "-f", f"query={QUERY}"))
    raw = [n for p in pages for n in p["data"]["repository"]["issues"]["nodes"]]
    board = next((i["number"] for i in raw if i["title"] == TITLE), None)
    values, now = fields(), datetime.now(timezone.utc)
    return [issue(i, values, now) for i in raw if i["number"] != board], board, bool(values)


def main():
    issues, board, readable = load()
    today = date.today()
    body = (f"Updated automatically by `.github/workflows/burn-charts.yml`. Do not edit.\n\n"
            f"📊 **[Open the dashboard]({DASHBOARD})**: earned value, cost, roadmap by date and the flagged issues.\n\n")
    if not issues:
        body += "No issues yet.\n"
    elif not readable:
        body += "No estimates: the `PROJECT_TOKEN` secret is missing or cannot read the project.\n\n"
    milestones = {i["milestone"]["title"]: day(i["milestone"].get("dueOn")) for i in issues if i["milestone"]}
    previous = None
    for title in sorted(milestones):
        sprint = [i for i in issues if i["milestone"] and i["milestone"]["title"] == title]
        body += charts(title, sprint, today, milestones[title])
        # A sprint starts when the one before it ends; the first one starts with its first issue.
        body += time_report(sprint, today, previous or min(i["created"] for i in sprint), milestones[title])
        previous = milestones[title]
    if issues:
        body += charts("Whole project", issues, today) + time_report(issues, today, table=False)

    if "--print" in sys.argv:
        print(body)
    elif board:
        gh("issue", "edit", str(board), "--body-file", "-", stdin=body)
    else:
        url = gh("issue", "create", "--title", TITLE, "--body-file", "-", stdin=body).strip()
        gh("issue", "pin", url)


def check():
    d = date(2026, 10, 1)
    issues = [{"created": d, "closed": d + timedelta(1), "work": 3}, {"created": d + timedelta(1), "closed": None, "work": 6}]
    expected = ([d, d + timedelta(1), d + timedelta(2)], [3, 9, 9], [0, 3, 3], [9.0, 4.5, 0.0])
    if series(issues, d + timedelta(2), d + timedelta(2)) != expected:
        raise RuntimeError("series() self-check failed")
    node = {"timelineItems": {"nodes": [{"createdAt": "2026-10-01T08:00:00Z"}]},
            "closedByPullRequestsReferences": {"nodes": [{"createdAt": "2026-10-01T11:30:00Z"}]}}
    idle = {"timelineItems": {"nodes": [{"createdAt": "2026-10-05T10:00:00Z"}]},
            "closedByPullRequestsReferences": {"nodes": [{"createdAt": "2026-10-08T10:00:00Z"}]}}
    late = {"timelineItems": {"nodes": [{"createdAt": "2026-10-05T23:00:00Z"}]},
            "closedByPullRequestsReferences": {"nodes": [{"createdAt": "2026-10-06T00:30:00Z"}]}}
    open_ = {"timelineItems": node["timelineItems"], "closedByPullRequestsReferences": {"nodes": []}}
    expected = [(node, None, 2), (idle, None, 8), (late, None, 1.5), (open_, datetime(2026, 10, 1, 9, 0, tzinfo=timezone.utc), 1)]
    if worked(open_) is not None or not all(math.isclose(worked(n, until), hours) for n, until, hours in expected):
        raise RuntimeError("worked() self-check failed")

    def timed(number, estimate, actual=None, measured=None, closed=d, started=True):
        return {"number": number, "created": d, "closed": closed, "estimate": estimate, "actual": actual,
                "work": work(estimate, None), "measured": measured, "started": started, "who": "a"}

    if (spent(timed(1, 2, actual=5, measured=0.1)) != (5, "logged") or spent(timed(2, 2, measured=18)) != (2, "left running")
            or spent(timed(3, None, measured=0.2)) != (0.5, "assigned at PR") or spent(timed(4, 4, measured=3)) != (3, "measured")
            or spent(timed(5, 4, closed=None, started=False)) != (0, "not started")
            or spent(timed(6, 4, measured=0.2, closed=None)) != (0.2, "measured")):
        raise RuntimeError("spent() self-check failed")
    report = time_report([timed(1, 2, measured=3.5), timed(2, 6, closed=None), timed(3, 2, measured=18)],
                         d + timedelta(1), d, d + timedelta(2))
    if ("Planned 10 h, done 4 h (40%). 50% of the sprint has passed, so 5 h were expected by now: 1 h behind."
            not in report or "| #1 | a | 2 | 3.5 | measured | +1.5 h (+75%) |" not in report
            or "| #3 | a | 2 | – | ⚠️ left running | – |" not in report or "| **Total** (1 of 2) |" not in report):
        raise RuntimeError("time_report() self-check failed")


if __name__ == "__main__":
    check()
    main()
