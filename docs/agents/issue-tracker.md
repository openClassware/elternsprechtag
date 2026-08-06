# Issue tracker: GitHub Issues

Issues and specs (you may know a spec as a PRD) for this repo live as **GitHub Issues** in
`openClassware/elternsprechtag`. Reach them with the `gh` CLI.

## Conventions

- A spec is a single issue holding the full Problem Statement / Solution / User Stories / Decisions
  body. Implementation tickets are separate issues that reference it under a `## Parent` heading.
- Implementation tickets are one issue per ticket — never a single combined tickets issue.
- Triage state is a **label** on the issue (see `triage-labels.md` for the role strings). All five
  labels exist in the repo.
- Blocking edges are a `## Blocked by` section in the issue body listing the blocking issue numbers
  (`- #36`), or "None — can start immediately". GitHub renders these as backlinks, so a blocker
  shows every ticket waiting on it.
- Comments and conversation history are issue comments.

## When a skill says "publish to the issue tracker"

Create the issues with `gh issue create --repo openClassware/elternsprechtag`, in dependency order
(blockers first) so each ticket's `## Blocked by` can name real issue numbers. Apply the triage
label the skill asks for via `--label`.

Do not close or modify the parent spec issue when publishing tickets derived from it.

## When a skill says "fetch the relevant ticket"

`gh issue view <number> --repo openClassware/elternsprechtag --json title,body,labels,comments`.
The user will normally pass the issue number or URL directly. Read the full body **and** the
comments — decisions often land in the comments.

## Wayfinding operations

Used by `/wayfinder`. The **map** is one issue with one **child** issue per open question.

- **Map**: an issue carrying the Notes / Decisions-so-far / Fog body. Keep it current by editing
  the body (`gh issue edit`), not by appending comments.
- **Child ticket**: one issue per question, referencing the map under `## Parent`. A `Type:` line
  near the top of the body records the ticket type (`research`/`prototype`/`grilling`/`task`).
- **Blocking**: a `## Blocked by` section listing issue numbers. A ticket is unblocked when every
  issue it lists is closed.
- **Claim**: assign the issue to yourself (`gh issue edit <n> --add-assignee @me`) before any work.
- **Frontier**: among the map's children, the open, unblocked, unassigned ones; lowest issue number
  wins.
- **Resolve**: post the answer as a comment under an `## Answer` heading, close the issue, then
  append a context pointer (gist + link to the comment) to the map's Decisions-so-far.

## Note on `.scratch/`

`.scratch/` is gitignored scratch space and holds leftovers from before this repo moved to GitHub
Issues. It is **not** the issue tracker. Don't read tickets from it and don't write tickets to it.
