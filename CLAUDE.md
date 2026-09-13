# CLAUDE.md

Kurzregeln für die Arbeit an diesem Projekt. Architektur-Details und Begründungen stehen in
[`docs/arc/ARCHITECTURE.md`](docs/arc/ARCHITECTURE.md).

**Bevor ein Feature als fertig gilt:** [`docs/arc/ABDECKUNG.md`](docs/arc/ABDECKUNG.md) ist der
Maßstab für die Fälle jenseits des Happy Path. Es stuft je Phase des Schulablaufs ein, was das
Produkt abdecken **muss**, was **fehlen darf** und was **bewusst nein** ist — Letzteres nicht ohne
ADR ändern.

## Zwei Kontexte, eine Struktur

Die Codebasis ist **hexagonal geschnitten**, mit DDD-Aggregaten und zwei Bounded Contexts
([ADR 0003](docs/adr/0003-hexagonale-architektur-mit-zwei-kontexten.md),
[0004](docs/adr/0004-spring-data-jdbc-statt-jpa.md),
[0005](docs/adr/0005-eltern-submit-bricht-eine-transaktion-ein-aggregat.md)). Der Umbau ist
abgeschlossen — es gibt **keine zweite, alte Struktur** und **keine JPA**.

```
sprechtag/          domain | application/{port/in,port/out,service} | adapter/{in/web,out/{persistence,mail,event,schulorganisation}}
schulorganisation/  domain | application | adapter        (ohne in/web)
config/, security/  Verdrahtung, sonst nichts
```

Alles Fachliche gehört in einen der beiden Kontexte. Die Grenze zwischen ihnen steht in
[`CONTEXT-MAP.md`](CONTEXT-MAP.md).

## Architektur & Schichten

- Die **Domäne importiert nur JDK** — kein Spring, kein Lombok, keine Persistenz, kein Vaadin.
  Aggregate referenzieren einander ausschließlich über **typisierte IDs**.
- **In einen fremden Kontext führt genau ein Weg: sein `port/in`.** Der Sprechtag liest die
  Schulorganisation, nie umgekehrt; geteilte Typen gibt es über die Grenze nicht — dort gehen
  `UUID`s und Text.
- Presenter rufen **ausschließlich Use-Case-Ports** (`port/in`), auch für Abfragen. Die Records
  liegen dort, nicht im Service — `Buchen.BuchungsAnfrage`, `Auswerten.SprechtagAuswertung`.
- **Kein View und keine UI-Komponente hält je ein Aggregat** — über die Presenter-Grenze gehen
  ausschließlich Records.
- Views sind **so dumm wie möglich**: Anzeige- und Entscheidungslogik liegt im Presenter,
  nicht im View. Presenter sind **zustandslos** (Singletons) — Per-View-Zustand bleibt im View.
- **Komplexe UI-Entscheidungslogik** (mit Zustand) gehört in ein **Vaadin-freies Modell**
  (z. B. `BookingSession`), das der View hält — so bleibt sie per plain JUnit testbar.
- **Zwei Wege in die Datenbank**: Aggregat-Repository zum Schreiben, Query-Port mit
  handgeschriebenem SQL zum Lesen. **Read-Modelle dürfen veraltet sein**; jede Entscheidung darauf
  wird beim Schreiben am Aggregat erneut geprüft.
- **Kein SQL joint über die Kontextgrenze.** Zwei Ports liefern ihre Teile, der Use Case fügt sie
  in Java zusammen.
- Das **Aggregat meldet** Ereignisse, der **Use Case bündelt und veröffentlicht** sie über den
  Ereignis-Port.
- Ein Aggregat ist nach `Termine.speichere` **verbraucht** — wer weiterarbeitet, lädt neu.
- **Ein Adapter kennt keinen zweiten.** Was sich zwei teilen, liegt eine Ebene höher
  (`sprechtag/adapter/Formats`).
- **Sichtbarkeit**: Use-Case-Services, Outbound-Adapter, Mapper, Persistenzmodelle und Presenter
  sind **package-private** — nach außen gilt der Port. Öffentlich sind Ports, Domäne und
  View-Klassen (Vaadin-Route); deren Konstruktoren wieder package-private.
- **Sprache**: Fachsprache deutsch (`Termin.buche(...)`, `Lehrauftrag`), Architekturvokabular
  englisch (`port/in`, `adapter`, `domain`, `application`).
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

- Drei Stellen, jede mit eigenem Zweck: **Aggregat-Logik ⇒ plain JUnit** (ohne Spring, ohne
  Datenbank), **Persistenz-Adapter ⇒ `@DataJdbcTest`**, **Use Case ⇒ `@ServiceTest`** (gegen eine
  echte Postgres). Eine Regel, die ins Aggregat gehört, wird nicht auf Service-Ebene getestet.
- Views bleiben dumm und testfrei.
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
