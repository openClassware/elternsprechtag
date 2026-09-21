# ADR 0006: Zweckerweiterung der Eltern-E-Mail-Adresse auf die Erinnerung

Status: akzeptiert
Datum: 2026-09-21

## Kontext

[ADR 0001](0001-eltern-email-pflicht-fuer-absage-benachrichtigung.md) hat die Pflicht-E-Mail an
der `Buchung` eingeführt und für die Absage-Benachrichtigung zweckgebunden.
[ADR 0002](0002-zweckerweiterung-eltern-email-buchungsbestaetigung.md) hat den Zweck um die
Bestätigung der eigenen Buchung erweitert — beide Male ausdrücklich **eng gefasst**: „Jede weitere
Verwendung — Werbung, Erinnerungen, sprechtagsübergreifende Anschreiben, Weitergabe an
Lehrkräfte — ist davon **nicht** gedeckt und wäre eine eigene Entscheidung."

[`docs/arc/ABDECKUNG.md`](../arc/ABDECKUNG.md) stuft in Phase 4 „Erinnerung vor dem Sprechtag" als
**muss** ein: ein täglicher Scheduler soll den Eltern kurz vor dem gebuchten Termin — zu einem am
Sprechtag wählbaren Vorlauf ([#106](https://github.com/openClassware/elternsprechtag/issues/106))
— eine Erinnerung schicken. Anders als Absage und Bestätigung meldet diese Mail **keine
Änderung** an der Buchung; sie ist ein reiner Hinweis auf einen bereits bekannten, unveränderten
Termin. Das ist genau die dritte Verwendung, die ADR 0002 ausdrücklich offengelassen hat, und
braucht damit eine eigene Entscheidung, bevor der Versand ([#107](https://github.com/openClassware/elternsprechtag/issues/107))
gebaut wird.

Derselbe tägliche Scheduler trägt laut ABDECKUNG.md perspektivisch auch den automatischen
Abschluss und — nach Ablauf der Aufbewahrungsfrist — die Anonymisierung der Elterndaten. Die
Anonymisierung beendet die Zweckbindung, sie stellt sie nicht in Frage: Ab dem Zeitpunkt der
Anonymisierung ist keine Adresse mehr vorhanden, an die erinnert werden könnte.

## Entscheidung

Der Verwendungszweck der an der `Buchung` gespeicherten E-Mail-Adresse wird ein drittes Mal
erweitert:

- **bisher:** Benachrichtigung über Änderungen an genau diesem Sprechtag (Absage) sowie
  Bestätigung der eigenen Buchung unmittelbar nach dem Absenden,
- **künftig:** zusätzlich die **Erinnerung an die eigene, unveränderte Buchung** zum am
  Sprechtag gewählten Vorlauf vor dem Termin.

ADR 0001 und ADR 0002 bleiben als historische Entscheidungen bestehen; die Pflicht zur Angabe, die
denormalisierte Speicherung an der `Buchung` und das übrige minimale Auth-Modell bleiben
unverändert. Es ändert sich **nur** der Zweck, für den die bereits erhobene Adresse verwendet
wird. Der Helper-Text am E-Mail-Feld nennt künftig alle drei Zwecke.

Die Erinnerung entfällt für Sprechtage im Zustand `ABGESAGT` und für stornierte Buchungen — wer
nicht mehr kommt, bekommt keine Erinnerung an einen Termin, der nicht stattfindet.

## Begründung

- **Der Vorlauf ist heute der schwächste Beleg.** Die Bestätigungsmail nach ADR 0002 liegt
  Wochen vor dem Termin; wer sie nicht abgelegt hat, hat am Vortag nichts mehr in der Hand außer
  einem Anruf im Sekretariat.
- **Der Kanal ist bereits da.** Die Adresse ist Pflichtfeld, sie liegt an der Buchung, und die
  Versand-Infrastruktur existiert. Ein dritter Zweck kostet keine zusätzliche Datenerhebung.
- **Derselbe Empfängerkreis, dieselbe Buchung.** Erinnerung, Bestätigung und Absage betreffen
  dieselbe Person zu derselben Buchung. Der erweiterte Zweck bleibt eng am ursprünglichen und
  öffnet die Adresse nicht für Werbung, Serienmails oder sprechtagsübergreifende Kommunikation.
- **Der Organizer-Knopf ist bewusst verworfen** ([#97](https://github.com/openClassware/elternsprechtag/issues/97)):
  Wer den Sprechtag Wochen vorher vorbereitet hat, denkt am Vortag nicht mehr daran, ihn
  auszulösen. Ohne automatischen Versand über genau diese Adresse bliebe der Fall unabgedeckt.

## Datenschutz

- Es werden **keine zusätzlichen Daten erhoben**. Weder ein neues Feld, noch eine neue Entity,
  noch eine Migration — es erweitert sich ausschließlich der Verwendungszweck der bereits nach
  ADR 0001 erhobenen Adresse.
- Der erweiterte Zweck bleibt **eng gefasst**: Erinnerung, Bestätigung und
  Änderungsbenachrichtigung zu genau der Buchung, für die die Adresse angegeben wurde. Jede
  weitere Verwendung — Werbung, sprechtagsübergreifende Anschreiben, Weitergabe an Lehrkräfte —
  ist davon **nicht** gedeckt und wäre eine eigene Entscheidung.
- Die Mail enthält personenbezogene Daten (Kind, Klasse, Lehrkräfte, Termine) und geht
  unverschlüsselt per SMTP an die angegebene Adresse — derselbe Preis wie bei Absage und
  Bestätigung, hier zum dritten Mal akzeptiert.
- **Die Zweckangabe am Formular** wird mitgeführt: Wer die Adresse eingibt, liest dort, dass sie
  für Bestätigung, Erinnerung *und* Absage verwendet wird.
- **Aufbewahrungs- und Löschfristen bleiben durch diese ADR unverändert.** Sie sind mit der
  Anonymisierung aus Phase 6 des Maßstabs bereits entschieden (Aufbewahrungsfrist ab
  Sprechtag-Ende, danach Anonymisierung). Diese ADR ändert an der Frist nichts; sie stellt nur
  fest, dass die Anonymisierung die hier erweiterte Zweckbindung **beendet**, nicht in Frage
  stellt — nach der Anonymisierung existiert keine Adresse mehr, an die erinnert werden könnte.
- **Kein Opt-out aus der Erinnerung allein.** Wie bei Bestätigung und Absage gibt es keine
  Feinsteuerung einzelner Zwecke; wer eine Buchung mit E-Mail-Adresse abgibt, erhält alle drei.
  Ein Opt-out wäre eine eigene Entscheidung und ist hier nicht getroffen.

## Konsequenzen

- **Positiv:** Eltern werden kurz vor dem Termin an ihre Buchung erinnert, ohne dass der
  Organizer dafür tätig werden muss; die Terminwahrnehmung sollte sich verbessern.
- **Negativ / Kosten:**
  - Eine misslungene Zustellung ist für die Eltern nicht sichtbar; der Versand bleibt
    best-effort, wie bei Absage und Bestätigung.
  - Jede Buchung mit gewähltem Erinnerungs-Vorlauf erzeugt einen dritten, zeitgesteuerten
    Mailversand.
  - Drei Anlässe teilen sich nun eine Adresse; ein vierter Anlass muss erneut geprüft werden, ob
    er vom hier festgehaltenen engen Zweck gedeckt ist.
- **Abgrenzung — was bewusst *nicht* mitkommt:**
  - **kein Organizer-Knopf** für die Erinnerung — sie läuft ausschließlich automatisch über den
    täglichen Scheduler,
  - **kein Opt-out** aus der Erinnerung für einzelne Buchungen,
  - **kein Storno-Link, keine Buchungsansicht, kein Eltern-Login** — unverändert gegenüber ADR
    0001 und ADR 0002,
  - **kein Nachholen** verpasster Erinnerungsläufe: Eine Erinnerung, die am Sprechtagmorgen
    eintrifft, weil der Server nachgeholt hat, hilft niemandem und wird nicht mehr versendet.

## Alternativen (verworfen)

- **Beim Zustand von ADR 0002 bleiben** (keine Erinnerung): Der einzige Beleg bliebe die
  Bestätigungsmail von Wochen zuvor; der in ABDECKUNG.md als `muss` eingestufte Fall bliebe
  unabgedeckt.
- **Erinnerung per Organizer-Knopf statt automatisch**: In Phase 4 des Maßstabs ausdrücklich
  verworfen — wer den Sprechtag früh vorbereitet, hat am Vortag anderes zu tun als an einen
  Knopf zu denken.
- **Eigene, zweite Adresse nur für Erinnerungen** (getrennte Zweckbindung): doppelte Eingabe für
  denselben Empfänger, doppelte Datenhaltung — löst kein reales Problem.
- **Opt-out-Feld für die Erinnerung**: zusätzliche UI und ein zusätzlicher Zustand für einen Fall,
  den heute niemand verlangt hat; kann bei Bedarf nachgezogen werden.
