# ADR 0002: Zweckerweiterung der Eltern-E-Mail-Adresse auf die Buchungsbestätigung

Status: akzeptiert
Datum: 2026-08-09

## Kontext

[ADR 0001](0001-eltern-email-pflicht-fuer-absage-benachrichtigung.md) hat die Pflicht-E-Mail an
der `Buchung` eingeführt und sie **zweckgebunden** erklärt: Sie dient „ausschließlich der
Benachrichtigung über Änderungen an genau dem gebuchten Sprechtag" — in der damaligen Iteration
konkret der Absage des gesamten Sprechtags.

Der Buchungsablauf der Eltern endet heute mit einer Bestätigungskarte im Browser. Schließen die
Eltern den Tab, bleibt **kein Beleg** zurück: Es gibt kein Eltern-Login, keine Buchungsansicht und
keinen zweiten Weg zur eigenen Buchung. Wer sich Datum, Uhrzeiten und Lehrkräfte nicht selbst
notiert hat, kann sie später nirgends nachsehen — bei mehreren gebuchten Terminen an einem
Nachmittag ist das genau die Information, die am Sprechtag gebraucht wird.

Zugleich gibt es heute keine Rückmeldung darüber, ob die eingegebene Adresse überhaupt stimmt.
Sie ist Pflichtfeld und der einzige Kanal, über den eine Absage die Eltern erreicht — ein
Tippfehler bleibt unbemerkt, bis es darauf ankommt.

Die naheliegende Lösung ist eine Bestätigungsmail an genau diese Adresse. Das ist ein anderer
Zweck als der in ADR 0001 festgeschriebene, und die dortige Zweckbindung gilt damit nicht mehr
unverändert.

## Entscheidung

Der Verwendungszweck der an der `Buchung` gespeicherten E-Mail-Adresse wird erweitert:

- **bisher:** ausschließlich Benachrichtigung über Änderungen an genau diesem Sprechtag
  (konkret: Absage),
- **künftig:** zusätzlich die **Bestätigung der eigenen Buchung** unmittelbar nach dem Absenden.

ADR 0001 bleibt als historische Entscheidung bestehen; die Pflicht zur Angabe, die
denormalisierte Speicherung an der `Buchung` und das übrige minimale Auth-Modell bleiben
unverändert. Es ändert sich **nur** der Zweck, für den die bereits erhobene Adresse verwendet
wird.

Die Bestätigungsmail ist ein **reiner Beleg** ohne jede Aktion. Weil die Adresse damit einen
zweiten Zweck trägt, nennt der Helper-Text am E-Mail-Feld künftig beide Zwecke — die Angabe am
Feld muss der Wirklichkeit entsprechen.

## Begründung

- **Ohne Beleg endet der Buchungsablauf spurlos.** Die Bestätigungskarte im Browser ist flüchtig;
  mit dem Schließen des Tabs ist die Information weg, und es gibt keinen zweiten Weg zur eigenen
  Buchung.
- **Der Kanal ist bereits da.** Die Adresse ist Pflichtfeld, sie liegt an der Buchung, und die
  Versand-Infrastruktur existiert. Ein zusätzlicher Zweck kostet keine zusätzliche Datenerhebung.
- **Die Mail deckt Tippfehler auf.** Eine falsch eingegebene Adresse fällt jetzt sofort auf statt
  erst im Absagefall — sie verbessert damit auch den in ADR 0001 begründeten ursprünglichen Zweck.
- **Zwei Zwecke, ein Empfängerkreis.** Bestätigung und Absage betreffen dieselbe Person zur
  derselben Buchung. Der erweiterte Zweck bleibt eng am ursprünglichen und öffnet die Adresse
  nicht für Werbung, Serienmails oder sprechtagsübergreifende Kommunikation.

## Datenschutz

- Es werden **keine zusätzlichen Daten erhoben**. Weder ein neues Feld, noch eine neue Entity,
  noch eine Migration — es erweitert sich ausschließlich der Verwendungszweck der bereits nach
  ADR 0001 erhobenen Adresse.
- Der erweiterte Zweck bleibt **eng gefasst**: Bestätigung und Änderungsbenachrichtigung zu genau
  der Buchung, für die die Adresse angegeben wurde. Jede weitere Verwendung — Werbung,
  Erinnerungen, sprechtagsübergreifende Anschreiben, Weitergabe an Lehrkräfte — ist davon **nicht**
  gedeckt und wäre eine eigene Entscheidung.
- Die Mail enthält personenbezogene Daten (Kind, Klasse, Lehrkräfte, Termine, eigene Notiz) und
  geht unverschlüsselt per SMTP an die angegebene Adresse. Das ist der Preis des Belegs und wird
  bewusst akzeptiert; deshalb enthält die Mail auch keinerlei Zugang zur Anwendung.
- Die **Zweckangabe am Formular** wird mitgeführt: Wer die Adresse eingibt, liest dort, dass sie
  für Bestätigung *und* Absage verwendet wird.
- **Aufbewahrungs- und Löschfristen bleiben offen.** ADR 0001 hat sie bereits als offenen Punkt
  vermerkt, und diese ADR löst ihn **nicht**. Der zweite Zweck ändert an der Dringlichkeit nichts,
  weil er keine zusätzlichen Daten und keine längere Speicherung verlangt — die Frage einer
  angemessenen Löschung der gespeicherten Adressen bleibt zu entscheiden.

## Konsequenzen

- **Positiv:** Eltern haben ihre Termine schriftlich vorliegen; Tippfehler in der Adresse fallen
  sofort auf; Rückfragen im Sekretariat werden weniger.
- **Negativ / Kosten:**
  - Eine misslungene Zustellung ist für die Eltern nicht sichtbar. Der Versand ist best-effort:
    Fehler werden protokolliert, die Buchung bleibt gültig, es gibt kein Retry.
  - Jede Buchung erzeugt jetzt Mailverkehr — bei falsch eingegebenen Adressen auch an Dritte.
  - Zwei Anlässe teilen sich eine Adresse; wer künftig einen dritten Anlass hinzufügen will, muss
    erneut prüfen, ob er vom hier festgehaltenen engen Zweck gedeckt ist.
- **Abgrenzung — was bewusst *nicht* mitkommt:**
  - **kein Storno-Link** und keine Selbstbedienung der Eltern in irgendeiner Form,
  - **keine Buchungsansicht** für Eltern,
  - **kein Eltern-Login** und keine Eltern-Accounts,
  - kein zusätzliches Token, keine neue Route, kein Link zurück in die Anwendung.

  Der Token-Zugang der Eltern und die einzelne Organizer-Identität bleiben unberührt. Diese ADR
  erweitert den Zweck einer bereits erhobenen Adresse — sie ist **kein** Einfallstor für ein
  Eltern-Konto.

## Alternativen (verworfen)

- **Beim Zustand von ADR 0001 bleiben** (keine Bestätigungsmail): lässt den Buchungsablauf ohne
  jede Spur für die Eltern enden und den Tippfehler bis zum Absagefall unbemerkt.
- **Beleg ohne Mail** — Druckansicht oder Download auf der Bestätigungsseite: erreicht nur, wer im
  richtigen Moment daran denkt, und deckt den Tippfehler in der Adresse nicht auf.
- **Bestätigung mit Storno-Link**: verlangt ein eigenes Buchungs-Token samt Sicherheitsbetrachtung
  und nähme dem Organisator die Kontrolle über den Plan. Änderungen laufen weiterhin über die
  Schule.
- **Eigene, zweite Adresse für Bestätigungen** (getrennte Zweckbindung): doppelte Eingabe für
  denselben Empfänger, doppelte Datenhaltung — löst kein reales Problem.
