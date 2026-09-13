# ADR 0003: Hexagonale Architektur mit zwei Kontexten und technologiefreien Aggregaten

Status: akzeptiert
Datum: 2026-09-13

## Kontext

Das Domänenmodell ist **anämisch**: Die acht Klassen unter `domain/` tragen `@Entity`,
Lombok-`@Getter`/`@Setter` und sonst nichts — die einzige Geschäftsmethode im gesamten Paket ist
`SprechtagStatusEnum.allowedTransitions()`. Die Regeln liegen in zwei Services (`BuchungService`
293 Zeilen, `SprechtagService` 317 Zeilen), die über einen dicht verdrahteten, bidirektionalen
Objektgraphen navigieren (`Sprechtag ↔ Klasse`, `Buchung → Termin → Sprechtag`, `Lehrer → Termin`).

Daraus folgt eine Reihe von Symptomen, die einzeln jeweils behandelt, aber nie an der Wurzel
gelöst wurden:

- Weil JPA-Entities lazy laden und `spring.jpa.open-in-view=false` gilt, musste eine DTO-Grenze
  zum Presenter gezogen werden. Drei der vier Methoden in `BuchungRepository` tragen ein
  `@EntityGraph`, das **ausschließlich** existiert, um Lazy Loading zu umgehen.
- Weil niemand für die Konsistenz zwischen `Sprechtag` und `Termin` zuständig ist, lässt
  `SprechtagService.createOrUpdate` Zeitfenster und Slot-Dauer eines **bereits veröffentlichten**
  Sprechtags ändern, ohne die Termine nachzuziehen — `materialisiereWennNoetig` steigt aus, weil
  schon Termine existieren. Die Termine driften stumm aus dem Raster.
- `docs/arc/ABDECKUNG.md` führt mehrere Fälle der Stufe `muss`, die alle dieselbe Ursache haben:
  Z. 87 (Zeitfenster nach dem Veröffentlichen sperren), Z. 88/89 (Klassen sperren), Z. 92/93
  (`createOrUpdate` umgeht `allowedTransitions`), Z. 230 (dritter Terminzustand fehlt).
- Der Findings-Backlog F1–F9 in `ARCHITECTURE.md` ist die Liste der Stellen, an denen der Code
  von den dokumentierten Regeln abgewichen ist — bei deutlich einfacheren Regeln als den hier
  beschlossenen.

Hinzu kommt eine Tatsache über die Zukunft: **Die Stammdaten kommen später aus einem Import.**
`Lehrer`, `Klasse`, `Fach` und `Lehrauftrag` werden heute vom Anwendungscode nie geschrieben —
kein `save`, kein `delete`, keine Verwaltungsoberfläche. Sie gehören fachlich einem anderen
System.

## Entscheidung

Die Codebasis wird auf eine **hexagonale Architektur mit zwei Bounded Contexts** und
**DDD-Aggregaten** umgestellt.

### Aggregate

Aggregate sind technologiefrei: reine Java-Klassen ohne `@Entity`, ohne Spring, ohne Lombok, mit
Importen nur aus dem JDK. **Aggregate referenzieren einander ausschließlich über typisierte IDs**
(`SprechtagId`, `TerminId`, `LehrkraftId`, …) — es gibt keine Objektnavigation über eine
Aggregat-Grenze hinweg.

| Kontext | Aggregat | Inhalt | Referenzen nach außen |
|---|---|---|---|
| `sprechtag` | **Sprechtag** | Titel, Datum, Zeitfenster, Slotdauer, AccessToken, Schulkontakt, Ort, Beschreibung, Status | `List<KlasseId>` |
| `sprechtag` | **Termin** | Zeitraum, Verfügbarkeit, **`List<Buchung>`** (innere Entities) | `SprechtagId`, `LehrkraftId`; je Buchung `LehrauftragId` |
| `schulorganisation` | **Lehrkraft**, **Klasse**, **Fach**, **Lehrauftrag** | Stammdaten | `Lehrauftrag` hält `LehrkraftId`, `KlasseId`, `FachId` |

`Buchung` ist **innere Entity von `Termin`**, nicht eigenes Aggregat — das ist Fowlers
Order/Line-Item-Fall. Die Invariante *ein Slot, höchstens eine aktive Buchung* lebt damit
innerhalb einer Konsistenzgrenze und wird vom Root durchgesetzt.

Folge daraus: **`TerminStatusEnum` entfällt.** „Belegt" war eine denormalisierte Zweitschrift der
Frage *gibt es eine aktive Buchung?* — zwei Wahrheiten für einen Fakt. Der Zustand wird künftig
abgeleitet. Erhalten bleibt ein **gespeichertes** Feld `Verfuegbarkeit` (`VERFUEGBAR` /
`ENTFAELLT`), denn ein entfallender Termin ist eine Absicht des Organizers, keine Folge — das ist
zugleich der in `ABDECKUNG.md` Z. 230 geforderte dritte Zustand.

### Einfrieren von Fremddaten

Eine `Buchung` speichert Lehrkraft, Klasse und Fach **als Schnappschuss zum Buchungszeitpunkt**
(`GebuchterUnterricht`), nicht als Verweis. Die `LehrauftragId` bleibt nur als Herkunftsspur.
Begründung: Ein periodischer Import lässt Lehraufträge verschwinden, während Buchungen daran
hängen; die Auswertung eines vergangenen Sprechtags muss davon unberührt bleiben.
`ABDECKUNG.md` Z. 89 beschreibt den Schaden bereits.

`Sprechtag` friert seine Zeitstruktur ab `VEROEFFENTLICHT` ein. Damit hält die Regel „Termin liegt
im Zeitfenster und im Slotraster" ohne laufende transaktionale Prüfung, und Z. 87 der Abdeckung
ist erfüllt.

### Zwei Kontexte

- **`schulorganisation`** — Ziel des kommenden Imports. Aggregate mit Schreibseite, aber **kein
  Web-Adapter**: Der Import ist ihr einziger Eingang.
- **`sprechtag`** — trägt die gesamte Oberfläche und konsumiert die Stammdaten über einen
  Outbound-Port.

Die Abhängigkeit ist **einseitig**: `sprechtag → schulorganisation`, nie umgekehrt. Es gibt kein
`shared`-Paket. Die kontextlosen UI-Bausteine (`MainLayout`, `LoginView`, `Formats`,
`ui/components`, `Roles`) liegen im Sprechtag-Kontext, weil dort die einzige Oberfläche lebt.

**Kein SQL-Statement joint über die Kontextgrenze.** Wo ein Read-Modell beide Seiten braucht,
liefern zwei Ports ihre Teile, und ein Use Case fügt sie in Java zusammen. Ein Sprechtag hat rund
30 beteiligte Lehrkräfte; die Kosten sind vernachlässigbar, der Gewinn ist eine Grenze, die auch
gegen Schemaänderungen hält.

### Ein Eingang, zwei Lesewege

Presenter rufen **ausschließlich Use-Case-Ports** — auch für Abfragen. Read-Modelle entstehen
dahinter an einem **Query-Port mit handgeschriebenem SQL**, der die Aggregate umgeht; die
Schreibseite und die Leseseite dieser Anwendung haben unterschiedliche Formen
(`SprechtagAuswertung` verbindet heute sechs Tabellen; über Aggregate gebaut wären das rund 600
Ladevorgänge).

Daraus folgt eine Regel, die festzuhalten ist: **Read-Modelle dürfen veraltet sein. Jede
Entscheidung, die auf ihnen beruht, wird beim Schreiben am Aggregat erneut geprüft.** Zwischen
„Slot wird als frei angezeigt" und „Submit" liegt beliebig viel Zeit; dass `Termin` beim Buchen
selbst prüft und sonst `TerminBelegtException` wirft, ist der tragende Mechanismus — nicht die
Aktualität der Anzeige.

### Domain-Events

Das **Aggregat meldet** feinkörnige Ereignisse (`BuchungAngelegt`, `BuchungStorniert`), weil laut
`ABDECKUNG.md` künftig drei Wege zur selben Zustandsänderung führen (Eltern-Submit, Organizer-
Nachtrag Z. 167, Umbuchen Z. 237) und alle dieselbe Bestätigung auslösen sollen (Z. 187–193). Der
**Use Case holt ab und bündelt** zum Vorgangs-Ereignis (`BuchungenBestaetigt`), an dem der Versand
hängt — eine Familie mit vier Terminen bekommt eine Mail, nicht vier (Z. 261).

Veröffentlicht wird über einen Outbound-Port; dessen Adapter ist die einzige Stelle im Projekt,
die `ApplicationEventPublisher` kennt. Die erprobte Semantik bleibt unverändert:
`@TransactionalEventListener(AFTER_COMMIT)` plus `@Async`.

### Paketstruktur

```
de.openclassware.elternsprechtag
├── sprechtag
│   ├── domain              Aggregate, Value Objects, Ereignisse   ← nur JDK-Importe
│   ├── application
│   │   ├── port/in         Use-Case-Schnittstellen
│   │   ├── port/out        Repository-, Query-, Ereignis-, Fremdkontext-Ports
│   │   └── service         Use-Case-Implementierungen
│   └── adapter
│       ├── in/web          Views, Presenter, gemeinsame UI-Bausteine
│       ├── out/persistence Persistenzmodell, Mapper, Query-SQL
│       └── out/mail        Versand
└── schulorganisation
    ├── domain | application | adapter    (ohne in/web)
```

**Sprachkonvention:** Fachsprache deutsch (`Termin.buche(...)`, `Sprechtag`, `Lehrauftrag`),
Architekturvokabular englisch (`port/in`, `adapter`, `domain`, `application`).

### Durchsetzung

Die Regeln werden als **ArchUnit-Tests** im Build festgehalten, nicht nur in Markdown: Die Domäne
importiert nichts außer JDK; `sprechtag` erreicht `schulorganisation` nur über deren Port; kein
View kennt ein Aggregat; Adapter kennen einander nicht; Aggregate referenzieren sich nur über
typisierte IDs.

## Begründung

- **Ein Aggregat ist eine Konsistenzgrenze, kein Ordnungsprinzip.** Der Schnitt folgt den
  Invarianten, nicht dem Lebenszyklus. `Termin` liegt deshalb *nicht* im `Sprechtag`, obwohl er
  daraus entsteht: Ein Sprechtag mit 30 Lehrkräften materialisiert rund 600 Termine, und ein
  Aggregat-Lock über alle davon würde sämtliche buchenden Eltern serialisieren.
- **Die Invariante braucht einen Hüter.** Mit `Buchung` als eigenem Aggregat wäre schon die
  einfachste Buchung aggregatübergreifend und die Kernregel dauerhaft ungeschützt.
- **Die Stammdaten gehören einem anderen System.** Sie als gewöhnliche Aggregate dieses Modells
  zu führen, würde eine Eigentümerschaft behaupten, die es nicht gibt. Der zweite Kontext legt
  die Naht dorthin, wo der Import andocken wird.
- **Regeln, die niemand prüfen kann, driften.** F1–F9 sind der Beleg. Fragen wie „referenziert
  dieses Aggregat ein anderes direkt?" sind im Review nicht zuverlässig zu beantworten, im Test
  schon.
- **Die Kernregel wird erstmals ohne Datenbank testbar.** *Ein Slot, eine Buchung* hängt heute an
  `@SpringBootTest` mit laufender Datenbank; am technologiefreien Aggregat ist es ein
  Millisekunden-Test in plain JUnit.

## Konsequenzen

- **Positiv:**
  - Mehrere offene `muss`-Fälle der Abdeckung werden strukturell erledigt (Z. 87, 88, 89, 92, 93,
    230), weil die Aggregate die Regeln erzwingen, die heute niemand durchsetzt.
  - Die gesamte Lazy-Loading-Narbe entfällt: kein `open-in-view`-Problem, keine
    `LazyInitializationException`, keine `@EntityGraph`-Pflege.
  - Geschäftslogik wird ohne Spring-Kontext und ohne Datenbank testbar.
  - Der kommende Import findet eine Naht vor, statt eine zu erzwingen.
- **Negativ / Kosten:**
  - Der Umbau berührt die 8 Domänenklassen, beide Services (610 Zeilen), alle 7 Repositories, die
    12 Testklassen und die Presenter-Seite der 25 UI-Dateien. Das ist eine **Neuschreibung der
    Innenseite bei gleichbleibender Oberfläche**, kein Refactoring.
  - Zusätzliche Klassen: Persistenzmodell und Mapper je Aggregat, typisierte IDs, Ports.
  - Zwei Wege in die Datenbank (Aggregat-Repository und Query-Port), die als solche verstanden
    sein wollen.
  - Für die Dauer der Migration stehen alte und neue Struktur nebeneinander.
- **Abgrenzung:** Diese Entscheidung ändert **nichts** am Auth-Modell (eine Organizer-Identität,
  anonymer Eltern-Zugang über den Access-Token-Link), nichts an der Deutsch-only-i18n und nichts
  an den Produktentscheidungen in `ABDECKUNG.md`. Sie ändert, *wie* gebaut wird, nicht *was*.

## Alternativen (verworfen)

- **`Sprechtag` als großes Aggregat** (Termine und Buchungen enthalten): formal am saubersten,
  praktisch tödlich — alle buchenden Eltern konkurrieren um einen Lock.
- **`Buchung` als eigenes Aggregat, `Termin` als Wächter** (heutiges Verhalten mit ID-Referenzen):
  kleinster Schritt, aber die zentrale Invariante bliebe dauerhaft ohne Hüter, und schon die
  Einzelbuchung würde die Aggregat-Grenze kreuzen.
- **`Buchung` als eigenes Aggregat mit eventual consistency** (ein Prozessmanager storniert bei
  Konflikt nach): formal DDD-rein, skaliert am besten — aber Eltern bekämen eine Zusage, die
  später zurückgenommen wird. Bei rund 200 Buchungen je Sprechtag steht das in keinem Verhältnis.
- **Ein Bounded Context mit sechs gleichwertigen Aggregaten:** einheitlich zu erklären, behauptet
  aber Eigentümerschaft an Stammdaten, die importiert werden.
- **Read-Modelle über die Aggregate bauen:** ein Weg in die Datenbank, maximale Konsistenz — aber
  ein N+1-Problem als Architekturentscheidung.
- **`Terminplan` je Lehrkraft als Aggregat** (statt je Termin): benennt ein echtes fachliches
  Konzept, macht den Lock aber zu grob — zwei Eltern, die verschiedene Slots derselben gefragten
  Lehrkraft buchen, kollidierten ohne Not.
- **Zwei Maven-Module statt zwei Paketbäume:** stärkste Durchsetzung, aber Build-Zeremonie für
  rund 1300 Zeilen Geschäftslogik.
