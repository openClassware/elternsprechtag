# Architektur — Elternsprechtag

Lebende Architektur-Doku. Beschreibt den **beabsichtigten** Zustand und die geltenden
Regeln. Kurzfassung der harten Regeln: [`/CLAUDE.md`](../../CLAUDE.md). Bekannte Abweichungen
vom Soll stehen im [Findings-Backlog](#findings-backlog) am Ende.

Was das Produkt an Fällen **jenseits des Happy Path** abdecken muss, steht nicht hier, sondern in
[`ABDECKUNG.md`](ABDECKUNG.md) — dem Maßstab entlang des realen Schulablaufs (`muss` /
`darf fehlen` / `bewusst nein`). Diese Datei sagt, *wie* gebaut wird; jene, *was* abgedeckt sein
muss.

Ergänzend: [`domain.puml`](domain.puml) (Domänen-Klassendiagramm),
[`docs/deploy.md`](../deploy.md) (Demo-Umgebung: Secrets, DNS, Server-Vorbereitung) und
[`docs/ci.md`](../ci.md) (PR-Workflow und Branch Protection auf `main`).

## Überblick

Web-App zur Buchung von Elternsprechtag-Terminen an einer Schule.

- **Organizer** legen Sprechtage an, veröffentlichen sie und verwalten ihren Status.
- **Eltern** buchen über einen anonymen Access-Token-Link Termine bei Lehrkräften.

Stack: **Spring Boot + Vaadin (Flow)** auf **PostgreSQL** (Flyway), Lombok, BEM-CSS. Persistenz in
zwei Hälften: **Spring Data JDBC** im migrierten Sprechtag-Kontext, **JPA/Hibernate** im noch nicht
migrierten Bestand.

## Der Umbau läuft — lies das zuerst

Die Codebasis steht **mitten in der Umstellung** auf eine hexagonale Architektur mit zwei
Bounded Contexts und DDD-Aggregaten ([ADR 0003](../adr/0003-hexagonale-architektur-mit-zwei-kontexten.md),
[0004](../adr/0004-spring-data-jdbc-statt-jpa.md), [0005](../adr/0005-eltern-submit-bricht-eine-transaktion-ein-aggregat.md)).
Zwei Welten stehen deshalb nebeneinander, und das ist für die Dauer der Migration Absicht:

| | migriert (Scheiben 1–2, #138/#139) | Bestand |
|---|---|---|
| Paket | `sprechtag.{domain,application,adapter}` | `domain`, `repositories`, `services`, `ui` |
| Inhalt | `Sprechtag`, `Termin` mit `Buchung`; Anlegen/Bearbeiten/Veröffentlichen/Absagen/Abschließen/Duplizieren, Materialisieren, Buchen, Auswerten, Buchungsoptionen | Stammdaten (Klasse, Lehrer, Fach, Lehrauftrag), Versand, gesamte Oberfläche |
| Persistenz | Spring Data JDBC, Aggregat + getrenntes Persistenzmodell | JPA-`@Entity` |

Was unten über Schichten, DTO-Grenze und Lazy Loading steht, beschreibt den **Bestand**. Für den
migrierten Kontext gelten die ADRs, und die Regeln daraus stehen als **ArchUnit-Test**
(`ArchitekturTest`) im Build — nicht nur hier.

Die Berührungspunkte sind gezählt und benannt: `BuchungBestaetigungService` und
`AbsageBenachrichtigungService` lesen über Query-Ports und hängen an Domänen-Ereignissen; die
Presenter rufen ausschließlich Use-Case-Ports. Die Adapter `KlassenJdbcAdapter` und
`LehrauftraegeJdbcAdapter` lesen übergangsweise direkt die Tabellen der Schulorganisation — mit dem
Schnitt des Stammdaten-Kontexts (#140) wird daraus ein echter Fremdkontext-Zugriff.

## Schichten & Datenfluss (Bestand)

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
- Records liegen **verschachtelt im erzeugenden Service** — Definition und Mapping bleiben
  beieinander. Im migrierten Kontext liegen sie stattdessen **im Use-Case-Port**, der sie zusagt
  (`Buchungsoptionen.LehrkraftOption`, `Auswerten.SprechtagAuswertung`, `Buchen.BuchungsAnfrage`,
  `Sprechtagsuebersicht.SprechtagZeile`): Dort steht der Vertrag, und der Presenter kennt nur ihn.
  Die eine Ausnahme ist `SprechtagFormular` — veränderlich, weil der Vaadin-`Binder` Feld für Feld
  hinein- und herausschreibt.

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

- **Sprechtag** — Ein Sprechtag-Event: Datum, `Zeitfenster`, `Slotdauer`, Status, teilnehmende
  Klassen, `AccessToken` für den Eltern-Link, `Schulkontakt`. Seit Scheibe 2 ein **Aggregat-Root**
  (`sprechtag.domain.Sprechtag`) mit zwei tragenden Invarianten: Die **Statusübergänge** laufen
  ausschließlich über ihn (kein zweiter Schreibpfad setzt den Status mehr nebenbei), und die
  **Zeitstruktur friert ab `VEROEFFENTLICHT` ein** — Datum, Zeitfenster, Slot-Dauer und
  Klassenliste, denn genau daraus sind die Termine entstanden. Titel, Ort, Hinweistext,
  Schulkontakt und Token bleiben änderbar. Die Klassen stehen als `KlasseId` darin.
- **Lehrauftrag** — Verknüpft (Lehrer × Klasse × Fach). Das fachlich-organisatorische
  Ziel einer Buchung.
- **Termin** — Ein **materialisierter** Zeit-Slot einer Lehrkraft an einem Sprechtag. Seit Scheibe 1
  ein **Aggregat-Root** mit den `Buchung`en darin (`sprechtag.domain.Termin`). Gespeichert ist nur
  die `Verfuegbarkeit` (`VERFUEGBAR`/`ENTFAELLT`) — die Absicht des Organizers; „belegt" ist
  abgeleitet (`istBuchbar()`) und hat keine Spalte mehr. Sprechtag und Lehrkraft stehen als
  typisierte Ids darin, nicht als Objektverweis.
- **Buchung** — **Innere Entity** von `Termin`, kein eigenes Aggregat: Nur so hat die Invariante
  *ein Slot, höchstens eine aktive Buchung* einen Hüter. Status `ZUGESAGT`/`STORNIERT` (nicht
  `ABGESAGT` — das kollidierte mit dem Sprechtag). Sie hält ihre Angaben **selbst**: `Familie`
  (Eltern-/Schülername, E-Mail), `Notiz` und ein **eingefrorenes** `Buchungsziel`
  (Lehrkraft, Klasse, Fach zum Buchungszeitpunkt); die `LehrauftragId` bleibt nur Herkunftsspur,
  und der Fremdschlüssel darauf ist mit V4 gefallen, damit ein Import Lehraufträge löschen kann,
  ohne alte Buchungen mitzunehmen.

### Slot-Materialisierung

Beim Wechsel auf `VEROEFFENTLICHT` erzeugt der Use Case `Materialisieren` für **jede teilnehmende
Lehrkraft × jeden Zeit-Slot** einen freien `Termin`. Die Slots rechnet das Sprechtag-Aggregat selbst
aus (`Sprechtag.slots()`); ein Rest-Slot, der nicht mehr voll ins Zeitfenster passt, entfällt.
Teilnehmende Lehrkräfte kommen über den `Lehrauftraege`-Port aus der Schulorganisation — jede genau
einmal, geteilt über all ihre Fächer. Der Existenz-Check macht den Aufruf idempotent.

Der Gegenzug ist `ZurueckAufEntwurf`: Es **verwirft** die Termine, denn sie haben ihre Grundlage
verloren. Nur so ist die Zeitstruktur danach wieder änderbar und das nächste Veröffentlichen rechnet
neu. Gebucht sein darf dabei nichts — auch nicht storniert; benachrichtigt wurde trotzdem, und dann
führt nur die Absage weiter.

## Nebenläufigkeit & Persistenz

Dieser Bereich gilt als solide und ist **bewusst** so gebaut:

- **`spring.jpa.open-in-view=false`** — Sessions enden mit der Service-Transaktion; deshalb
  die DTO-Grenze (s. o.). Gilt nur noch für den Bestand: Spring Data JDBC lädt nichts lazy.
- **Optimistisches Locking** über `@Version` — am Persistenzmodell des jeweiligen Roots
  (`TerminZeile`, `SprechtagZeile`), niemals am Aggregat. Am Termin trägt es die
  Buchungs-Kollision und sperrt ihn samt seiner Buchungen; am Sprechtag die harmlosere, aber ebenso
  reale Kollision zweier Organizer-Fenster.
- **Atomare Buchung** (`BuchenService.buchen`): Ein Eltern-Submit mit N Wünschen ist
  „alles oder nichts". Ist auch nur ein Slot vergeben, rollt die ganze Transaktion zurück
  (`TerminBelegtException`) und es wird kein Ereignis veröffentlicht. Jedes Aggregat wird sofort
  gespeichert, damit ein Versionskonflikt im `try`-Block auftritt und nicht erst beim Commit;
  parallele Doppelbuchung eines Slots ist fachlich derselbe Fall.
- Das ist die **einzige** Stelle, die mehrere Aggregate in einer Transaktion ändert — ein benannter
  Bruch von „eine Transaktion, ein Aggregat" ([ADR 0005](../adr/0005-eltern-submit-bricht-eine-transaktion-ein-aggregat.md)).
- **Domain-Events**: Das Aggregat *meldet* feinkörnig (`BuchungAngelegt`, `BuchungStorniert`,
  `SprechtagVeroeffentlicht`, `SprechtagAbgesagt`), der Use Case *holt ab und bündelt* — beim Buchen
  zu `BuchungenBestaetigt`. Daran hängt der Versand; deshalb bekommt eine Familie mit vier Terminen
  eine Mail und nicht vier, und die Absage-Mails hängen an `SprechtagAbgesagt`. Veröffentlicht wird
  über den `Ereignisse`-Port; dessen Adapter ist die einzige Stelle, die `ApplicationEventPublisher`
  kennt. Semantik unverändert: `@TransactionalEventListener(AFTER_COMMIT)` plus `@Async`.
- **Stabile Kind-Ids**: Spring Data JDBC schreibt die Buchungszeilen beim Speichern neu
  (Delete-and-Insert). Weil die Domäne ihre Ids selbst vergibt, bleiben sie stabil — Voraussetzung
  dafür, dass der `@Async`-Listener sie nach dem Commit noch findet. Bewiesen in
  `TerminePersistenceAdapterTest`, nicht angenommen.

> Dies ist das Kronjuwel der App. Die Kernregel steht seit Scheibe 1 ohne Spring und ohne Datenbank
> im Test (`TerminTest`), die Atomarität weiterhin gegen eine echte Postgres
> (`BuchenUndAuswertenTest`).

## Zwei Wege in die Datenbank

Im migrierten Kontext gibt es sie bewusst:

- **Aggregat-Repositories** (`Termine`, `Sprechtage`) — der Schreibweg. Laden und speichern ganze
  Aggregate.
- **Query-Ports** (`TerminAnsichten`, `BuchungsAnsichten`, `SprechtagAnsichten`) — die Leseseite,
  handgeschriebenes SQL an den Aggregaten vorbei. Die Auswertung über Aggregate zu bauen wären rund
  600 Ladevorgänge.

Eine Ausnahme, die man kennen sollte: Der **Eltern-Zugang** (`Sprechtagszugang`) liest über das
Aggregat, nicht über einen Query-Port. Es geht um genau einen Sprechtag, und „darf hier gebucht
werden" ist keine Anzeigefrage — sie gehört an dieselbe Stelle wie die Regel.

Daraus folgt eine Regel, die man kennen muss: **Read-Modelle dürfen veraltet sein.** Jede
Entscheidung, die auf ihnen beruht, wird beim Schreiben am Aggregat erneut geprüft. Dass ein Slot in
der Eltern-Ansicht als frei erscheint, ist keine Zusage; `Termin.buche` entscheidet.

**Kein SQL-Statement joint über die Kontextgrenze.** Wo ein Read-Modell Stammdaten und Buchungen
braucht, liefern zwei Ports ihre Teile und der Use Case fügt sie in Java zusammen.

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
- Im migrierten Kontext verteilt sich das auf drei Stellen, jede mit eigenem Zweck:
  **Aggregat** (`TerminTest`, `SprechtagTest`, plain JUnit — die Kernregeln, ohne Spring und ohne
  Datenbank), **Persistenz-Adapter** (`TerminePersistenceAdapterTest`, `@DataJdbcTest` — stabile
  Buchungs-Ids, optimistisches Sperren) und **Use Case** (`BuchenUndAuswertenTest`,
  `SprechtagUseCaseTest` gegen eine echte Postgres — Rollback, Materialisierung, die
  Zusammenführung zweier Ports). Eine Regel, die ins Aggregat gehört, wird nicht auf Service-Ebene
  getestet.
- **Die Architekturregeln stehen als Test im Build** (`ArchitekturTest`, ArchUnit): Die Domäne
  importiert nur JDK, kein View kennt ein Aggregat, Adapter kennen einander nicht, die Anwendung
  kennt keinen Adapter. Regeln, die niemand prüfen kann, driften — F1–F9 sind der Beleg.
- Views bleiben dumm → kein Vaadin-E2E, keine View-Tests nötig. **Vaadin-freie UI-Modelle**
  (z. B. `BookingSession`) sind die Ausnahme: Sie tragen Entscheidungslogik und werden per plain
  JUnit getestet (`BookingSessionTest`) — ohne Spring-Kontext.

## Konventionen

- **CSS**: BEM (Block / Element / Modifier). Details in `/CLAUDE.md`.
- **Views**: responsive (Mobile, Tablet, Desktop).
- **Sichtbarkeit**: Presenter sind **package-private** (nur ihr View im selben Package nutzt sie).
  View-**Klassen** bleiben `public` (Vaadin-Route + paketübergreifende `X.ROUTE`/`X.class`-Referenzen),
  ihre **Konstruktoren** sind package-private (nur das Framework konstruiert sie).

## Lizenz

- Das Projekt steht unter **Apache-2.0** — bewusst permissiv, damit ein Schulträger die
  Prüfung ohne Rückfrage abschließen kann.
- Verbindlich sind genau zwei Stellen: die Datei **`/LICENSE`** (vollständiger Lizenztext,
  Grundlage der GitHub-Erkennung) und der **`<licenses>`-Block in `pom.xml`** (Name + URL,
  das, was ein Lizenz-Scanner am Maven-Artefakt sieht). Beide müssen übereinstimmen.
- **`/package.json` ist bewusst ausgenommen.** Die Datei ist ein Generat des
  `vaadin-maven-plugin` (`"name": "no-name"`, `"license": "UNLICENSED"`, `vaadin.hash`), steht
  in `.gitignore` und wird bei jedem Frontend-Build neu geschrieben. Sie gehört damit weder zum
  versionierten noch zum ausgelieferten Stand — ausgeliefert wird das gebündelte Frontend im
  Spring-Boot-Jar, nicht das Paketmanifest. Eine Korrektur dort wäre nicht dauerhaft und für
  keinen Scanner sichtbar; die `UNLICENSED`-Kennung in einer lokalen Arbeitskopie ist deshalb
  kein Widerspruch zur Lizenzaussage des Repositories.

## Findings-Backlog

Bekannte Abweichungen vom Soll und ihr Stand. Erledigte Findings bleiben als Changelog stehen.

| #  | Thema                                                        | Status                |
|----|-------------------------------------------------------------|-----------------------|
| F1 | Read-Modelle statt Entities über die Presenter-Grenze       | ✅ erledigt           |
| F2 | Lazy-Zugriff im View (`@EntityGraph` bereits vorhanden)      | ✅ kein Bug           |
| F3 | Datum/Zeit zentral über `ui.Formats`                        | ✅ erledigt           |
| F4 | Service-Tests (Buchen, `SprechtagService`)                  | ✅ erledigt           |
| F5 | `OrganizerView` dumm, Logik ins View-Model                  | ✅ erledigt           |
| F6 | Sichtbarkeit von Presentern/Views vereinheitlicht           | ✅ erledigt           |
| F7 | Filterlogik `ManageSprechtagView` → `…Presenter.filter(…)`  | ✅ erledigt           |
| F8 | Screen-Entscheidung Eltern-View → `…Presenter.pruefeZugang` | ✅ erledigt           |
| F9 | Buchungs-Warenkorb + Konfliktlogik → `BookingSession` (+Test)| ✅ erledigt           |
