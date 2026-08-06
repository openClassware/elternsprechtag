# ADR 0001: Pflicht-E-Mail an der Buchung für Absage-Benachrichtigungen

Status: akzeptiert — Zweckbindung erweitert durch
[ADR 0002](0002-zweckerweiterung-eltern-email-buchungsbestaetigung.md)
Datum: 2026-07-19

> **Hinweis:** Die unten festgeschriebene Zweckbindung („ausschließlich der Benachrichtigung über
> Änderungen an genau dem gebuchten Sprechtag") gilt in dieser Form nicht mehr. Die Adresse dient
> zusätzlich der Bestätigung der eigenen Buchung — siehe
> [ADR 0002](0002-zweckerweiterung-eltern-email-buchungsbestaetigung.md). Die Entscheidung selbst
> (Pflicht-E-Mail, denormalisiert an der `Buchung`) bleibt unverändert gültig.

## Kontext

Das Auth-Modell ist bewusst minimal: Es gibt genau eine Organizer-Identität, und **Eltern
greifen anonym über den Access-Token-Link zu** (`Sprechtag.accessToken`). Es gibt keine
Lehrer-Logins, keine DB-Accounts und keinen Self-Service — siehe `docs/arc/ARCHITECTURE.md`
(Abschnitt „Auth") und `CLAUDE.md`. Eine `Buchung` speichert bislang nur `elternName`,
`schuelerName` und eine optionale `notiz`, aber **keinen Kontaktweg**.

Sagt der Organizer einen bereits veröffentlichten Sprechtag ab (z. B. wegen Erkrankung von
Lehrkräften), müssen die Eltern mit gebuchten Terminen davon erfahren, **bevor** sie zur Schule
fahren. Ohne gespeicherten Kontakt ist keine aktive Benachrichtigung möglich. Ein reines
„Pull"-Modell (Eltern sehen die Absage nur, wenn sie ihren Link erneut öffnen) erreicht genau
im kritischen, kurzfristigen Fall zu wenige.

## Entscheidung

Der Eltern-Buchungsflow erhält ein **Pflichtfeld E-Mail**. Die Adresse wird denormalisiert an
jeder `Buchung` gespeichert (keine neue `Eltern`-Entity). Sie ist **zweckgebunden**: Sie dient
ausschließlich der Benachrichtigung über Änderungen an genau dem gebuchten Sprechtag — in dieser
Iteration konkret der Absage des gesamten Sprechtags.

Damit wird das Anonymitätsprinzip **bewusst und begrenzt aufgeweicht**: Eine Buchung ist künftig
einer erreichbaren E-Mail-Adresse zuordenbar. Es bleibt beim Rest des minimalen Auth-Modells
(kein Eltern-Login, keine Accounts, weiterhin Token-Zugang).

## Begründung

- **Push schlägt Pull im Notfall.** Bei kurzfristiger Absage ist aktive Benachrichtigung nötig;
  ein Pull-Modell erreicht zu wenige.
- **E-Mail statt SMS**, weil an Schulen etabliert, kostenlos und ohne Drittanbieter-Vertrag; die
  Zustellschwäche gegenüber SMS wird akzeptiert.
- **Pflicht statt optional**, weil eine optionale Adresse genau im Absagefall wieder eine
  Erreichbarkeitslücke reißt — der Zweck von Push wäre unterlaufen.
- **Denormalisiert an der `Buchung`**, weil es kein `Eltern`-Konzept gibt und ein Submit ohnehin
  N Buchungen mit derselben Adresse erzeugt. Zielgenaue Empfänger-Queries („alle aktiven
  Buchungen eines Sprechtags") sind so trivial — besser als ein separates Abo-/Newsletter-Konstrukt,
  das dieselbe Lücke nur verschiebt.

## Konsequenzen

- **Positiv:** Betroffene Eltern sind bei einer Sprechtag-Absage zielgenau und automatisch
  erreichbar. Die Empfänger-Ermittlung ist eine einfache Query über aktive Buchungen.
- **Negativ / Kosten:**
  - Höhere Buchungshürde (ein Pflichtfeld mehr).
  - **Datenschutz:** personenbezogene Kontaktdaten werden erhoben und gespeichert; Zweckbindung
    (nur Benachrichtigung zu diesem Sprechtag) und eine angemessene Löschung/Aufbewahrung müssen
    beachtet werden.
  - Teilweiser Bruch mit dem dokumentierten Anonymitätsprinzip — diese ADR ist die bewusste
    Ausnahme, nicht ein Einfallstor für weitere Account-/Login-Funktionen.
- **Abgrenzung:** Diese Entscheidung rechtfertigt **nur** die Kontakt-E-Mail zur
  Benachrichtigung. Lehrer-Logins, Eltern-Accounts oder Multi-Organizer bleiben unberührt und
  wären eigene Entscheidungen.

## Alternativen (verworfen)

- **Pull-Modell** (Absage nur beim erneuten Öffnen des Links sichtbar): erreicht den
  kurzfristigen Notfall nicht zuverlässig.
- **SMS/Push-Benachrichtigung:** kostenpflichtiger Gateway, Nummernpflege, höhere
  Datenschutzlast — für ein Schulprojekt überzogen.
- **Optionale E-Mail** oder **separates Newsletter-/Abo-Modell:** lässt eine Gruppe im Absagefall
  unerreichbar bzw. verschiebt dieselbe Lücke in eine zweite Datenstruktur.
