# Architektur — Elternsprechtag

Lebende Architektur-Doku. Beschreibt den Zustand, der tatsächlich im Repo steht, und die geltenden
Regeln. Kurzfassung der harten Regeln: [`/CLAUDE.md`](../../CLAUDE.md). Die harten Regeln stehen
zusätzlich als Test im Build (`ArchitekturTest`) — der [Findings-Backlog](#findings-backlog) am
Ende ist deshalb geschlossen und nur noch Changelog.

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

Stack: **Spring Boot + Vaadin (Flow)** auf **PostgreSQL** (Flyway), Lombok, BEM-CSS. Persistenz
durchgängig **Spring Data JDBC** ([ADR 0004](../adr/0004-spring-data-jdbc-statt-jpa.md)); JPA und
Hibernate sind nicht mehr im Projekt.

## Zwei Kontexte, eine Struktur

Die Codebasis ist **hexagonal geschnitten**, mit zwei Bounded Contexts und DDD-Aggregaten
([ADR 0003](../adr/0003-hexagonale-architektur-mit-zwei-kontexten.md),
[0004](../adr/0004-spring-data-jdbc-statt-jpa.md), [0005](../adr/0005-eltern-submit-bricht-eine-transaktion-ein-aggregat.md)).
Der Umbau lief in vier senkrechten Scheiben (#138–#141) und ist abgeschlossen: Es gibt keine
zweite, alte Struktur mehr und keine `@Entity`.

```
de.openclassware.elternsprechtag
├── sprechtag
│   ├── domain              Aggregate, Value Objects, Ereignisse   ← nur JDK-Importe
│   ├── application
│   │   ├── port/in         Use-Case-Schnittstellen
│   │   ├── port/out        Repository-, Query-, Ereignis-, Fremdkontext-Ports
│   │   └── service         Use-Case-Implementierungen
│   └── adapter
│       ├── Formats         geteilte Datums-/Zeit-Formatierung (Oberfläche wie Mailtext)
│       ├── in/web          Views, Presenter, gemeinsame UI-Bausteine
│       ├── out/persistence Persistenzmodell, Mapper, Query-SQL
│       ├── out/mail        Versand (Absage, Buchungsbestätigung)
│       ├── out/event       der einzige Ort, der `ApplicationEventPublisher` kennt
│       └── out/schulorganisation  der Weg in den anderen Kontext
└── schulorganisation
    ├── domain | application | adapter    (ohne in/web)
```

Außerhalb der beiden Kontexte steht nur noch Verdrahtung: `config` (Spring-, Vaadin- und
Security-Konfiguration) und `security` (die eine Organizer-Rolle).

Die Regeln dieser Struktur stehen als **ArchUnit-Test** (`ArchitekturTest`) im Build — nicht nur
hier. Wer eine Grenze verletzt, bekommt einen roten Build, keine Review-Anmerkung.

## Schichten & Datenfluss

Die Abhängigkeit zeigt **nach innen**: Adapter kennen Ports, Ports kennen die Domäne, die Domäne
kennt nichts.

```
Adapter in/web      Views, Presenter (Vaadin, BEM-CSS)
      │  ruft
port/in             Use-Case-Schnittstellen — der einzige Eingang
      │
application/service Use Cases (@Transactional): laden, entscheiden lassen, speichern, melden
      │  ruft
port/out            Aggregat-Repository, Query-Port, Ereignis-Port, Fremdkontext-Port
      │  erfüllt
Adapter out/…       Spring Data JDBC, handgeschriebenes SQL, Mail, Events
```

**Regeln:**

- **Presenter rufen ausschließlich Use-Case-Ports** (`port/in`) — auch für Abfragen. Kein View
  und keine Komponente greift an ihnen vorbei auf einen Query-Port, ein Repository oder einen
  anderen Adapter zu.
- **Kein View und keine UI-Komponente hält je ein Aggregat.** Über die Presenter-Grenze gehen
  ausschließlich Records. Die Begründung ist heute eine andere als früher — es geht nicht mehr um
  Lazy Loading, sondern um die Aggregat-Grenze: Ein `Termin` im View wäre ein Schreibobjekt in
  einer Hand, die nicht schreiben darf.
- Die Records liegen **im Use-Case-Port**, der sie zusagt (`Buchungsoptionen.LehrkraftOption`,
  `Auswerten.SprechtagAuswertung`, `Buchen.BuchungsAnfrage`, `Sprechtagsuebersicht.SprechtagZeile`):
  Dort steht der Vertrag, und der Presenter kennt nur ihn. Die eine Ausnahme ist
  `SprechtagFormular` — veränderlich, weil der Vaadin-`Binder` Feld für Feld hinein- und
  herausschreibt.
- **Die Domäne importiert nur JDK.** Kein Spring, kein Lombok, keine Persistenz, kein Vaadin.
  Aggregate referenzieren einander ausschließlich über typisierte Ids.
- **Sichtbarkeit als Grenze, nicht als Konvention**: Use-Case-Implementierungen und Outbound-
  Adapter samt Mappern und Persistenzmodellen sind package-private — nach außen gilt der Port.
  Öffentlich sind die Ports, die Domäne und die View-Klassen (Vaadin-Route).

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
  Klassen, `AccessToken` für den Eltern-Link, `Schulkontakt`. Ein **Aggregat-Root**
  (`sprechtag.domain.Sprechtag`) mit zwei tragenden Invarianten: Die **Statusübergänge** laufen
  ausschließlich über ihn (kein zweiter Schreibpfad setzt den Status mehr nebenbei), und die
  **Zeitstruktur friert ab `VEROEFFENTLICHT` ein** — Datum, Zeitfenster, Slot-Dauer und
  Klassenliste, denn genau daraus sind die Termine entstanden. Titel, Ort, Hinweistext,
  Schulkontakt und Token bleiben änderbar. Die Klassen stehen als `KlasseId` darin.
- **Lehrauftrag** — Verknüpft (Lehrer × Klasse × Fach). Das fachlich-organisatorische
  Ziel einer Buchung.
- **Termin** — Ein **materialisierter** Zeit-Slot einer Lehrkraft an einem Sprechtag. Ein
  **Aggregat-Root** mit den `Buchung`en darin (`sprechtag.domain.Termin`). Gespeichert ist nur
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

- **Kein Lazy Loading.** Spring Data JDBC lädt ein Aggregat vollständig oder gar nicht. Die ganze
  Narbe der JPA-Zeit — `open-in-view`, `@EntityGraph`, `LazyInitializationException` — ist mit
  ADR 0004 entfallen; wer sie in älteren Notizen findet, liest Geschichte.
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

> Dies ist das Kronjuwel der App. Die Kernregel steht ohne Spring und ohne Datenbank
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
- Datum/Zeit-Formatierung läuft ausschließlich über **`sprechtag.adapter.Formats`** (`time`, `dateLong`,
  `monthShort`) — nicht inline duplizieren. Die Klasse liegt bewusst über den Adaptern: Oberfläche
  und Mailtext formatieren dasselbe Datum. Ein späterer Sprachwechsel bliebe dadurch billig,
  ist aber nicht geplant.

## Tests

Die Tests verteilen sich auf drei Stellen, jede mit eigenem Zweck — **eine Regel, die ins Aggregat
gehört, wird nicht auf Service-Ebene getestet**:

- **Aggregat** (`TerminTest`, `SprechtagTest`, die Domänentests der Schulorganisation): plain
  JUnit, ohne Spring und ohne Datenbank. Hier stehen die Kernregeln — Millisekunden statt Sekunden.
- **Persistenz-Adapter** (`TerminePersistenceAdapterTest`, `SprechtagePersistenceAdapterTest`,
  `@DataJdbcTest` gegen eine echte Postgres): stabile Buchungs-Ids, optimistisches Sperren, und
  seit dem Wegfall von Hibernates `validate` auch die Frage, ob Migration und Persistenzmodell
  überhaupt zusammenpassen.
- **Use Case** (`SprechtagUseCaseTest`, `BuchenUndAuswertenTest` und die Versand-Tests, alle über
  die gemeinsame Naht `@ServiceTest`): Rollback, Materialisierung, Ereignisse, die Zusammenführung
  zweier Ports. Gegen dieselbe Postgres, mit derselben Migrationskette.
- **Die Architekturregeln stehen als Test im Build** (`ArchitekturTest`, ArchUnit): Die Domäne
  importiert nur JDK, kein View kennt ein Aggregat, Adapter kennen einander nicht, die Anwendung
  kennt keinen Adapter, in die Schulorganisation führt nur ihr `port/in`, und Innenleben bleibt
  package-private. Regeln, die niemand prüfen kann, driften — F1–F9 sind der Beleg.
- Views bleiben dumm → kein Vaadin-E2E, keine View-Tests nötig. **Vaadin-freie UI-Modelle**
  (z. B. `BookingSession`) sind die Ausnahme: Sie tragen Entscheidungslogik und werden per plain
  JUnit getestet (`BookingSessionTest`) — ohne Spring-Kontext.

## Konventionen

- **CSS**: BEM (Block / Element / Modifier). Details in `/CLAUDE.md`.
- **Views**: responsive (Mobile, Tablet, Desktop).
- **Sichtbarkeit**: Presenter sind **package-private** (nur ihr View im selben Package nutzt sie).
  View-**Klassen** bleiben `public` (Vaadin-Route + paketübergreifende `X.ROUTE`/`X.class`-Referenzen),
  ihre **Konstruktoren** sind package-private (nur das Framework konstruiert sie). Dasselbe eine
  Schicht tiefer: Use-Case-Services und Outbound-Adapter sind package-private, öffentlich ist ihr
  Port. Der `ArchitekturTest` prüft es.
- **Sprache**: Fachsprache deutsch (`Termin.buche(...)`, `Sprechtag`, `Lehrauftrag`),
  Architekturvokabular englisch (`port/in`, `adapter`, `domain`, `application`).

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

**Geschlossen.** F1–F9 waren Abweichungen der alten Schichtenarchitektur — Entities über der
Presenter-Grenze, Lazy-Zugriffe, Logik im View. Sie sind nicht einzeln nachgepflegt worden, sondern
mit dem Umbau (#137, Scheiben #138–#141) **überholt**: Die Strukturen, in denen sie auftreten
konnten, gibt es nicht mehr, und was von ihnen als Regel übrig ist, steht seither im
`ArchitekturTest` statt in dieser Tabelle.

Genau das war die Lehre daraus, und sie steht so in ADR 0003: *Regeln, die niemand prüfen kann,
driften.* Eine Liste bekannter Abweichungen ersetzt keinen Test. Neue Abweichungen gehören deshalb
nicht wieder hierher, sondern entweder in eine Regel im `ArchitekturTest` oder in ein Issue.

Die Tabelle bleibt als Changelog stehen:

| #  | Thema                                                        | Status                        |
|----|-------------------------------------------------------------|-------------------------------|
| F1 | Read-Modelle statt Entities über die Presenter-Grenze       | ✅ erledigt, seither Regel     |
| F2 | Lazy-Zugriff im View (`@EntityGraph` bereits vorhanden)      | ⬛ gegenstandslos (kein JPA)   |
| F3 | Datum/Zeit zentral über `Formats`                           | ✅ erledigt                    |
| F4 | Service-Tests (Buchen, `SprechtagService`)                  | ✅ erledigt, Tests mitgewandert|
| F5 | `OrganizerView` dumm, Logik ins View-Model                  | ✅ erledigt                    |
| F6 | Sichtbarkeit von Presentern/Views vereinheitlicht           | ✅ erledigt, seither Regel     |
| F7 | Filterlogik `ManageSprechtagView` → `…Presenter.filter(…)`  | ✅ erledigt                    |
| F8 | Screen-Entscheidung Eltern-View → `…Presenter.pruefeZugang` | ✅ erledigt                    |
| F9 | Buchungs-Warenkorb + Konfliktlogik → `BookingSession` (+Test)| ✅ erledigt                    |
