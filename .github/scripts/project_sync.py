"""Keeps the project board in step with the work, so the roadmap view draws itself.

Every run reads the whole board and writes what differs from these rules. It does not matter which event woke it.
Not started: the bar is the plan. It starts when the issue's blockers end and a member is free, never before its
sprint or in the past, and lasts as long as its estimate says.
Assigned: Status -> In progress, the bar starts the day of the assignment and ends when the estimate says it should.
PR opened: the bar ends that day, and Deviation (days) says how far that is from the estimate.
Run with --print to see the changes without touching the project. docs/project-automation/ has the full picture.
"""
import json
import math
import os
import subprocess
import sys
from datetime import date, timedelta

PROJECT = 3  # the repo owner's "orbit" project
HOURS_PER_DAY = 2  # what one member is expected to put into the project per day, weekends included
TEAM = 4  # members, so at most this many issues run at once
SIZE_HOURS = {"XS": 1, "S": 3, "M": 6, "L": 12, "XL": 20}  # for issues without an Estimate
QUERY = """query($owner: String!, $name: String!, $project: Int!, $cursor: String) {
  repository(owner: $owner, name: $name) {
    milestones(first: 50) { nodes { title createdAt dueOn } }
  }
  user(login: $owner) {
    projectV2(number: $project) {
      id
      fields(first: 50) {
        nodes {
          ... on ProjectV2FieldCommon { id name }
          ... on ProjectV2SingleSelectField { options { id name } }
        }
      }
      items(first: 100, after: $cursor) {
        pageInfo { hasNextPage endCursor }
        nodes {
          id
          status: fieldValueByName(name: "Status") { ... on ProjectV2ItemFieldSingleSelectValue { name } }
          priority: fieldValueByName(name: "Priority") { ... on ProjectV2ItemFieldSingleSelectValue { name } }
          size: fieldValueByName(name: "Size") { ... on ProjectV2ItemFieldSingleSelectValue { name } }
          estimate: fieldValueByName(name: "Estimate") { ... on ProjectV2ItemFieldNumberValue { number } }
          start: fieldValueByName(name: "Start date") { ... on ProjectV2ItemFieldDateValue { date } }
          target: fieldValueByName(name: "Target date") { ... on ProjectV2ItemFieldDateValue { date } }
          deviation: fieldValueByName(name: "Deviation (days)") { ... on ProjectV2ItemFieldNumberValue { number } }
          content {
            ... on Issue {
              number state
              milestone { title }
              labels(first: 20) { nodes { name } }
              assignees { totalCount }
              blockedBy(first: 50) { nodes { number } }
              timelineItems(itemTypes: ASSIGNED_EVENT, first: 1) { nodes { ... on AssignedEvent { createdAt } } }
              closedByPullRequestsReferences(first: 10) { nodes { createdAt } }
            }
          }
        }
      }
    }
  }
}"""


def graphql(query, **variables):
    request = json.dumps({"query": query, "variables": variables})
    done = subprocess.run(["gh", "api", "graphql", "--input", "-"], input=request, check=True, capture_output=True,
                          text=True)
    return json.loads(done.stdout)["data"]


def load():
    """The board: project id, fields by name, the items that are issues, and the sprint windows."""
    owner, name = os.environ["GITHUB_REPOSITORY"].split("/")
    items, cursor = [], None
    while True:
        data = graphql(QUERY, owner=owner, name=name, project=PROJECT, cursor=cursor)
        project = data["user"]["projectV2"]
        items += [i for i in project["items"]["nodes"] if i["content"]]
        cursor = project["items"]["pageInfo"]["endCursor"]
        if not project["items"]["pageInfo"]["hasNextPage"]:
            fields = {f["name"]: f for f in project["fields"]["nodes"]}
            return project["id"], fields, items, windows(data["repository"]["milestones"]["nodes"])


def windows(milestones):
    """Sprint title -> (first day, due day). A sprint starts when the one before it is due."""
    out, previous = {}, None
    for sprint in sorted((m for m in milestones if m["dueOn"]), key=lambda m: m["dueOn"]):
        out[sprint["title"]] = (previous or sprint["createdAt"][:10], sprint["dueOn"][:10])
        previous = sprint["dueOn"][:10]
    return out


def value(item, field):
    return next(iter((item.get(field) or {}).values()), None)


def shift(day, days):
    return (date.fromisoformat(day) + timedelta(days)).isoformat()


def hours(item):
    """The Estimate, or the hours its size stands for (board field, then the `size:` label), or None."""
    labels = (label["name"] for label in item["content"]["labels"]["nodes"])
    size = value(item, "size") or next((name[6:] for name in labels if name.startswith("size: ")), None)
    return value(item, "estimate") or SIZE_HOURS.get(size)


def last_day(start, item):
    """The day work that starts on `start` should end: 8 h at 2 h a day -> 4 days, so three days later."""
    return shift(start, math.ceil((hours(item) or SIZE_HOURS["S"]) / HOURS_PER_DAY) - 1)


def progress(item):
    """Where an issue is: its start and end when a PR closes it, its start when someone has it, else nothing."""
    issue = item["content"]
    assigned = next((e["createdAt"][:10] for e in issue["timelineItems"]["nodes"]), None)
    opened = min((p["createdAt"][:10] for p in issue["closedByPullRequestsReferences"]["nodes"]), default=None)
    if opened:
        return min(assigned or opened, opened), opened
    return (assigned if issue["assignees"]["totalCount"] else None), None


def schedule(items, sprints, today):
    """Issue number -> (start, end) for the open issues nobody has started."""
    # ponytail: members are interchangeable lanes. Plan per assignee if the team needs to see who does what.
    by_number = {i["content"]["number"]: i for i in items}
    waiting = [i for i in items if i["content"]["state"] == "OPEN" and progress(i) == (None, None)]
    running = [max(last_day(progress(i)[0], i), today) for i in items
               if i["content"]["state"] == "OPEN" and progress(i)[0] and not progress(i)[1]]
    # The day each member is free again: the work in progress keeps its lane until it should end.
    free = [shift(day, 1) for day in sorted(running, reverse=True)[:TEAM]] + [today] * (TEAM - len(running))
    plan = {}

    def end(number, path):
        """The day `number` stops holding others back, or None when it is done, unknown or part of a cycle."""
        item = by_number.get(number)
        if not item or item["content"]["state"] == "CLOSED" or number in path or progress(item)[1]:
            return None
        if progress(item)[0]:
            return max(last_day(progress(item)[0], item), today)
        return place(number, path)

    def sprint(item):
        return sprints.get((item["content"]["milestone"] or {}).get("title"))

    def place(number, path=()):
        item = by_number[number]
        window = sprint(item)
        if number not in plan and window:
            ends = [end(b["number"], path + (number,)) for b in item["content"]["blockedBy"]["nodes"]]
            ready = max([today, window[0]] + [shift(e, 1) for e in ends if e])
            # The member who has waited least by then, so nobody idles while another one queues work.
            lane = max((n for n in range(TEAM) if free[n] <= ready), key=free.__getitem__,
                       default=min(range(TEAM), key=free.__getitem__))
            start = max(ready, free[lane])
            plan[number] = (start, last_day(start, item))
            free[lane] = shift(plan[number][1], 1)
        return plan.get(number, (None, None))[1]

    # Earlier sprints first, then the higher priority, then the older issue.
    for item in sorted(waiting, key=lambda i: ((sprint(i) or ("", "~"))[1], value(i, "priority") or "P9",
                                               i["content"]["number"])):
        place(item["content"]["number"])
    return plan


def wanted(item, today, plan):
    """The field values an item should have right now."""
    issue = item["content"]
    start, end = progress(item)
    if end:
        values = {"Start date": start, "Target date": end}
        if hours(item):
            values["Deviation (days)"] = (date.fromisoformat(end) - date.fromisoformat(last_day(start, item))).days
        return values
    if issue["state"] == "CLOSED":
        return {}
    if start:
        return {"Status": "In progress", "Start date": start, "Target date": last_day(start, item)}
    values = dict(zip(("Start date", "Target date"), plan.get(issue["number"], ())))
    if value(item, "status") == "In progress":  # nobody has it any more
        values["Status"] = "Planned"
    return values


def apply(project, fields, changes, dry):
    """Writes the (item, {field: value}) pairs that differ from the board, twenty field values per request."""
    now = {"Status": "status", "Start date": "start", "Target date": "target", "Deviation (days)": "deviation"}
    updates = []
    for item, values in changes:
        for field, new in values.items():
            if value(item, now[field]) == new:
                continue
            print(f"#{item['content']['number']}: {field} = {new}")
            if field == "Status":
                new = {"singleSelectOptionId": next(o["id"] for o in fields[field]["options"] if o["name"] == new)}
            else:
                new = {"number": new} if isinstance(new, int) else {"date": new}
            updates.append((item["id"], fields[field]["id"], new))
    for at in range(0, 0 if dry else len(updates), 20):
        batch = updates[at:at + 20]
        mutation = "mutation(%s) {\n%s\n}" % (
            ", ".join(f"$v{n}: ProjectV2FieldValue!" for n in range(len(batch))),
            "\n".join(f'm{n}: updateProjectV2ItemFieldValue(input: {{projectId: "{project}", itemId: "{item}",'
                      f' fieldId: "{field}", value: $v{n}}}) {{ clientMutationId }}'
                      for n, (item, field, _) in enumerate(batch)))
        graphql(mutation, **{f"v{n}": new for n, (_, _, new) in enumerate(batch)})


def main():
    today = date.today().isoformat()
    project, fields, items, sprints = load()
    plan = schedule(items, sprints, today)
    apply(project, fields, [(i, wanted(i, today, plan)) for i in items], "--print" in sys.argv)


def check():
    sprints = windows([{"title": "S2", "createdAt": "2026-09-01T00:00:00Z", "dueOn": "2026-11-27T00:00:00Z"},
                       {"title": "S1", "createdAt": "2026-10-01T00:00:00Z", "dueOn": "2026-10-27T00:00:00Z"}])
    if sprints != {"S1": ("2026-10-01", "2026-10-27"), "S2": ("2026-10-27", "2026-11-27")}:
        raise RuntimeError("windows() self-check failed")

    def issue(number, blockers=(), assigned=None, opened=None, **fields):
        return {**{name: {"x": content} for name, content in fields.items()},
                "content": {"number": number, "state": "OPEN", "milestone": {"title": "S1"},
                            "labels": {"nodes": [{"name": "size: L"}]},
                            "assignees": {"totalCount": int(bool(assigned))},
                            "blockedBy": {"nodes": [{"number": b} for b in blockers]},
                            "timelineItems": {"nodes": [{"createdAt": assigned}] if assigned else []},
                            "closedByPullRequestsReferences": {"nodes": [{"createdAt": opened}] if opened else []}}}

    today = "2026-10-07"
    # 8 h is four days, the L label is six, and #2 waits for #1 even though it outranks it.
    first, second, third = issue(1, estimate=8), issue(2, blockers=[1, 2], priority="P0", estimate=3), issue(3)
    plan = schedule([second, first, third], sprints, today)
    if plan != {1: (today, "2026-10-10"), 2: ("2026-10-11", "2026-10-12"), 3: (today, "2026-10-12")}:
        raise RuntimeError("schedule() self-check failed")
    taken = issue(1, assigned="2026-10-07T09:00:00Z", estimate=8, status="Planned")
    late = issue(1, assigned="2026-10-07T09:00:00Z", opened="2026-10-12T09:00:00Z", estimate=8)
    if (wanted(second, today, plan) != {"Start date": "2026-10-11", "Target date": "2026-10-12"}
            or wanted(taken, today, plan) != {"Status": "In progress", "Start date": today,
                                              "Target date": "2026-10-10"}
            or wanted(late, today, plan) != {"Start date": today, "Target date": "2026-10-12", "Deviation (days)": 2}
            or wanted(issue(4, status="In progress"), today, {}) != {"Status": "Planned"}):
        raise RuntimeError("wanted() self-check failed")


if __name__ == "__main__":
    check()
    main()
