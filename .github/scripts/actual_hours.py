"""The "Actual hours" PR check, and a helper to log them.

Check: fails while an issue the PR closes has no Actual (hours really spent) on the project, and keeps one comment on
the PR that says how to log them, deleted once they are. Reads PR_NUMBER, and COMMENT_TOKEN for the comment.
Log:   python3 .github/scripts/actual_hours.py --set <issue> <hours>
"""
import json
import os
import subprocess
import sys

from project_sync import PROJECT, graphql

MARKER = "<!-- actual-hours -->"  # finds the check's own comment among the PR's
BOARD = "https://github.com/users/Erkko68/projects/3"
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


def message(gaps):
    """The PR comment for the issues that still have no Actual."""
    rows = "".join(f"| #{i['number']} {i['title']} | `python3 .github/scripts/actual_hours.py --set {i['number']} <hours>` |\n"
                   for i in gaps)
    return (f"{MARKER}\n### ⏱️ Log your hours before review\n\n"
            f"The **Actual hours** check fails until every issue this PR closes has the hours really spent on it.\n\n"
            f"| Issue | From the repo, in a terminal |\n|---|---|\n{rows}\n"
            f"**Or on the board:** open the issue's card on the [project]({BOARD}) and fill **Actual** with your hours"
            f" (decimals allowed, review fixes included).\n\n"
            f"**Then re-run the check:** a board field triggers nothing. Checks tab → *Actual hours* → *Re-run*, or push,"
            f" or edit this PR's description.\n\nThis comment goes away once the hours are logged.\n")


def comment(pr, body):
    """Posts or updates the check's one comment on the PR, or deletes it when `body` is None. Only in Actions, as
    the workflow's own token, so it comes from github-actions. A failure only warns: the check still decides."""
    token = os.environ.get("COMMENT_TOKEN")
    if not token:
        return
    env = {**os.environ, "GH_TOKEN": token}

    def gh(*args, payload=None):
        return subprocess.run(["gh", "api", *args] + (["--input", "-"] if payload else []), check=True, text=True,
                              capture_output=True, env=env, input=json.dumps(payload) if payload else None).stdout

    try:
        pages = json.loads(gh(f"repos/{{owner}}/{{repo}}/issues/{pr}/comments", "--paginate", "--slurp"))
        mine = next((c for page in pages for c in page if MARKER in (c["body"] or "")), None)
        if body is None and mine:
            gh("-X", "DELETE", f"repos/{{owner}}/{{repo}}/issues/comments/{mine['id']}")
        elif body and mine and mine["body"] != body:
            gh("-X", "PATCH", f"repos/{{owner}}/{{repo}}/issues/comments/{mine['id']}", payload={"body": body})
        elif body and not mine:
            gh("-X", "POST", f"repos/{{owner}}/{{repo}}/issues/{pr}/comments", payload={"body": body})
    except subprocess.CalledProcessError as error:
        print(f"::warning::Could not update the PR comment: {error.stderr.strip()}")


def check_pr(number):
    issues = graphql(CLOSING + ACTUAL, **repo(), pr=int(number))["repository"]["pullRequest"]["closingIssuesReferences"]["nodes"]
    if not issues:
        print("This PR closes no issue, so there are no hours to check.")
        comment(number, None)
        return
    gaps = missing(issues)
    comment(number, message(gaps) if gaps else None)
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
    text = message([{**empty, "title": "Some issue"}])
    if not text.startswith(MARKER) or "| #3 Some issue | `python3 .github/scripts/actual_hours.py --set 3 <hours>` |" not in text:
        raise RuntimeError("message() self-check failed")


if __name__ == "__main__":
    check()
    if "--set" in sys.argv:
        at = sys.argv.index("--set")
        log(sys.argv[at + 1], sys.argv[at + 2])
    else:
        check_pr(os.environ["PR_NUMBER"])
