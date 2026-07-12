# Architektur — Elternsprechtag

Lebende Architektur-Doku. Beschreibt den **beabsichtigten** Zustand und die geltenden
Regeln. Kurzfassung der harten Regeln: [`/CLAUDE.md`](../../CLAUDE.md). Bekannte Abweichungen
vom Soll stehen im [Findings-Backlog](#findings-backlog) am Ende.

Ergänzend: [`domain.puml`](domain.puml) (Domänen-Klassendiagramm).

## Überblick

Web-App zur Buchung von Elternsprechtag-Terminen an einer Schule.

- **Organizer** legen Sprechtage an, veröffentlichen sie und verwalten ihren Status.
- **Eltern** buchen über einen anonymen Access-Token-Link Termine bei Lehrkräften.

Stack: **Spring Boot + Vaadin (Flow)**, JPA/Hibernate auf **PostgreSQL**, Lombok, BEM-CSS.

## Schichten & Datenfluss

Strikte, gerichtete Schichtung — jede Schicht kennt nur die direkt darunter:

```
View / Component   (Vaadin, BEM-CSS)   — nur Records/DTOs, keine Entities, "dumm"
      │
Presenter          (@Component)        — dünne UI-Grenze, reicht durch
      │
Service            (@Service, @Transactional) — Geschäftslogik, Entity→Record-Mapping
      │
Repository         (Spring Data)       — Datenzugriff
      │
Domain / @Entity   (JPA)              — Persistenzmodell
```

**Regeln:**

- Views/Components rufen **nie** ein Repository oder eine Service-Methode direkt am
  Presenter vorbei auf.
- **JPA-Entities verlassen die Service-Schicht nicht.** Über die Presenter-Grenze gehen
  ausschließlich Records/DTOs. (Begründung: `spring.jpa.open-in-view=false` — eine Entity
  im View ist detached, jeder Lazy-Zugriff wirft `LazyInitializationException`.)
- **Entity→Record-Mapping ausschließlich im Service** (dort ist die Transaktion offen und
  Lazy-Zugriffe sind sicher). Der Presenter delegiert nur.
- Records liegen **verschachtelt im erzeugenden Service** (`BuchungService.LehrkraftOption`,
  `BuchungService.SlotOption`, …) — Definition und Mapping bleiben beieinander.

## UI-Architektur (MVP)

- Jeder View hat einen Presenter (`@Component`), per Konstruktor injiziert.
- **Der View ist so dumm wie möglich.** Anzeige- und Entscheidungslogik (Filter, „leer? →
  welche Komponente", Aufbereitung) gehört in den Presenter; der View rendert nur das vom
  Presenter gelieferte View-Model.
- UI-Texte kommen über `getTranslation(...)`, nicht als String-Literal.

## Domänenmodell

Kern-Kette: **Sprechtag → Termin → Buchung**, mit **Lehrauftrag** als Buchungsziel.

- **Sprechtag** — Ein Sprechtag-Event: Datum, Zeitfenster, Slot-Dauer, Status, teilnehmende
  Klassen, Access-Token für den Eltern-Link.
- **Lehrauftrag** — Verknüpft (Lehrer × Klasse × Fach). Das fachlich-organisatorische
  Ziel einer Buchung.
- **Termin** — Ein **materialisierter** Zeit-Slot einer Lehrkraft an einem Sprechtag
  (`FREI`/`BELEGT`/gesperrt). Wird beim Veröffentlichen erzeugt (siehe unten).
- **Buchung** — Eine Eltern-Buchung eines `Termin` gegen einen `Lehrauftrag`
  (`ZUGESAGT`/storniert), mit Eltern-/Schülername und optionaler Notiz.

### Slot-Materialisierung

Beim Wechsel auf `VEROEFFENTLICHT` erzeugt `SprechtagService` für **jede teilnehmende
Lehrkraft × jeden Zeit-Slot** einen freien `Termin`. Der Existenz-Check macht das
idempotent (kein Doppel-Erzeugen bei erneutem Speichern). Teilnehmende Lehrkräfte werden aus
den Lehraufträgen der Sprechtag-Klassen abgeleitet.

## Nebenläufigkeit & Persistenz

Dieser Bereich gilt als solide und ist **bewusst** so gebaut:

- **`spring.jpa.open-in-view=false`** — Sessions enden mit der Service-Transaktion; deshalb
  die DTO-Grenze (s. o.).
- **Optimistisches Locking** über `@Version` auf `Termin`.
- **Atomare Buchung** (`BuchungService.buchen`): Ein Eltern-Submit mit N Wünschen ist
  „alles oder nichts". Ist auch nur ein Slot belegt, rollt die ganze Transaktion zurück
  (`TerminBelegtException`). `saveAndFlush` erzwingt den Lock-Konflikt früh (im try-Block),
  parallele Doppelbuchung eines Slots wird als Konflikt behandelt.

> Dies ist das Kronjuwel der App und aktuell **ungetestet** — siehe Findings.

## Auth

**Bewusst minimal:**

- **Eine Organizer-Identität**, in-memory (`InMemoryUserDetailsManager`), Credentials aus
  `application.properties` (`elternsprechtag.security.organizer.*`, bcrypt). Rolle:
  `Roles.ORGANIZER`.
- **Eltern**: kein Login. Zugang anonym (`@AnonymousAllowed`) über den
  `Sprechtag.accessToken`-Link.
- **Kein Lehrer-Login, keine DB-Accounts, kein Self-Service.** Es gibt **keine
  `Account`-Entity** — das ist Absicht, nicht eine Lücke.

Ändern erst, wenn ein konkretes Feature (Multi-Organizer, Lehrer-Login) es erzwingt — dann
als eigene, saubere Entscheidung.

## i18n

- **Deutsch-only ist Absicht.** i18n dient hier der **Externalisierung** von Texten, nicht
  der Mehrsprachigkeit.
- Alle UI-Texte über `getTranslation(...)` → ein Bundle `vaadin-i18n/translations.properties`.
  Keine String-Literale im Code.
- Datum/Zeit-Formatierung läuft ausschließlich über **`ui.Formats`** (`time`, `dateLong`,
  `monthShort`) — nicht inline duplizieren. Ein späterer Sprachwechsel bliebe dadurch billig,
  ist aber nicht geplant.

## Tests

- **Service-Layer-Tests sind das Rückgrat.** Neue oder geänderte Geschäftslogik im Service ⇒
  Test (`@DataJpaTest` / `@SpringBootTest`).
- Priorität: `BuchungService` (Buchungs-Atomarität, `TerminBelegtException`, optimistisches
  Locking) und `SprechtagService` (Materialisierung, Status-Übergänge).
- Views bleiben dumm → kein Vaadin-E2E, keine View-Tests nötig.

## Konventionen

- **CSS**: BEM (Block / Element / Modifier). Details in `/CLAUDE.md`.
- **Views**: responsive (Mobile, Tablet, Desktop).
- **Sichtbarkeit**: Presenter/Views wo möglich package-private halten (Kapselung).

---

## Findings-Backlog

Beim Architektur-Review (2026-07-12) gefundene Abweichungen vom Soll. Strukturelle Punkte
sind **noch nicht umgesetzt** — pro Punkt einzeln priorisieren.

| # | Fund | Schwere | Status |
|---|------|---------|--------|
| F1 | Views/Components halten/empfangen JPA-Entities statt Records | strukturell | **erledigt für Read-Path + Edit-Formular** (siehe unten); Rest: `ElternsprechtagView` (Eltern-Buchungsflow) offen |
| F2 | ~~Live-Bug LazyInitializationException~~ **Kein Bug**: `SprechtagRepository` lädt `klassen` in allen Lesepfaden per `@EntityGraph` eager — Zugriff abgesichert. Für den Read-Path durch F1 jetzt ohnehin obsolet. | hinfällig | geschlossen |
| F3 | Datum/Zeit-Formatter (`Locale.GERMANY`, `"HH:mm"`) an mehreren Stellen dupliziert | mittel | **erledigt**: zentrale `ui.Formats` (`time`/`dateLong`/`monthShort`) |
| F4 | Geschäftslogik in Services komplett ungetestet | strukturell | **erledigt**: `BuchungServiceTest` (6) + `SprechtagServiceTest` (9) via `@DataJpaTest` |
| F5 | Presenter ist dünne Delegation, während der View Logik trägt — gegen „dummer View" | strukturell | offen (v. a. `OrganizerView`: isEmpty→Komponente, Card-Aufbau) |
| F6 | Uneinheitliche Sichtbarkeit (public vs. package-private) bei Presentern/Views | kosmetisch | offen |

**Erledigt:**

- **F1 (Read-Path + Edit-Formular):** Neue Records `SprechtagService.SprechtagRow` (Übersicht/
  Verwaltung), `SprechtagService.SprechtagForm` (Anlegen/Bearbeiten) und
  `KlassenService.KlasseOption`. Übersicht, Manage-Tabelle, Cards, Filter und das Edit-Formular
  (`Binder<SprechtagForm>` statt `Binder<Sprechtag>`, Klassenauswahl über `KlasseOption`) halten
  keine JPA-Entities mehr. Mapping ausschließlich in den Services. **Offen bleibt** der
  Eltern-Buchungsflow (`ElternsprechtagView` hält noch `Sprechtag`/`Klasse` — durch `@EntityGraph`
  abgesichert).
- **F4 (Service-Tests):** `BuchungServiceTest` und `SprechtagServiceTest` (`@DataJpaTest` + H2,
  Services via `@Import`, `@Transactional(NOT_SUPPORTED)` für echte Transaktionsgrenzen). Deckt u. a.
  Buchungs-Atomarität (Alles-oder-nichts-Rollback), belegte Slots, Lehrer-Mismatch, Slot-
  Materialisierung inkl. Rand-Slot, Status-Übergänge, Idempotenz und Duplizieren ab.
- **Diagramm-Drift (trivial):** nicht existierende `Account`-Entity aus `domain.puml` entfernt
  (Auth ist bewusst in-memory, ohne DB-Account).
