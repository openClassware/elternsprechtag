# Veröffentlichen — App- und Doku-Image

Dieses Repo veröffentlicht zwei Images in die GitHub Container Registry (GHCR) und sonst nichts.
Wo und wie sie laufen, weiß es nicht: Ausrollen, Server, Domains und der Betrieb der öffentlichen
Demo liegen beim jeweiligen Betreiber, nicht hier (#232).

| Image                                         | Inhalt                         | Workflow                                              |
|-----------------------------------------------|--------------------------------|-------------------------------------------------------|
| `ghcr.io/openclassware/elternsprechtag`       | die Anwendung                  | [`image.yml`](../.github/workflows/image.yml)         |
| `ghcr.io/openclassware/elternsprechtag-docs`  | die gebaute Anwender-Doku      | [`docs.yml`](../.github/workflows/docs.yml)           |

Beide laufen bei jedem Push auf `main` und per `workflow_dispatch`, und beide tragen dieselben
Tags:

| Tag       | Stand                                          |
|-----------|------------------------------------------------|
| `:latest` | letzter Push auf `main`                        |
| `:<sha>`  | genau dieser Commit auf `main`, unveränderlich |

So gibt es zu jedem Commit App und Doku im selben Stand, und ein Betreiber kann per SHA-Tag gezielt
zurückrollen. Kommen mehrere Pushes während eines Laufs, baut GitHub nur den letzten nach — die
Commits dazwischen bekommen keinen eigenen Tag. Ein Lauf per `workflow_dispatch` außerhalb von
`main` baut und prüft, pusht aber kein Image.

Wie eine Schule die Anwendung betreibt und welche Werte sie setzt, steht auf der Doku-Site unter
[Betrieb › Installation](https://docs.openclassware.de/betrieb/installation/) und
[Betrieb › Konfiguration](https://docs.openclassware.de/betrieb/konfiguration/). Was auf dem Weg
**nach** `main` passiert (PR-Workflow, Branch Protection), steht in [`ci.md`](ci.md).

## App-Image

```
Push auf main ─► Test-Gate (mvn verify) ─► Image bauen ─► GHCR (:latest, :<sha>)
```

Ein rotes Test-Gate bricht ab, bevor irgendetwas gepusht wird. Das Gate ist eine Spiegelung des
Jobs `Test-Gate` aus [`ci.yml`](../.github/workflows/ci.yml): gleicher Postgres-Service-Container,
gleiche Verbindungswerte. Der teure Vaadin-Production-Build läuft auf dem GitHub-Runner; wer das
Image betreibt, zieht nur das fertige Ergebnis. Gebaut wird es aus dem
[`Dockerfile`](../Dockerfile) im Repo-Wurzelverzeichnis.

Das Schreibrecht auf GHCR liegt in einem eigenen Job, nicht in dem, der Maven und die Tests
ausführt. Secrets braucht der Workflow keine: Den Push erledigt das automatische `GITHUB_TOKEN`.

### Profil `demo`

Das Image enthält das Profil `demo`
([`application-demo.properties`](../src/main/resources/application-demo.properties)). Es ist Teil
des Produkts, nicht der öffentlichen Demo: Wer es einschaltet, bekommt erfundene Stammdaten und
Beispiel-Sprechtage aus der wiederholbaren Migration `db/demo/R__demo_stammdaten.sql`, deren Daten
relativ zum Starttag stehen. Das Schema baut auch dort Flyway über dieselbe Migrationskette wie in
einer Schulinstanz. Soll eine Demo täglich frisch aussehen, leert ihr Betreiber die Datenbank vor
dem Start; das Profil selbst setzt nichts zurück.

## Doku-Site

Die Anwender-Doku aus [`site/`](../site) steht unter <https://docs.openclassware.de>; ausgeliefert
wird sie vom Hosting. [`docs.yml`](../.github/workflows/docs.yml) startet vor dem Build die App mit
Profil `demo` und erzeugt daraus die Screenshots — im Repo liegt kein Bild der Site. Danach baut er
die Site und schiebt sie als Doku-Image nach GHCR.

Das Image ([`site/Dockerfile`](../site/Dockerfile)) ist `FROM scratch` und enthält nur die
statischen Dateien aus `site/dist` unter **`/site`** — `/site/index.html` ist die Startseite. Es
gibt darin weder Webserver noch Shell; es ist ein Transportbehälter. Herausholen:

```bash
docker create --name doku ghcr.io/openclassware/elternsprechtag-docs:latest
docker cp doku:/site/. ./doku
docker rm doku
```

Das Kommando im Image (`/site/index.html`) ist ein Platzhalter, ohne den `docker create` das Image
verweigert; ausgeführt wird es nie.

## Einmalig von Hand

Ein neu angelegtes GHCR-Paket ist privat. Soll ein Betreiber ein Image ohne Anmeldung ziehen, unter
*Packages → \<Paket\> → Package settings → Change visibility* auf **Public** stellen.
