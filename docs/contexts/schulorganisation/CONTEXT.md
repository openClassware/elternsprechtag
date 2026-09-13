# Domänen-Kontext — Schulorganisation

Das Vokabular der **Stammdaten**: wer unterrichtet was in welcher Klasse. Der zweite der beiden
Kontexte des Projekts — die Übersicht steht in [`CONTEXT-MAP.md`](../../../CONTEXT-MAP.md), der
andere Kontext in [`../sprechtag/CONTEXT.md`](../sprechtag/CONTEXT.md).

**Was hier _nicht_ steht:** Architekturregeln stehen in
[`docs/arc/ARCHITECTURE.md`](../../arc/ARCHITECTURE.md), die Kurzfassung in
[`CLAUDE.md`](../../../CLAUDE.md), einzelne Entscheidungen in [`docs/adr/`](../../adr/).

## Worum es geht

Dieser Kontext beantwortet eine einzige Frage: **Wer unterrichtet was in welcher Klasse?** Er hat
**keine Oberfläche** — kein Bildschirm des Produkts gehört ihm. Der Sprechtag-Kontext liest ihn,
und zwar nur lesend.

Er ist das **Ziel des später kommenden Stammdaten-Imports**. Dass es ihn schon vor dem Import gibt,
liegt daran, dass er die Entscheidungen festlegt, die der Import sonst nebenbei träfe — allen voran
die, dass Stammdaten stillgelegt und nicht gelöscht werden.

## Glossar

### Lehrkraft

Die Person, mit der am Sprechtag gesprochen wird: Vorname, Nachname, Kürzel.

Lehrkräfte haben **kein Login** — sie sind Stammdaten, keine Benutzer. Siehe Abschnitt „Auth" in
[`ARCHITECTURE.md`](../../arc/ARCHITECTURE.md).

Sprachgebrauch: Überall **Lehrkraft**. Die Tabelle heißt aus historischen Gründen weiterhin
`lehrer`, und ihre Fremdschlüssel heißen `lehrer_id`; das bleibt so, weil ein Umbenennen eine
Migration über Tabellen mit Fremdschlüsseln kostete, ohne dass jemand außerhalb des
Persistenz-Adapters etwas davon hätte. **„Lehrer" steht nur noch dort, wo es den Spaltennamen
meint.**

### Klasse

Eine Schulklasse (`10a`). Ein Sprechtag umfasst eine oder mehrere Klassen — daraus leitet sich ab,
welche Lehrkräfte teilnehmen und welche Gespräche buchbar sind.

Eine Klasse wird **umbenannt**, nicht versetzt: Aus der 5a wird im nächsten Schuljahr *nicht* die
6a, weil die 5a dann von anderen Kindern gebildet wird. Wie der Jahrgangswechsel abläuft,
entscheidet der Import, wenn Quellsystem und Format bekannt sind.

### Fach

Das Unterrichtsfach: Name und Kürzel. Fachlich nur als Teil eines Lehrauftrags relevant — und
trotzdem ein eigener Datensatz, weil es umbenannt und stillgelegt wird, ohne dass ein Lehrauftrag
davon weiß.

Sprachgebrauch: **Kürzel**. Die Spalte heißt `short_name`.

### Lehrauftrag

Die Verknüpfung **Lehrkraft × Klasse × Fach** — „Frau Berg unterrichtet Deutsch in der 5a".

Ein Lehrauftrag **ist** sein Tripel. Wechselt einer der drei Verweise, ist es ein anderer
Lehrauftrag: Der alte wird stillgelegt, der neue erteilt. Es gibt deshalb kein „Lehrauftrag
ändern" — alles andere hieße, den Auftrag „Deutsch in der 5a" still in „Mathe in der 7b" zu
verwandeln, während Buchungen auf ihn zeigen.

Dasselbe Tripel gibt es **höchstens einmal**. Diese Regel steht als Datenbank-Constraint, nicht als
Aggregat-Invariante: Sie spannt über alle Lehraufträge, und ein Aggregat, das sie prüfen wollte,
müsste dafür alle laden.

Aus Sicht des Sprechtag-Kontexts ist der Lehrauftrag das **Buchungsziel** — dort steht, was das
heißt.

### Stilllegen

Der Lebenszyklus, den alle vier Stammdaten teilen: **angelegt → stillgelegt**. Ein stillgelegter
Datensatz bleibt bestehen, nimmt aber an nichts Neuem mehr teil, und er lässt sich nicht mehr
ändern.

Stilllegen ist das Gegenstück zum Löschen, und es ist die tragende Entscheidung dieses Kontexts:

- **Gelöscht wird nicht.** Buchungen und Auswertungen früherer Sprechtage bezeugen die Stammdaten;
  ein gelöschter Datensatz risse Lücken in Vergangenes.
- **Stillgelegt heißt: raus aus dem Angebot.** Eine stillgelegte Lehrkraft steht nicht mehr zur
  Wahl, eine stillgelegte Klasse lässt sich nicht mehr zu einem Sprechtag einladen.
- **Schon Vergebenes bleibt lesbar.** Ein stillgelegter Lehrauftrag ist einzeln weiterhin abrufbar,
  und eine stillgelegte Klasse behält ihren Namen an dem Sprechtag, der sie bereits eingeladen hat.

Dass das überhaupt geht, hängt daran, dass eine **Buchung ihr Ziel einfriert** (siehe
`CONTEXT-MAP.md`). Ohne das dürfte hier nichts stillgelegt werden.

Zweimal stilllegen ist kein Fehler — der Import darf denselben Abgleich wiederholen.

### Import

Der geplante Weg, auf dem Stammdaten aus einem Schulverwaltungssystem hierher gelangen. **Er ist
noch nicht gebaut**; dieser Kontext hat nur die Naht dafür (die Schreibseite: anlegen,
aktualisieren, stilllegen).

Offen und bewusst noch nicht entschieden: die Wiedererkennung über eine Fremd-Id und der Umgang mit
Datensätzen, die im nächsten Export fehlen. Beides wird entschieden, wenn Quellsystem und Format
bekannt sind.

## Begriffe, die wir nicht benutzen

| Nicht                       | Sondern                                                          |
|-----------------------------|------------------------------------------------------------------|
| Lehrer                      | **Lehrkraft** (nur der Spalten-/Tabellenname bleibt `lehrer`)     |
| Löschen (von Stammdaten)    | **Stilllegen** — gelöscht wird hier nichts                        |
| Lehrauftrag ändern          | alten **stilllegen**, neuen **erteilen**                          |
| Kurs, Unterricht            | **Lehrauftrag**                                                   |
