# ADR 0002: Zweckerweiterung der Eltern-E-Mail-Adresse auf die Buchungsbestätigung

Status: akzeptiert
Datum: 2026-08-06

## Kontext

[ADR 0001](0001-eltern-email-pflicht-fuer-absage-benachrichtigung.md) hat die Pflicht-E-Mail an
der `Buchung` eingeführt und sie **zweckgebunden** festgeschrieben: „ausschließlich der
Benachrichtigung über Änderungen an genau dem gebuchten Sprechtag" — konkret der Absage des
gesamten Sprechtags.

Der Buchungsablauf der Eltern endet damit heute ohne jede Spur: Eltern buchen anonym über den
Access-Token-Link, sehen eine Bestätigungsseite und haben danach **nichts** in der Hand — keine
Terminliste, keinen Beleg, keine Erinnerung an Uhrzeit und Lehrkraft. Es gibt kein Eltern-Login,
über das sich die Buchung nachschlagen ließe; der Token-Link führt zur Buchungsansicht, nicht zu
den eigenen Terminen. Wer die Bestätigungsseite schließt oder auf dem Handy gebucht hat, ist auf
das eigene Gedächtnis angewiesen.

Eine Bestätigungsmail unmittelbar nach der Buchung schließt genau diese Lücke — sie nutzt aber
dieselbe Adresse für einen Zweck, den ADR 0001 ausdrücklich ausgeschlossen hat. Diese Entscheidung
gehört offen dokumentiert, nicht stillschweigend mit implementiert.

## Entscheidung

Die an der `Buchung` gespeicherte E-Mail-Adresse dient künftig **zwei** Zwecken:

1. **Benachrichtigung** über Änderungen an dem gebuchten Sprechtag (bisher, aus ADR 0001) — in
   dieser Iteration die Absage des gesamten Sprechtags.
2. **Bestätigung der eigenen Buchung** unmittelbar nach dem Absenden — als Beleg mit den
   gebuchten Terminen (Datum, Uhrzeit, Lehrkraft).

Die Zweckbindung bleibt bestehen, sie wird nur um diesen zweiten Punkt erweitert. Beide Zwecke
betreffen ausschließlich **die eigene Buchung der Eltern an genau diesem Sprechtag**. Alles
darüber hinaus — insbesondere jede anlasslose oder werbliche Ansprache — bleibt ausgeschlossen.

## Begründung

- **Ohne Beleg endet der Buchungsablauf spurlos.** Die Bestätigungsseite ist flüchtig; danach
  existiert für die Eltern keine überprüfbare Information über den gebuchten Termin.
- **Die Adresse liegt bereits vor** und ist bereits Pflicht — für den Beleg entsteht kein neues
  Formularfeld und keine zusätzliche Buchungshürde.
- **Der Beleg passt zum Anonymitätsmodell.** Die Mail ist eine Einbahnstraße: Sie transportiert,
  was die Eltern gerade selbst eingegeben haben, und eröffnet keinen Rückkanal in die Anwendung.
- **Die Alternative wäre teurer.** Ein abrufbarer Buchungsstatus verlangt entweder ein
  Eltern-Login oder einen personenbezogenen Dauer-Link — beides sind größere Eingriffe ins
  Auth-Modell als eine Mail (siehe „Alternativen").

## Datenschutz

- **Es werden keine zusätzlichen Daten erhoben.** Kein neues Feld, kein neuer Datensatz, keine
  weitere Kategorie personenbezogener Daten. Erweitert wird ausschließlich der **Verwendungszweck**
  der bereits nach ADR 0001 erhobenen Adresse.
- Die Bestätigungsmail enthält nur Daten, die die Eltern im selben Vorgang selbst angegeben oder
  ausgewählt haben (Name, Kind, gebuchte Termine).
- **Aufbewahrungs- und Löschfristen bleiben weiterhin offen.** ADR 0001 hat diesen Punkt als
  Kosten benannt und nicht entschieden; daran ändert diese ADR nichts. Er bleibt eine offene
  Entscheidung und wird hier ausdrücklich **nicht** stillschweigend als erledigt behandelt.

## Konsequenzen

- **Positiv:** Eltern haben nach der Buchung einen dauerhaften Beleg mit Datum, Uhrzeit und
  Lehrkraft. Fehleingaben bei der Adresse fallen sofort auf, statt erst im Absagefall — die
  Erreichbarkeit aus ADR 0001 wird dadurch nebenbei praktisch geprüft.
- **Negativ / Kosten:**
  - Jede Buchung erzeugt eine Mail; bei falsch angegebener Adresse geht der Beleg an Dritte —
    dasselbe Risiko wie bei der Absage-Mail, nun aber bei jeder Buchung statt nur im Ausnahmefall.
  - Der Versand ist best-effort: Eine nicht zugestellte Bestätigung darf die Buchung nicht
    scheitern lassen; die Buchung bleibt die Wahrheit, die Mail ist nur ihr Beleg.
- **Bewusst nicht Teil dieser Entscheidung:**
  - **Kein Storno-Link** in der Mail — Stornieren bleibt außerhalb dieser Iteration.
  - **Keine Buchungsansicht** für Eltern (kein abrufbarer Stand der eigenen Buchung).
  - **Kein Eltern-Login** und keine Eltern-Accounts; der Zugang bleibt anonym per Token-Link.
  - Diese drei sind eigene Entscheidungen und wären eigene ADRs.

## Alternativen (verworfen)

- **Alles beim Alten lassen (nur Bestätigungsseite):** belässt die Eltern ohne jeden Beleg — genau
  das Problem, das diese ADR löst.
- **Eltern-Buchungsansicht per personenbezogenem Dauer-Link:** erzeugt einen langlebigen,
  raterbaren Zugang zu personenbezogenen Daten und damit deutlich mehr Datenschutzlast als eine
  einmalige Mail.
- **Eltern-Login:** widerspricht dem bewusst minimalen Auth-Modell (genau eine Organizer-Identität,
  Eltern anonym per Token) und wäre für einen bloßen Beleg unverhältnismäßig.
