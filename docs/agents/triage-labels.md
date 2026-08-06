# Triage Labels

The skills speak in terms of five canonical triage roles. This file maps those roles to the actual label strings used in this repo's issue tracker.

| Label in mattpocock/skills | Label in our tracker | Meaning                                  |
| -------------------------- | -------------------- | ---------------------------------------- |
| `needs-triage`             | `needs-triage`       | Maintainer needs to evaluate this issue  |
| `needs-info`               | `needs-info`         | Waiting on reporter for more information |
| `ready-for-agent`          | `ready-for-agent`    | Fully specified, ready for an AFK agent  |
| `ready-for-human`          | `ready-for-human`    | Requires human implementation            |
| `wontfix`                  | `wontfix`            | Will not be actioned                     |

When a skill mentions a role (e.g. "apply the AFK-ready triage label"), use the corresponding label string from this table.

## Zusätzlich: `blocked`

Über die fünf Rollen hinaus kennt dieses Repo das Label `blocked`: Das Issue wartet auf ein
anderes (siehe seinen `## Blocked by`-Abschnitt). `blocked` **ersetzt** die Umsetzungs-Rolle,
statt neben ihr zu stehen — ein blockiertes Ticket trägt weder `ready-for-agent` noch
`ready-for-human`, damit es nicht versehentlich gegriffen wird. Ist der letzte Blocker
geschlossen, wird `blocked` entfernt und die Umsetzungs-Rolle wieder gesetzt.

Reine Spec-Issues (der Parent, auf den Tickets unter `## Parent` verweisen) tragen **gar keine**
Umsetzungs-Rolle — sie sind nichts zum Bauen.

This repo uses GitHub Issues (see `issue-tracker.md`), so a role is a real GitHub label of the same
name — all five exist in `openClassware/elternsprechtag`. Apply one with
`gh issue create --label <name>` or `gh issue edit <n> --add-label <name>`.

Edit the right-hand column to match whatever vocabulary you actually use.
