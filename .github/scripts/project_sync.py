"""Keeps the project board in step with the work, so the roadmap view draws itself.

Issue assigned: Status -> In progress, Start date = today, Target date = the milestone's due date.
PR opened: Target date of every issue it closes = today.
Reads the event GitHub Actions hands over. Run with --print to see the changes without touching the project.
"""
import json
import os
import re
import subprocess
import sys
from datetime import date

PROJECT = 3  # the repo owner's "orbit" project
QUERY = """query($owner: String!, $name: String!, $project: Int!, $issue: Int!) {
  user(login: $owner) {
    projectV2(number: $project) {
      id
      fields(first: 50) {
        nodes {
          ... on ProjectV2FieldCommon { id name }
          ... on ProjectV2SingleSelectField { options { id name } }
        }
      }
    }
  }
  repository(owner: $owner, name: $name) {
    issue(number: $issue) {
      state
      milestone { dueOn }
      projectItems(first: 20) {
        nodes {
          id
          project { number }
          start: fieldValueByName(name: "Start date") { ... on ProjectV2ItemFieldDateValue { date } }
        }
      }
    }
  }
}"""
MUTATION = """mutation($project: ID!, $item: ID!, $field: ID!, $value: ProjectV2FieldValue!) {
  updateProjectV2ItemFieldValue(input: {projectId: $project, itemId: $item, fieldId: $field, value: $value}) {
    clientMutationId
  }
}"""


def graphql(query, **variables):
    request = json.dumps({"query": query, "variables": variables})
    done = subprocess.run(["gh", "api", "graphql", "--input", "-"], input=request, check=True, capture_output=True,
                          text=True)
    return json.loads(done.stdout)["data"]


def closed_issues(body):
    """Issue numbers a PR body closes, e.g. "Closes #42"."""
    return [int(n) for n in re.findall(r"(?i)\b(?:close[sd]?|fix(?:e[sd])?|resolve[sd]?):?\s+#(\d+)", body or "")]


def sync(issue, started, today, dry):
    owner, name = os.environ["GITHUB_REPOSITORY"].split("/")
    data = graphql(QUERY, owner=owner, name=name, project=PROJECT, issue=issue)
    project, node = data["user"]["projectV2"], data["repository"]["issue"]
    item = next((i for i in node["projectItems"]["nodes"] if i["project"]["number"] == PROJECT), None)
    if not item or (started and node["state"] == "CLOSED"):
        print(f"#{issue}: not on the board or already closed, nothing to do")
        return
    fields = {f["name"]: f for f in project["fields"]["nodes"]}
    values = {"Target date": {"date": today}}
    if started:
        in_progress = next(o["id"] for o in fields["Status"]["options"] if o["name"] == "In progress")
        values = {"Status": {"singleSelectOptionId": in_progress}}
        if not item["start"]:  # a reassignment keeps the day the work really started
            values["Start date"] = {"date": today}
        if node["milestone"] and node["milestone"]["dueOn"]:
            values["Target date"] = {"date": node["milestone"]["dueOn"][:10]}
    for field, value in values.items():
        print(f"#{issue}: {field} = {next(iter(value.values()))}")
        if not dry:
            graphql(MUTATION, project=project["id"], item=item["id"], field=fields[field]["id"], value=value)


def main():
    with open(os.environ["GITHUB_EVENT_PATH"]) as file:
        event = json.load(file)
    today, dry = date.today().isoformat(), "--print" in sys.argv
    if "pull_request" in event:
        for issue in closed_issues(event["pull_request"]["body"]):
            sync(issue, False, today, dry)
    else:
        sync(event["issue"]["number"], True, today, dry)


def check():
    if closed_issues("Adds it.\n\nCloses #42\nsee #7, fixes #9") != [42, 9] or closed_issues(None):
        raise RuntimeError("closed_issues() self-check failed")


if __name__ == "__main__":
    check()
    main()
