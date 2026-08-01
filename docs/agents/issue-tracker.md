# Issue tracker: GitHub

Issues and PRDs for this repo live as GitHub issues. Use the `gh` CLI for all operations.

## Conventions

- Create, read, edit, comment on, label, and close issues using `gh issue`.
- Infer the repository from `git remote -v`.
- A bare GitHub number can identify either an issue or PR; resolve it before acting.

## Pull requests as a triage surface

**PRs as a request surface: no.**

External pull requests do not enter the issue-triage queue.

## Publishing and fetching

- When a skill says “publish to the issue tracker,” create a GitHub issue.
- When a skill says “fetch the relevant ticket,” use `gh issue view <number> --comments`.

## Wayfinding operations

- A map is an issue labelled `wayfinder:map`.
- Tickets are native GitHub sub-issues labelled `wayfinder:research`, `wayfinder:prototype`, `wayfinder:grilling`, or `wayfinder:task`.
- Use GitHub’s native issue dependencies for blocking relationships.
- The frontier consists of open child issues without open blockers or assignees.
- Claim a ticket with `gh issue edit <number> --add-assignee @me`.
- Resolve it by posting the answer, closing the ticket, and appending a linked gist to the map’s “Decisions so far” section.
