# CI & Branch Protection

Jeder Pull Request gegen `main` läuft durch die vollständige Testsuite, und nach `main` kommt
nichts anderes hinein als ein Pull Request mit grüner Prüfung. Damit landet kein ungetesteter
Stand auf der öffentlichen [Demo](deploy.md), und ein fremder Beitrag muss nicht von Hand
durchgetestet werden.

Zwei Teile: der Workflow im Repository und die Branch Protection als Repository-Einstellung.
Der Workflow ist versioniert, die Einstellung nicht — deshalb steht sie unten so genau, dass
sie nach einem Verlust rekonstruierbar ist.

Was diese Prüfung monatlich von allein zu tun bekommt, steht unter
[Abhängigkeits-Aktualisierungen](#abhängigkeits-aktualisierungen).

## Der Workflow

[`.github/workflows/ci.yml`](../.github/workflows/ci.yml), ein Job namens **`Test-Gate`**:

```
Pull Request gegen main ─► Postgres-Service-Container hochfahren ─► ./mvnw -B verify
```

Der Job spiegelt das Test-Gate aus [`deploy-demo.yml`](../.github/workflows/deploy-demo.yml):
gleiches `postgres:17`, gleiche Verbindungswerte (`elternsprechtag` / `myuser` / `mysecret`
auf 5432), gleicher Healthcheck. Das ist Absicht — ein PR-Lauf soll dieselbe Umgebung sehen wie
der Lauf unmittelbar vor einem Deploy.

**Der Service-Container ist keine Bequemlichkeit, sondern Voraussetzung.** Nicht nur
`ElternsprechtagApplicationTests` startet den echten Spring-Context — auch die Service-Tests laufen
gegen eine echte Postgres. Sie sind über die Annotation `@ServiceTest` auf `Replace.NONE` gestellt,
bekommen also keine eingebettete Datenbank untergeschoben. Ohne erreichbare Postgres ist die Suite
rot, nicht etwa grün gegen einen Ersatz.

Getestet wird gegen eine **eigene Datenbank** `elternsprechtag_test` (Profil `test`, siehe
`src/test/resources/application-test.properties`), nicht gegen die der Anwendung — die
Service-Tests räumen vor jedem Test alle Tabellen ab, und das darf die Entwicklungsdatenbank nicht
treffen. Ein Service-Container kann kein Init-Skript aus dem Repository mounten, weil er vor dem
Checkout startet; deshalb legt ein eigener Workflow-Schritt die Datenbank an, bevor `mvn verify`
läuft. Lokal erledigt das `docker/init-test-db.sql` beim ersten Containerstart.

Das ist eine bewusste Entscheidung: Es soll genau eine Schema-Wahrheit geben, und
datenbankspezifisches Verhalten soll in Tests sichtbar werden statt erst in der Produktion. Der
Preis ist eine längere Testlaufzeit. Testcontainers wäre die Alternative gewesen und wurde
verworfen — der einzige Gewinn wäre ein Migrations-Kaltstart auch lokal, und den deckt dieser
Service-Container ab, der bei jedem Lauf frisch ist.

Lokal gilt dieselbe Voraussetzung; dort bedient sie der Container aus
[`compose.yaml`](../compose.yaml) (siehe [README](../README.md#lokal-starten)).

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

## Abhängigkeits-Aktualisierungen

[`.github/dependabot.yml`](../.github/dependabot.yml) lässt Dependabot **monatlich** nach
Aktualisierungen suchen — für die Maven-Abhängigkeiten der Anwendung und für die im
Repository verwendeten GitHub-Actions. Beide Ökosysteme bekommen je einen gebündelten Pull
Request (`groups`) statt eines pro Abhängigkeit: ein Maven-Build dauert hier Minuten, und
Spring-Boot- wie Vaadin-Artefakte wandern ohnehin im Verbund.

Der Weg über einen Pull Request ist der Punkt. Jede Aktualisierung läuft durch das
**`Test-Gate`** oben, und der Maintainer entscheidet am Prüfergebnis, ob sie gefahrlos
gemerged werden kann — Sicherheitsaktualisierungen bleiben so nicht liegen, ohne dass etwas
Ungetestetes nach `main` kommt. Ein Dependabot-Pull-Request bekommt ein eingeschränktes
Token; das genügt, weil der Job keine Secrets braucht und auf `contents: read` steht.

Warum auch GitHub-Actions: sonst würde ausgerechnet die Pipeline, die alles andere prüft,
selbst nicht aktuell gehalten. `directory: /` ist bei diesem Ökosystem richtig — Dependabot
sucht die Workflows selbst unter `.github/workflows/`, das Verzeichnis wird nicht angegeben.

**Gültigkeit prüfen**: GitHub liest die Datei nur vom Default-Branch. Nach dem Merge zeigt
*Insights → Dependency graph → Dependabot* beide Ökosysteme mit ihrem letzten Prüfzeitpunkt;
ein Parse-Fehler stünde dort statt der Einträge. Einen Lauf sofort auslösen (statt bis zum
Monatstermin zu warten) geht dort über *Check for updates*.
