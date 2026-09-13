# ADR 0004: Spring Data JDBC statt JPA/Hibernate im Persistenz-Adapter

Status: akzeptiert
Datum: 2026-09-13

Ergänzt [ADR 0003](0003-hexagonale-architektur-mit-zwei-kontexten.md), der den Aggregat-Schnitt
und die Forderung nach einer technologiefreien Domäne festhält. Diese Entscheidung betrifft nur,
**womit** die Aggregate persistiert werden.

## Kontext

ADR 0003 verlangt Aggregate ohne Framework-Annotationen und ein **getrenntes Persistenzmodell**
mit Mapper. Das allein lässt die Frage offen, welche Technologie unter dem Adapter liegt — die
Trennung des Modells und die Wahl der Zugriffstechnologie sind zwei unabhängige Achsen.

Heute läuft alles über JPA/Hibernate. Das hat sichtbare Spuren hinterlassen:

- `spring.jpa.open-in-view=false`, daraus folgend die gesamte DTO-Grenze zum Presenter.
- Drei der vier Methoden in `BuchungRepository` tragen ein `@EntityGraph`, das ausschließlich
  Lazy Loading umgeht. Dasselbe Muster in `TerminRepository` und `SprechtagRepository`.
- Ein ganzer Abschnitt in `ARCHITECTURE.md` und mehrere Findings (F1, F2) existieren nur, um mit
  detached Entities und `LazyInitializationException` umzugehen.

Das Schema bleibt in jedem Fall dasselbe (PostgreSQL, Flyway); es geht nicht um eine
Datenmigration, sondern um die Zugriffsschicht, die beim Umbau ohnehin vollständig neu
geschrieben wird.

## Entscheidung

Der Persistenz-Adapter wird auf **Spring Data JDBC** gestellt. JPA/Hibernate entfällt.

Das Persistenzmodell bleibt vom Domänenmodell getrennt; Spring-Data-Annotationen (`@Id`,
`@Version`, `@Table`, `@MappedCollection`) liegen ausschließlich dort, niemals an einem Aggregat.

**Die Domäne vergibt ihre IDs selbst** (`TerminId.neu()`, `BuchungId.neu()`), nicht die Datenbank.
Das ist ohnehin DDD-üblich und hier zusätzlich notwendig: Spring Data JDBC schreibt die
Kind-Zeilen eines Aggregats beim Speichern neu, und die `Buchung`-IDs müssen über den Commit
hinaus stabil bleiben, weil `BuchungenBestaetigt` sie in einen `@Async`-Listener nach dem Commit
trägt und die Bestätigungsmail daran hängt.

## Begründung

- **Spring Data JDBC ist entlang genau dieses Aggregat-Modells gebaut.** Ein Repository je
  Aggregat-Root, Speichern schreibt das ganze Aggregat, Laden lädt es ganz, `AggregateReference`
  ist „Referenz per Identität" als Typ. Es **erzwingt** den Schnitt aus ADR 0003, statt ihn nur
  zuzulassen.
- **JPA arbeitet gegen den Entwurf.** `buchung.getTermin().getSprechtag()` bleibt jederzeit einen
  Punkt entfernt. Bei JPA wäre der Mapper das Einzige, was die alte Graph-Navigation draußen
  hält — reine Disziplin, und F1–F9 zeigen, wie belastbar die ist.
- **Kein Lazy Loading, also keine Lazy-Loading-Probleme.** Die gesamte Problemklasse verschwindet
  ersatzlos: kein `open-in-view`, keine detached Entities, keine `@EntityGraph`-Pflege. Das ist
  rund ein Drittel der heutigen Architekturregeln.
- **Die Leseseite verliert nichts.** ADR 0003 legt die Read-Modelle ohnehin auf einen Query-Port
  mit handgeschriebenem SQL. Dort ist SQL eher Vorteil als Nachteil — JPQL über einen Graphen,
  den es nicht mehr geben soll, wäre der falsche Weg.
- **Es ist kein zweites Risiko.** Das Schema ändert sich nicht, Flyway bleibt, und der Adapter
  wird beim Umbau ohnehin neu geschrieben. Der Mehraufwand gegenüber JPA ist Lernkurve, nicht
  Migrationsrisiko.

## Konsequenzen

- **Positiv:** Die Aggregat-Grenzen werden von der Zugriffstechnologie gestützt statt unterlaufen.
  Was geschrieben wird, ist explizit statt implizit (kein Dirty Checking, keine
  Cascade-Semantik). Die Lazy-Loading-Narbe entfällt.
- **Negativ / Kosten:**
  - Abfragen jenseits der Aggregat-Grenze sind handgeschriebenes SQL statt abgeleiteter
    Methodennamen — beabsichtigt, aber mehr Arbeit je Read-Modell.
  - Kein Second-Level-Cache, kein Dirty Checking, keine automatische Schemaableitung. Für dieses
    Projekt ist keines davon in Gebrauch.
  - Weniger verbreitetes Wissen als JPA; wer dazukommt, kennt vermutlich Hibernate besser.
  - Das Verhalten von Kind-Zeilen beim Speichern (Delete-and-Insert) wurde **nicht durch einen
    Spike verifiziert**, sondern wird in der ersten Migrationsscheibe (`Termin` + `Buchung`)
    bewiesen. Das ist die bewusst gewählte Reihenfolge: die riskanteste Scheibe zuerst.
- **Risiko und Gegenmaßnahme:** Sollte sich zeigen, dass Spring Data JDBC das `Termin`-Aggregat
  mit stabilen Kind-IDs und optimistischem Locking am Root nicht trägt, ist eine Scheibe verloren,
  nicht der ganze Umbau — das getrennte Persistenzmodell aus ADR 0003 bleibt gültig, nur der
  Adapter würde auf JPA zurückfallen.

## Alternativen (verworfen)

- **Getrenntes Persistenzmodell mit JPA/Hibernate:** kleinstes Risiko, erprobte Lock-Semantik,
  kein neues Framework. Verworfen, weil die Aggregat-Grenzen dann allein von Disziplin gehalten
  würden und die `@EntityGraph`-/`open-in-view`-Narbe erhalten bliebe.
- **Hibernate direkt auf die Domänen-POJOs via `orm.xml`:** kein `@Entity` im Code, keine
  Doppelklassen, kein Mapper. Verworfen als Etikettenschwindel — Hibernate erzwingt weiterhin
  No-Arg-Konstruktor, nicht-finale Felder und eigene Collection-Proxies, womit echte Immutability
  und Value Objects am Aggregat scheitern; `orm.xml` ist zudem schlecht unterstützt und bricht bei
  Refactorings stumm.
- **jOOQ oder plain `JdbcTemplate`:** Domäne zu 100 % frei, volle Kontrolle über jedes Statement.
  Verworfen wegen des Aufwands — Laden und Speichern inklusive der Buchungs-Collection und des
  optimistischen Locks wäre vollständig Handarbeit, ohne erkennbaren Gewinn gegenüber Spring Data
  JDBC, das genau das mitbringt.
- **Zwei Stacks nebeneinander** (Stammdaten bei JPA, Sprechtag-Kontext auf Spring Data JDBC):
  verworfen — zwei Persistenztechnologien in einem Projekt sind dauerhafte Kosten für jeden, der
  später dazukommt.
