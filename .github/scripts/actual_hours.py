"""The "Actual hours" PR check, and a helper to log them.

Check: fails while an issue the PR closes has no Actual (hours really spent) on the project. Reads PR_NUMBER.
Log:   python3 .github/scripts/actual_hours.py --set <issue> <hours>
"""
import os
import subprocess
import sys

from project_sync import PROJECT, graphql
CLOSING = """query($owner: String!, $name: String!, $pr: Int!) {
  repository(owner: $owner, name: $name) {
    pullRequest(number: $pr) {
      closingIssuesReferences(first: 20) { nodes { number title ...Actual } }
    }
  }
}"""
ISSUE = """query($owner: String!, $name: String!, $issue: Int!) {
  repository(owner: $owner, name: $name) { issue(number: $issue) { number title ...Actual } }
}"""
ACTUAL = """fragment Actual on Issue {
  projectItems(first: 10) {
    nodes {
      id
      project { id number field(name: "Actual") { ... on ProjectV2Field { id } } }
      actual: fieldValueByName(name: "Actual") { ... on ProjectV2ItemFieldNumberValue { number } }
    }
  }
}"""
SET = """mutation($project: ID!, $item: ID!, $field: ID!, $hours: Float!) {
  updateProjectV2ItemFieldValue(input: {projectId: $project, itemId: $item, fieldId: $field, value: {number: $hours}}) {
    clientMutationId
  }
}"""


def repo():
    """{"owner", "name"} of this repository: from Actions, or from the checkout."""
    full = os.environ.get("GITHUB_REPOSITORY") or subprocess.run(
        ["gh", "repo", "view", "--json", "nameWithOwner", "-q", ".nameWithOwner"], check=True, capture_output=True,
        text=True).stdout.strip()
    return dict(zip(("owner", "name"), full.split("/")))


def item(issue):
    """The issue's card on the project, or None when it is not on the board (or the token cannot see it)."""
    return next((n for n in issue["projectItems"]["nodes"] if n["project"]["number"] == PROJECT), None)


def missing(issues):
    """The issues that have no Actual yet."""
    return [i for i in issues if not item(i) or (item(i)["actual"] or {}).get("number") is None]


def check_pr(number):
    issues = graphql(CLOSING + ACTUAL, **repo(), pr=int(number))["repository"]["pullRequest"]["closingIssuesReferences"]["nodes"]
    if not issues:
        print("This PR closes no issue, so there are no hours to check.")
        return
    gaps = missing(issues)
    for i in issues:
        if i not in gaps:
            print(f"#{i['number']}: {item(i)['actual']['number']:g} h logged.")
    for i in gaps:
        print(f"::error::#{i['number']} ({i['title']}) has no Actual hours. Log the hours you really spent on it:"
              f" the Actual field on the project board, or `python3 .github/scripts/actual_hours.py --set"
              f" {i['number']} <hours>`. Then re-run this check.")
    if gaps:
        sys.exit(1)


def log(number, hours):
    issue = graphql(ISSUE + ACTUAL, **repo(), issue=int(number))["repository"]["issue"]
    card = item(issue)
    if not card:
        sys.exit(f"#{number} is not on project {PROJECT}.")
    graphql(SET, project=card["project"]["id"], item=card["id"], field=card["project"]["field"]["id"], hours=float(hours))
    print(f"#{number} ({issue['title']}): Actual = {float(hours):g} h")


def check():
    def issue(number, actual, project=PROJECT):
        return {"number": number, "projectItems": {"nodes": [{"project": {"number": project},
                                                              "actual": {"number": actual} if actual is not None else None}]}}

    logged, zero, empty, elsewhere = issue(1, 2.5), issue(2, 0), issue(3, None), issue(4, 3, project=9)
    if missing([logged, zero, empty, elsewhere]) != [empty, elsewhere]:
        raise RuntimeError("missing() self-check failed")


if __name__ == "__main__":
    check()
    if "--set" in sys.argv:
        at = sys.argv.index("--set")
        log(sys.argv[at + 1], sys.argv[at + 2])
    else:
        check_pr(os.environ["PR_NUMBER"])
