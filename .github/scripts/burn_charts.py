"""Rebuilds the burn-up and burn-down charts in the "Burn charts" issue from the repo's issues.

One pair of charts per milestone (sprint) plus one for the whole project, each with a time report:
estimated hours against the time the issues really took.
Run with --print to see the markdown without touching GitHub.
"""
import json
import os
import subprocess
import sys
from datetime import date, datetime, timedelta

TITLE = "Burn charts"
PROJECT = "3"  # the repo owner's "orbit" project, where the Estimate field (hours) lives
QUERY = """query($owner: String!, $name: String!, $endCursor: String) {
  repository(owner: $owner, name: $name) {
    issues(first: 100, after: $endCursor) {
      pageInfo { hasNextPage endCursor }
      nodes {
        number title createdAt closedAt
        milestone { title dueOn }
        assignees(first: 5) { nodes { login } }
        timelineItems(itemTypes: ASSIGNED_EVENT, first: 1) { nodes { ... on AssignedEvent { createdAt } } }
        closedByPullRequestsReferences(first: 10) { nodes { createdAt } }
      }
    }
  }
}"""


def gh(*args, stdin=None, env=None):
    return subprocess.run(["gh", *args], input=stdin, check=True, capture_output=True, text=True, env=env).stdout


def estimates():
    """Issue number -> estimated hours, from the project. Empty when the token cannot read the project."""
    # GITHUB_TOKEN cannot read a user-owned project, so CI passes a PAT with the project scope as PROJECT_TOKEN.
    token = os.environ.get("PROJECT_TOKEN")
    try:
        owner = gh("repo", "view", "--json", "owner", "-q", ".owner.login").strip()
        items = json.loads(gh("project", "item-list", PROJECT, "--owner", owner, "--limit", "1000", "--format", "json",
                              env={**os.environ, "GH_TOKEN": token} if token else None))["items"]
    except subprocess.CalledProcessError:
        return {}
    return {i["content"]["number"]: i["estimate"] for i in items
            if i.get("estimate") and i["content"].get("type") == "Issue"}


def worked(node):
    """Hours from the first assignment to the closing PR being opened, or None when either is missing."""
    assigned = [e["createdAt"] for e in node["timelineItems"]["nodes"]]
    opened = [p["createdAt"] for p in node["closedByPullRequestsReferences"]["nodes"]]
    if not assigned or not opened:
        return None
    start, end = (datetime.fromisoformat(s.replace("Z", "+00:00")) for s in (assigned[0], min(opened)))
    return round((end - start).total_seconds() / 3600, 1) if end >= start else None


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
    rows = [i for i in issues if i["closed"] and i["estimate"] and i["hours"] is not None]
    if not rows:
        return out + "\n\n"
    estimate, actual = sum(i["estimate"] for i in rows), sum(i["hours"] for i in rows)
    out += "\n\n| Issue | Assignee | Estimate (h) | Actual (h) | Deviation |\n|---|---|---|---|---|\n"
    if table:
        out += "".join(f"| #{i['number']} | {i['who']} | {i['estimate']:g} | {i['hours']:g} |"
                       f" {deviation(i['estimate'], i['hours'])} |\n" for i in rows)
    out += f"| **Total** | | {estimate:g} | {actual:g} | {deviation(estimate, actual)} |\n\n"
    # ponytail: actual is elapsed time, not logged hours. Add an "Actual" number field to the project if the team logs them.
    return out + "Actual is the time from the first assignment to the closing PR being opened.\n\n"


def series(issues, today, due=None):
    """Per day from the first issue to today: (days, scope, done, ideal remaining)."""
    start = min(i["created"] for i in issues)
    days = [start + timedelta(n) for n in range((today - start).days + 1)]
    scope = [sum(i["created"] <= d for i in issues) for d in days]
    done = [sum(i["closed"] is not None and i["closed"] <= d for i in issues) for d in days]
    span = max((due - start).days, 1) if due else None
    ideal = [round(len(issues) * max(1 - (d - start).days / span, 0), 1) for d in days] if due else None
    return days, scope, done, ideal


def charts(name, issues, today, due=None):
    days, scope, done, ideal = series(issues, today, due)
    remaining = [s - d for s, d in zip(scope, done)]
    x = json.dumps([d.strftime("%d/%m") for d in days])
    top = max(scope) + 1
    # ponytail: counts issues, not hours. Weight by the project's Estimate field if the team starts filling it in.
    up = f'xychart-beta\n  title "{name}: burn-up (scope and done)"\n  x-axis {x}\n  y-axis "Issues" 0 --> {top}\n  line {scope}\n  line {done}'
    down = f'xychart-beta\n  title "{name}: burn-down (remaining{" and ideal" if ideal else ""})"\n  x-axis {x}\n  y-axis "Issues" 0 --> {top}\n  line {remaining}'
    if ideal:
        down += f"\n  line {ideal}"
    return f"## {name}\n\n{done[-1]} of {scope[-1]} issues done.\n\n```mermaid\n{up}\n```\n\n```mermaid\n{down}\n```\n"


def day(stamp):
    return date.fromisoformat(stamp[:10]) if stamp else None


def main():
    pages = json.loads(gh("api", "graphql", "--paginate", "--slurp", "-F", "owner={owner}", "-F", "name={repo}",
                          "-f", f"query={QUERY}"))
    raw = [n for p in pages for n in p["data"]["repository"]["issues"]["nodes"]]
    board = next((i["number"] for i in raw if i["title"] == TITLE), None)
    hours = estimates()
    issues = [{"number": i["number"], "created": day(i["createdAt"]), "closed": day(i["closedAt"]),
               "milestone": i["milestone"], "estimate": hours.get(i["number"]), "hours": worked(i),
               "who": ", ".join(a["login"] for a in i["assignees"]["nodes"])}
              for i in raw if i["number"] != board]
    today = date.today()
    body = "Updated automatically by `.github/workflows/burn-charts.yml`. Do not edit.\n\n"
    if not issues:
        body += "No issues yet.\n"
    elif not hours:
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
    issues = [{"created": d, "closed": d + timedelta(1)}, {"created": d + timedelta(1), "closed": None}]
    expected = ([d, d + timedelta(1), d + timedelta(2)], [1, 2, 2], [0, 1, 1], [2.0, 1.0, 0.0])
    if series(issues, d + timedelta(2), d + timedelta(2)) != expected:
        raise RuntimeError("series() self-check failed")
    node = {"timelineItems": {"nodes": [{"createdAt": "2026-10-01T08:00:00Z"}]},
            "closedByPullRequestsReferences": {"nodes": [{"createdAt": "2026-10-01T11:30:00Z"}]}}
    if worked(node) != 3.5:
        raise RuntimeError("worked() self-check failed")
    timed = [{"number": 1, "created": d, "closed": d, "estimate": 2, "hours": 3.5, "who": "a"},
             {"number": 2, "created": d, "closed": None, "estimate": 6, "hours": None, "who": ""}]
    report = time_report(timed, d + timedelta(1), d, d + timedelta(2))
    if "Planned 8 h, done 2 h (25%). 50% of the sprint has passed, so 4 h were expected by now: 2 h behind." \
            not in report or "| #1 | a | 2 | 3.5 | +1.5 h (+75%) |" not in report:
        raise RuntimeError("time_report() self-check failed")


if __name__ == "__main__":
    check()
    main()
