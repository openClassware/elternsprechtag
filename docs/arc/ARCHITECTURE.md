# Architektur — Elternsprechtag

Lebende Architektur-Doku. Beschreibt den **beabsichtigten** Zustand und die geltenden
Regeln. Kurzfassung der harten Regeln: [`/CLAUDE.md`](../../CLAUDE.md). Bekannte Abweichungen
vom Soll stehen im [Findings-Backlog](#findings-backlog) am Ende.

Ergänzend: [`domain.puml`](domain.puml) (Domänen-Klassendiagramm) und
[`docs/deploy.md`](../deploy.md) (Demo-Umgebung: Secrets, DNS, Server-Vorbereitung).

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
- **Presenter sind zustandslos** (`@Component`-Singletons). Per-View-Zustand (Filterauswahl,
  Buchungs-„Warenkorb") lebt im View bzw. in einem vom View gehaltenen Modell — nicht im Presenter.
- **Komplexe UI-Entscheidungslogik gehört in ein Vaadin-freies Modell** (z. B. `BookingSession`
  für den Eltern-Buchungsflow: Slot-Zustand inkl. Zeitkonflikt, Aufräumen nach Konflikt,
  Anfrage-Bau). Bewusst ohne Vaadin-Abhängigkeit, damit die Logik per plain JUnit testbar ist —
  der View hält eine Instanz und rendert nur.
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
- Views bleiben dumm → kein Vaadin-E2E, keine View-Tests nötig. **Vaadin-freie UI-Modelle**
  (z. B. `BookingSession`) sind die Ausnahme: Sie tragen Entscheidungslogik und werden per plain
  JUnit getestet (`BookingSessionTest`) — ohne Spring-Kontext.

## Konventionen

- **CSS**: BEM (Block / Element / Modifier). Details in `/CLAUDE.md`.
- **Views**: responsive (Mobile, Tablet, Desktop).
- **Sichtbarkeit**: Presenter sind **package-private** (nur ihr View im selben Package nutzt sie).
  View-**Klassen** bleiben `public` (Vaadin-Route + paketübergreifende `X.ROUTE`/`X.class`-Referenzen),
  ihre **Konstruktoren** sind package-private (nur das Framework konstruiert sie).

## Findings-Backlog

Bekannte Abweichungen vom Soll und ihr Stand. Erledigte Findings bleiben als Changelog stehen.

| #  | Thema                                                        | Status                |
|----|-------------------------------------------------------------|-----------------------|
| F1 | Read-Modelle statt Entities über die Presenter-Grenze       | ✅ erledigt           |
| F2 | Lazy-Zugriff im View (`@EntityGraph` bereits vorhanden)      | ✅ kein Bug           |
| F3 | Datum/Zeit zentral über `ui.Formats`                        | ✅ erledigt           |
| F4 | Service-Tests (`BuchungService`, `SprechtagService`)        | ✅ erledigt           |
| F5 | `OrganizerView` dumm, Logik ins View-Model                  | ✅ erledigt           |
| F6 | Sichtbarkeit von Presentern/Views vereinheitlicht           | ✅ erledigt           |
| F7 | Filterlogik `ManageSprechtagView` → `…Presenter.filter(…)`  | ✅ erledigt           |
| F8 | Screen-Entscheidung Eltern-View → `…Presenter.pruefeZugang` | ✅ erledigt           |
| F9 | Buchungs-Warenkorb + Konfliktlogik → `BookingSession` (+Test)| ✅ erledigt           |
