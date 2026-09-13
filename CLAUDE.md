# CLAUDE.md

Kurzregeln für die Arbeit an diesem Projekt. Architektur-Details, Begründungen und
das Findings-Backlog stehen in [`docs/arc/ARCHITECTURE.md`](docs/arc/ARCHITECTURE.md).

**Bevor ein Feature als fertig gilt:** [`docs/arc/ABDECKUNG.md`](docs/arc/ABDECKUNG.md) ist der
Maßstab für die Fälle jenseits des Happy Path. Es stuft je Phase des Schulablaufs ein, was das
Produkt abdecken **muss**, was **fehlen darf** und was **bewusst nein** ist — Letzteres nicht ohne
ADR ändern.

## Der Umbau läuft — zwei Welten gelten gleichzeitig

Die Codebasis stellt auf eine **hexagonale Architektur mit DDD-Aggregaten** um
([ADR 0003](docs/adr/0003-hexagonale-architektur-mit-zwei-kontexten.md),
[0004](docs/adr/0004-spring-data-jdbc-statt-jpa.md),
[0005](docs/adr/0005-eltern-submit-bricht-eine-transaktion-ein-aggregat.md)). Prüfe zuerst, in
welcher Hälfte du arbeitest — die Regeln unterscheiden sich:

- **`sprechtag.{domain,application,adapter}`** und **`schulorganisation.{domain,application,adapter}`**
  — migriert (Scheiben 1–3: `Termin` mit `Buchung`, `Sprechtag`, und die Stammdaten als eigener
  Kontext). Hier gelten die ADRs; Spring Data JDBC, Aggregate ohne Framework-Annotationen, Ports
  statt Repositories.
- **`services`, `ui`** — Bestand (Versand, gesamte Oberfläche). Hier gelten die Schichtenregeln
  unten weiter.

Neue fachliche Arbeit gehört in die migrierte Hälfte. **Neue JPA-Entities kommen nicht mehr dazu**
— es gibt keine mehr.

Die beiden Kontexte und ihre Grenze stehen in [`CONTEXT-MAP.md`](CONTEXT-MAP.md).

## Architektur & Schichten (Bestand)

- Strikte Schichtung: **Domain (JPA) → Repository → Service → Presenter → View**.
- JPA-`@Entity` bleiben in Persistenz/Service. **Kein View und keine UI-Component hält
  oder empfängt je eine Entity** — über die Presenter-Grenze gehen ausschließlich
  Records/DTOs.
- Entity→Record-Mapping passiert **ausschließlich im Service**; der Presenter reicht nur
  durch. Records liegen verschachtelt im erzeugenden Service (`XxxService.YyyOption`).
- Views sind **so dumm wie möglich**: Anzeige- und Entscheidungslogik liegt im Presenter,
  nicht im View. Presenter sind **zustandslos** (Singletons) — Per-View-Zustand bleibt im View.
- **Komplexe UI-Entscheidungslogik** (mit Zustand) gehört in ein **Vaadin-freies Modell**
  (z. B. `BookingSession`), das der View hält — so bleibt sie per plain JUnit testbar.
- Views/Components rufen **nie** ein Repository direkt.
- **Sichtbarkeit**: Presenter sind package-private; View-Klassen `public` (Vaadin-Route), ihre
  Konstruktoren aber package-private.

## Architektur & Schichten (migrierte Kontexte)

- Die **Domäne importiert nur JDK** — kein Spring, kein Lombok, kein JPA. Aggregate referenzieren
  einander ausschließlich über **typisierte IDs**.
- **In einen fremden Kontext führt genau ein Weg: sein `port/in`.** Der Sprechtag liest die
  Schulorganisation, nie umgekehrt; geteilte Typen gibt es über die Grenze nicht — dort gehen
  `UUID`s und Text.
- Presenter rufen **ausschließlich Use-Case-Ports** (`port/in`), auch für Abfragen. Die Records
  liegen dort, nicht im Service — `Buchen.BuchungsAnfrage`, `Auswerten.SprechtagAuswertung`.
- **Zwei Wege in die Datenbank**: Aggregat-Repository zum Schreiben, Query-Port mit
  handgeschriebenem SQL zum Lesen. **Read-Modelle dürfen veraltet sein**; jede Entscheidung darauf
  wird beim Schreiben am Aggregat erneut geprüft.
- **Kein SQL joint über die Kontextgrenze.** Zwei Ports liefern ihre Teile, der Use Case fügt sie
  in Java zusammen.
- Das **Aggregat meldet** Ereignisse, der **Use Case bündelt und veröffentlicht** sie über den
  Ereignis-Port.
- Ein Aggregat ist nach `Termine.speichere` **verbraucht** — wer weiterarbeitet, lädt neu.
- Diese Regeln stehen als **ArchUnit-Test** (`ArchitekturTest`) im Build. Wer sie ändert, ändert
  dort mit.

## Auth

- Genau **eine Organizer-Identität** (in-memory, aus `application.properties`). Eltern
  greifen **anonym per Access-Token-Link** zu (`Sprechtag.accessToken`). Kein Lehrer-Login,
  keine DB-Accounts — das ist bewusst so. Erst ändern, wenn ein Feature es zwingend erfordert.

## i18n

- **Deutsch-only ist Absicht.** Alle UI-Texte über `getTranslation(...)` +
  `vaadin-i18n/translations.properties` — nie Strings direkt im Code.
- Datum/Zeit über eine **zentrale Formatter-Stelle**, nicht inline dupliziert.

## Tests

- Geschäftslogik lebt im Service und braucht Tests. Neue oder geänderte Service-Logik ⇒
  Test (`@DataJpaTest` / `@SpringBootTest`). Views bleiben dumm und testfrei.
- Im migrierten Kontext teilt sich das auf: **Aggregat-Logik ⇒ plain JUnit** (ohne Spring, ohne
  Datenbank), **Persistenz-Adapter ⇒ `@DataJdbcTest`**, **Use Case ⇒ Service-Test**. Eine Regel,
  die ins Aggregat gehört, wird nicht auf Service-Ebene getestet.
- Ausnahme: Vaadin-freie UI-Modelle (z. B. `BookingSession`) tragen Entscheidungslogik und
  bekommen **plain-JUnit-Tests** ohne Spring-Kontext.

## Views

- Alle Views sollen responsive sein (Mobile, Tablet, Desktop).
- **Untergrenze ist 375 × 667 px** (iPhone SE 2./3. Generation). 320 px ist ausdrücklich
  **kein** Ziel.
- Genau **zwei Schwellen**, desktop-first und `max-width`-basiert: **1024 px** (Tablet) und
  **640 px** (Telefon). Keine weiteren Breakpoints erfinden.
- **Mobile-Layouts entstehen in CSS, nicht in Java**: keine Viewport-Abfrage in einem View,
  kein `BrowserWindowResizeListener`, keine bildschirmabhängige Komponentenauswahl.

## CSS

- CSS wird in [BEM-Notation](https://getbem.com/) geschrieben (Block, Element, Modifier).
  - Block: `card`
  - Element: `card__title`
  - Modifier: `card--highlighted`
- **Tabellen-Karten-Muster**: Unterhalb von 640 px wird die Kopfzeile ausgeblendet und die
  Zeile zur gestapelten Karte — die erste Spalte bleibt fest als Scan-Anker, der Rest stapelt
  daneben. Vorlage ist die Auswertungsansicht (`styles/auswertung-view.css`).

## Agent skills

### Issue tracker

Issues und Specs leben als GitHub Issues (`gh` CLI, Repo `openClassware/elternsprechtag`). See `docs/agents/issue-tracker.md`.

### Triage labels

Die fünf kanonischen Standard-Rollen, unverändert. See `docs/agents/triage-labels.md`.

### Domain docs

Zwei Kontexte: `CONTEXT-MAP.md` im Repo-Root verweist auf je ein `CONTEXT.md` unter `docs/contexts/`; ADRs gemeinsam in `docs/adr/`. See `docs/agents/domain.md`.
