# Contributing to Orbit

All work starts from an issue on the [project board](https://github.com/users/Erkko68/projects/3) (Backlog, Planned, In progress, Done). There is one milestone per sprint. The complete rules, including architecture and code style, are in [AGENTS.md](AGENTS.md); this page is the day-to-day workflow.

## One-time setup

Install the GitHub CLI and log in:

```sh
brew install gh        # or https://cli.github.com
gh auth login
```

## Working on an issue

1. **Pick a free issue from the current milestone.**
   ```sh
   gh issue list --milestone "Sprint 1" --search "no:assignee"
   ```
   Open it and check that every issue under "Blocked by" is closed.
2. **Assign it to yourself** and move it to In progress on the board.
   ```sh
   gh issue edit 42 --add-assignee @me
   ```
   An assigned issue is taken. Don't work on someone else's issue unless you have agreed to help; in that case add yourself as a second assignee.
3. **Create the branch** `<type>/<issue>-short-name`, where `<type>` is `feature`, `fix`, `refactor`, `docs`, `test`, `chore` or `build`.
   ```sh
   git switch -c feature/42-balance-calculation
   ```
4. **Commit** with Conventional Commits and the issue number at the end of the subject. Keep commits small, one logical change each.
   ```
   feat(expenses): add balance calculation (#42)
   ```
5. **Check before pushing.**
   ```sh
   ./gradlew :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64
   ./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test
   ```
6. **Open one pull request** per complete functionality. The title is a Conventional Commit without the issue number; the body follows the template and contains `Closes #42`, which closes the issue and moves it to Done on merge.

If the work you want to do has no issue, create the issue first.

## Review and merge

- 1 approval from another team member, all conversations resolved, green CI (Android, iOS, PR title, branch name, SonarCloud).
- Squash merge only. No direct pushes to `main`.

## Working with AI agents

Agents read [AGENTS.md](AGENTS.md) and follow the same workflow: they check that `gh` is set up, ask you which issue of the current milestone to work on, assign it to you, and reference it in every commit and in the pull request. They never take an issue assigned to someone else unless you tell them you are helping on it.
