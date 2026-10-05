"""Rebuilds the burn-up and burn-down charts in the "Burn charts" issue from the repo's issues.

One pair of charts per milestone (sprint) plus one for the whole project.
Run with --print to see the markdown without touching GitHub.
"""
import json
import subprocess
import sys
from datetime import date, timedelta

TITLE = "Burn charts"


def gh(*args, stdin=None):
    return subprocess.run(["gh", *args], input=stdin, check=True, capture_output=True, text=True).stdout


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
    raw = json.loads(gh("issue", "list", "--state", "all", "--limit", "1000",
                        "--json", "number,title,createdAt,closedAt,milestone"))
    board = next((i["number"] for i in raw if i["title"] == TITLE), None)
    issues = [{"created": day(i["createdAt"]), "closed": day(i["closedAt"]), "milestone": i["milestone"]}
              for i in raw if i["number"] != board]
    today = date.today()
    body = "Updated automatically by `.github/workflows/burn-charts.yml`. Do not edit.\n\n"
    if not issues:
        body += "No issues yet.\n"
    milestones = {i["milestone"]["title"]: day(i["milestone"].get("dueOn")) for i in issues if i["milestone"]}
    for title in sorted(milestones):
        body += charts(title, [i for i in issues if i["milestone"] and i["milestone"]["title"] == title],
                       today, milestones[title])
    if issues:
        body += charts("Whole project", issues, today)

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
    assert series(issues, d + timedelta(2), d + timedelta(2)) == (
        [d, d + timedelta(1), d + timedelta(2)], [1, 2, 2], [0, 1, 1], [2.0, 1.0, 0.0])


if __name__ == "__main__":
    check()
    main()
