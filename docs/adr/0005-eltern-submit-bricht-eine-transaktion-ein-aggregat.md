# ADR 0005: Der Eltern-Submit bricht bewusst „eine Transaktion, ein Aggregat"

Status: akzeptiert
Datum: 2026-09-13

Ergänzt [ADR 0003](0003-hexagonale-architektur-mit-zwei-kontexten.md). Diese ADR hält den einen
Punkt fest, an dem der dort beschlossene Schnitt eine anerkannte DDD-Regel verletzt — damit der
Bruch eine Entscheidung bleibt und nicht als Versehen gelesen wird.

## Kontext

Martin Fowlers Definition des Aggregats nennt vier Eigenschaften. Die vierte lautet:

> *Transactions should not cross aggregate boundaries.*

ADR 0003 schneidet `Termin` als Aggregat-Root mit `Buchung` als innerer Entity. Für eine einzelne
Buchung geht die Rechnung auf: ein Aggregat, eine Transaktion, eine Invariante, ein Hüter.

Das Produkt verlangt aber mehr. `ARCHITECTURE.md` und `BuchungService.buchen` halten fest: **Ein
Eltern-Submit mit N Terminwünschen ist „alles oder nichts".** Ist auch nur ein Slot zwischenzeitlich
vergeben, rollt die gesamte Transaktion zurück und die Eltern bekommen `TerminBelegtException`.
Das ist keine Implementierungsbequemlichkeit, sondern eine Produktentscheidung: Eine Familie, die
vier Lehrkräfte sehen will, soll nicht mit zwei Terminen und zwei Absagen dastehen.
`ABDECKUNG.md` Z. 187–191 zieht dieselbe Anforderung ausdrücklich auf die kommende
Organizer-Buchungsstrecke: *„Sie muss dieselben Regeln durchsetzen wie die Eltern-Ansicht: kein
Zeitkonflikt, alles oder nichts."*

N Wünsche bedeuten N Slots bei N verschiedenen Lehrkräften — also **N `Termin`-Aggregate in einer
Transaktion**.

## Entscheidung

Der Use Case `Buchen` **darf mehrere `Termin`-Aggregate in einer Transaktion ändern**. Das ist die
einzige Stelle im Projekt, an der das erlaubt ist.

Der Mechanismus bleibt der heutige und erprobte:

- Optimistisches Locking über `@Version` am `Termin`-Root.
- Jedes Aggregat wird sofort gespeichert, damit ein Versionskonflikt im `try`-Block auftritt und
  nicht erst beim Commit.
- Ein Konflikt (`OptimisticLockingFailureException`) wird als `TerminBelegtException` behandelt.
- Die gesamte Transaktion rollt zurück; die von den Aggregaten gemeldeten Ereignisse werden dann
  nie zu einem Vorgangs-Ereignis gebündelt und nie veröffentlicht.

Jede andere Operation im Projekt bleibt bei einem Aggregat je Transaktion.

## Begründung

- **Die Alternative bricht dieselbe Regel häufiger.** Mit `Buchung` als eigenem Aggregat wäre schon
  die *einfachste* Buchung aggregatübergreifend — und die Invariante *ein Slot, eine Buchung* hätte
  zusätzlich keinen Hüter. Regel 4 einmal für einen mehrteiligen Vorgang zu brechen ist billiger,
  als sie bei jeder Einzelbuchung zu brechen.
- **Die DDD-reinen Auswege kosten mehr, als sie einbringen.** Eventual Consistency mit
  nachträglichem Storno hieße, Eltern eine Zusage zu geben und sie zurückzunehmen — bei rund 200
  Buchungen je Sprechtag steht das in keinem Verhältnis. Ein gröberes Aggregat (alle Slots einer
  Lehrkraft) würde zwei Eltern kollidieren lassen, die verschiedene Slots derselben gefragten
  Lehrkraft buchen.
- **Der Umfang ist begrenzt und real.** Ein Submit umfasst eine Handvoll Termine, keine
  unbestimmte Menge. Die Transaktion bleibt kurz, die Sperren bleiben fein (je Slot, nicht je
  Lehrkraft oder Sprechtag).
- **Es bleibt eine Ausnahme, weil sie benannt ist.** Ein undokumentierter Bruch wird zum Muster;
  ein dokumentierter bleibt eine Ausnahme, auf die man beim nächsten Fall verweisen kann.

## Konsequenzen

- **Positiv:** Das erprobte Verhalten des Buchungsvorgangs — das Kronjuwel der Anwendung — bleibt
  unverändert erhalten. Die Sperrgranularität bleibt am einzelnen Slot, also am natürlichen
  Konkurrenzpunkt.
- **Negativ / Kosten:**
  - Das Modell ist nicht formal DDD-rein; wer die Regel kennt, wird hier stolpern. Dafür gibt es
    diese ADR.
  - Sollte die Anwendung je auf mehrere Knoten oder getrennte Datenspeicher wachsen, wäre diese
    Stelle die erste, die bricht. Für eine Schulanwendung mit einer PostgreSQL-Instanz ist das
    kein absehbarer Fall.
- **Abgrenzung:** Diese Erlaubnis gilt **ausschließlich** für das gemeinsame Festschreiben mehrerer
  Terminwünsche eines Vorgangs (Eltern-Submit, künftig Organizer-Nachtrag und Umbuchen). Sie ist
  keine allgemeine Erlaubnis, Aggregate in einer Transaktion zu vermischen, und ausdrücklich
  **keine** Erlaubnis, die Kontextgrenze zur `schulorganisation` transaktional zu überschreiten.
- **Durchsetzung:** Der ArchUnit-Regelsatz aus ADR 0003 kann diesen Bruch nicht prüfen — er ist
  eine Laufzeiteigenschaft. Er wird stattdessen durch Tests des Buchungs-Use-Cases abgesichert
  (Rollback bei belegtem Slot, kein Ereignisversand nach Rollback).

## Alternativen (verworfen)

- **Eventual Consistency** (Buchung annehmen, Prozessmanager setzt nach und storniert bei
  Konflikt): formal rein, aber die Zusage an die Eltern wäre widerruflich.
- **Ein Aggregat je Lehrkraft und Sprechtag** (`Terminplan`): benennt ein echtes fachliches
  Konzept und würde N von „je Slot" auf „je Lehrkraft" senken — löst die Mehr-Aggregat-Transaktion
  aber nicht, weil ein Submit gerade verschiedene Lehrkräfte betrifft, und verschlechtert die
  Nebenläufigkeit spürbar.
- **Die Atomarität aufgeben** (jeder Wunsch für sich, Teilerfolg möglich): würde die Regel wahren,
  aber eine ausdrückliche Produktentscheidung kassieren — und Familien mit halb gebuchten
  Terminplänen zurücklassen.
