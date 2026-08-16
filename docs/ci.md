# CI & Branch Protection

Jeder Pull Request gegen `main` läuft durch die vollständige Testsuite, und nach `main` kommt
nichts anderes hinein als ein Pull Request mit grüner Prüfung. Damit landet kein ungetesteter
Stand auf der öffentlichen [Demo](deploy.md), und ein fremder Beitrag muss nicht von Hand
durchgetestet werden.

Zwei Teile: der Workflow im Repository und die Branch Protection als Repository-Einstellung.
Der Workflow ist versioniert, die Einstellung nicht — deshalb steht sie unten so genau, dass
sie nach einem Verlust rekonstruierbar ist.

## Der Workflow

[`.github/workflows/ci.yml`](../.github/workflows/ci.yml), ein Job namens **`Test-Gate`**:

```
Pull Request gegen main ─► Postgres-Service-Container hochfahren ─► ./mvnw -B verify
```

Der Job spiegelt das Test-Gate aus [`deploy-demo.yml`](../.github/workflows/deploy-demo.yml):
gleiches `postgres:17`, gleiche Verbindungswerte (`elternsprechtag` / `myuser` / `mysecret`
auf 5432), gleicher Healthcheck. Das ist Absicht — `ElternsprechtagApplicationTests` startet
den echten Spring-Context und damit die Default-Datasource aus `application.properties`, und
ein PR-Lauf soll dieselbe Umgebung sehen wie der Lauf unmittelbar vor einem Deploy.

Getrennte Workflows statt eines gemeinsamen: der Deploy hängt an `push: main`,
`workflow_dispatch` und dem [nächtlichen Zeitplan](deploy.md#täglicher-reset) und darf auf
einem Pull Request nicht mitlaufen — schon gar nicht auf einem aus einem Fork.

**Fork-Pull-Requests** laufen ohne Zutun mit. Der Trigger ist `pull_request`, der Job braucht
keine Secrets, und das Token ist auf `contents: read` beschränkt; die Deploy-Secrets sind für
ihn nicht erreichbar.

Ein neuer Push auf denselben PR bricht den vorigen Lauf ab (`cancel-in-progress`). Im Deploy
ist das bewusst andersherum.

## Branch Protection auf `main`

**Voraussetzung**: Branch Protection und Rulesets gibt es auf dem Free-Plan nur für
**öffentliche** Repositories — bei einem privaten antwortet die API mit *"Upgrade to GitHub
Pro or make this repository public"*. Das Repo ist öffentlich, damit ist die Bedingung
erfüllt; würde es je wieder privat gestellt, fiele die Regel ersatzlos weg.

*Settings → Branches → Branch protection rules*, Regel für den Branch-Namen `main`:

| Einstellung                                                    | Wert                       |
|----------------------------------------------------------------|----------------------------|
| Require a pull request before merging                          | an                         |
| ↳ Require approvals                                            | aus (Solo-Repo)            |
| Require status checks to pass before merging                   | an                         |
| ↳ Require branches to be up to date before merging             | an                         |
| ↳ Erforderliche Prüfung                                        | **`Test-Gate`**            |
| Require conversation resolution before merging                 | an                         |
| Allow force pushes                                             | aus                        |
| Allow deletions                                                | aus                        |
| Do not allow bypassing the above settings (gilt auch für Admins)| aus                        |

Zu zwei Punkten die Begründung, weil sie sonst wie Nachlässigkeit aussehen:

- **Keine Approvals.** Das Repo hat einen Maintainer; ein Pflicht-Review wäre eine Selbst-
  Freigabe und damit reine Zeremonie. Kommt ein zweiter Maintainer dazu, gehört das an.
- **Kein Admin-Bypass-Verbot.** Der Maintainer muss sich im Notfall (klemmender Workflow,
  kaputtes Token) selbst entsperren können, ohne die Regel löschen zu müssen. Die Regel
  schützt vor Versehen, nicht vor dem Maintainer.

Der Name der erforderlichen Prüfung ist der **Job-Name** aus `ci.yml`, nicht der Workflow-Name.
Wird der Job umbenannt, wartet die Branch Protection auf eine Prüfung, die nie startet, und
kein Merge geht mehr durch — beides muss zusammen geändert werden.

### Rekonstruktion

Nach einem Verlust der Regel stellt dieser Aufruf sie vollständig wieder her (`gh` CLI,
authentifiziert als Repo-Admin):

```bash
gh api -X PUT repos/openClassware/elternsprechtag/branches/main/protection \
  --input - <<'EOF'
{
  "required_status_checks": {
    "strict": true,
    "contexts": ["Test-Gate"]
  },
  "required_pull_request_reviews": {
    "required_approving_review_count": 0
  },
  "required_conversation_resolution": true,
  "enforce_admins": false,
  "restrictions": null,
  "allow_force_pushes": false,
  "allow_deletions": false
}
EOF
```

Prüfen lässt sich der Ist-Zustand mit:

```bash
gh api repos/openClassware/elternsprechtag/branches/main/protection
```
