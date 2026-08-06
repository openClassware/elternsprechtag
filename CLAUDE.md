# CLAUDE.md

Kurzregeln für die Arbeit an diesem Projekt. Architektur-Details, Begründungen und
das Findings-Backlog stehen in [`docs/arc/ARCHITECTURE.md`](docs/arc/ARCHITECTURE.md).

## Architektur & Schichten

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
- Ausnahme: Vaadin-freie UI-Modelle (z. B. `BookingSession`) tragen Entscheidungslogik und
  bekommen **plain-JUnit-Tests** ohne Spring-Kontext.

## Views

- Alle Views sollen responsive sein (Mobile, Tablet, Desktop).

## CSS

- CSS wird in [BEM-Notation](https://getbem.com/) geschrieben (Block, Element, Modifier).
  - Block: `card`
  - Element: `card__title`
  - Modifier: `card--highlighted`

## Agent skills

### Issue tracker

Issues und Specs leben als GitHub Issues (`gh` CLI, Repo `openClassware/elternsprechtag`). See `docs/agents/issue-tracker.md`.

### Triage labels

Die fünf kanonischen Standard-Rollen, unverändert. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context (`CONTEXT.md` + `docs/adr/` im Repo-Root). See `docs/agents/domain.md`.
