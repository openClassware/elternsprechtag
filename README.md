# Elternsprechtag

Terminbuchung für den Elternsprechtag einer Schule: Das Sekretariat veröffentlicht den Sprechtag,
Eltern buchen ohne Anmeldung über einen Link, jede Lehrkraft bekommt ihren Terminplan.

> **Benutzen oder betreiben?** Die Anleitung für Sekretariat, Lehrkräfte, Schulleitung und Schul-IT
> — samt Demo, Installation und Datenschutz — steht unter **<https://docs.openclassware.de>**.
> Diese README richtet sich an Entwickler.

> [!WARNING]
> **Version 0.x — noch nicht für den Echtbetrieb an einer Schule freigegeben.**
> Die Anwendung läuft und ist vollständig bedienbar, aber es fehlen Dinge, die ein produktiver
> Einsatz braucht:
> - **Stammdaten sind nur per SQL pflegbar.** Lehrkräfte, Klassen, Fächer und Lehraufträge werden
>   direkt in der Datenbank angelegt — es gibt weder Import noch Pflegemasken.
>
> Zum Ausprobieren, Bewerten und Mitentwickeln ist das Projekt gedacht — für den Sprechtag im
> nächsten Monat noch nicht.

Technisch ist es eine Spring-Boot-Anwendung mit Vaadin-Oberfläche und PostgreSQL-Datenbank,
konfiguriert über Umgebungsvariablen und als Docker-Image ausrollbar.

## Lokal starten

Voraussetzungen: **JDK 25** und **Docker** (für die Datenbank).

```bash
git clone https://github.com/openClassware/elternsprechtag.git
cd elternsprechtag
ELTERNSPRECHTAG_OEFFENTLICHE_URL=http://localhost:8080 ./mvnw spring-boot:run
```

Die Anwendung startet die PostgreSQL-Datenbank aus [`compose.yaml`](compose.yaml) selbst und ist
danach unter <http://localhost:8080> erreichbar. Die öffentliche Adresse ist der einzige Wert ohne
Default: Aus ihr entsteht der Elternlink, und ohne sie startet die Anwendung nicht. Für alles andere
gelten ohne gesetzte Umgebungsvariablen die Entwicklungs-Defaults aus
[`application.properties`](src/main/resources/application.properties), inklusive eines für jeden
nachlesbaren Organizer-Zugangs — für eine erreichbare Instanz müssen Datenbankverbindung und
Organizer-Zugang gesetzt werden (siehe [Konfiguration](https://docs.openclassware.de/betrieb/konfiguration/)).

Eine frische Datenbank ist leer. Mit dem `demo`-Profil kommen Beispiel-Stammdaten (Lehrkräfte,
Klassen, Fächer, Lehraufträge) dazu:

```bash
SPRING_PROFILES_ACTIVE=demo ELTERNSPRECHTAG_OEFFENTLICHE_URL=http://localhost:8080 ./mvnw spring-boot:run
```

### Datenbankschema

Das Schema gehört [Flyway](https://flywaydb.org/): Die Skripte liegen unter
[`src/main/resources/db/migration`](src/main/resources/db/migration) und laufen beim Start in
jedem Profil, auch in Tests. Sie sind die einzige Beschreibung des Schemas: Spring Data JDBC
erzeugt und prüft nichts. Ob Persistenzmodell und Migration zusammenpassen, zeigen die
Persistenz-Tests (`@DataJdbcTest` gegen dieselbe migrierte Datenbank).

Das `demo`-Profil nimmt zusätzlich [`db/demo`](src/main/resources/db/demo) in die Suchpfade auf;
dort liegen die Demo-Daten (Stammdaten und Beispiel-Sprechtage) als wiederholbare Migration. Eine Schulinstanz aktiviert dieses
Profil nicht und bekommt sie deshalb nie.

Jede Schemaänderung ist damit ein neues, versioniertes Skript (`V2__…sql`, `V3__…sql`); bereits
ausgelieferte Skripte werden nicht mehr geändert, Flyway prüft ihre Prüfsummen.

Wer die Entwicklungsdatenbank noch aus der Zeit vor Flyway hat, muss sie einmalig leeren — Flyway
verweigert die Arbeit an einem gefüllten Schema ohne Historientabelle:

```bash
docker exec elternsprechtag-database-1 psql -U myuser -d elternsprechtag \
  -c 'DROP SCHEMA public CASCADE' -c 'CREATE SCHEMA public'
```

Tests laufen mit:

```bash
docker compose up -d   # falls die Datenbank nicht ohnehin läuft
./mvnw verify
```

**Die Testsuite braucht eine laufende Datenbank.** Die Tests laufen bewusst gegen PostgreSQL und
nicht gegen eine untergeschobene In-Memory-Datenbank — nur so wird datenbankspezifisches Verhalten
in Tests überhaupt sichtbar.

Sie benutzen dabei eine **eigene Datenbank** (`elternsprechtag_test`), nicht die der Anwendung.
Das ist kein Detail: Die Service-Tests räumen vor jedem Test alle Tabellen ab. Liefen sie gegen
`elternsprechtag`, würde jedes `./mvnw verify` die Daten leeren, mit denen gerade entwickelt wird —
lautlos und auch bei laufender Anwendung. Angelegt wird sie von
[`docker/init-test-db.sql`](docker/init-test-db.sql) beim **ersten** Start des Containers. Wer den
Container schon länger laufen hat, legt sie einmalig von Hand an:

```bash
docker exec elternsprechtag-database-1 \
  psql -U myuser -d elternsprechtag -c 'CREATE DATABASE elternsprechtag_test'
```

Anders als beim Anwendungsstart fährt der Testlauf die Datenbank **nicht** selbst hoch: Spring
Boots Docker-Compose-Unterstützung hängt am Start der Anwendung, nicht am Start der Tests. In der
Praxis fällt das selten auf, weil der Container aus [`compose.yaml`](compose.yaml) mit
`restart: always` läuft, sobald er einmal gestartet wurde. Wer ihn gestoppt hat, bekommt rote
Tests mit Verbindungsfehler — dann hilft die Zeile oben.

Auch die Test-Datenbank füllt Flyway. Sie bleibt zwischen zwei Läufen bestehen, im CI ist sie bei
jedem Lauf frisch — wer sie noch aus der Zeit vor Flyway hat oder eine Migration lokal ändert,
während sie schon eingespielt war, bekommt hier rote Tests, die im CI grün sind. Dann hilft ein
Neuanlegen:

```bash
docker exec elternsprechtag-database-1 psql -U myuser -d elternsprechtag \
  -c 'DROP DATABASE elternsprechtag_test' -c 'CREATE DATABASE elternsprechtag_test'
```

## Dokumentation

| Dokument                                             | Inhalt                                                          |
|------------------------------------------------------|-----------------------------------------------------------------|
| [Doku-Site](https://docs.openclassware.de)            | Anwender-Doku: Einstieg mit Demo, Anwendungsfälle, Betrieb, Datenschutz, Referenz; Schreibanleitung in [`site/README.md`](site/README.md) |
| [Deploy](docs/deploy.md)                              | Wie die öffentliche Demo betrieben wird                          |
| [CI](docs/ci.md)                                      | Was auf dem Weg nach `main` geprüft wird                         |
| [Context Map](CONTEXT-MAP.md)                         | Die zwei Kontexte und ihre Grenze; von dort je ein Glossar          |
| [Architektur](docs/arc/ARCHITECTURE.md)               | Schichtung, Auth-Modell, Entscheidungen                          |
| [Abdeckung](docs/arc/ABDECKUNG.md)                    | Was das Produkt jenseits des Happy Path abdecken muss — und was bewusst nicht |

## Lizenz

[Apache-2.0](LICENSE) — frei nutzbar, veränderbar und weitergebbar, auch kommerziell.
