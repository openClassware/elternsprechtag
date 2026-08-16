# Domänen-Kontext — Elternsprechtag

Einstiegspunkt ins **Domänenwissen**: das Vokabular des Projekts und was die Begriffe fachlich
bedeuten. Wer hier nachschlägt, soll die Sprache des Projekts benutzen statt eigene zu erfinden.

**Was hier _nicht_ steht:** Architekturregeln (Schichtung, DTO-Grenze, Auth-Modell, i18n, Tests)
stehen in [`docs/arc/ARCHITECTURE.md`](docs/arc/ARCHITECTURE.md), die Kurzfassung in
[`CLAUDE.md`](CLAUDE.md), einzelne Entscheidungen in [`docs/adr/`](docs/adr/). Von hier wird
darauf verwiesen, nicht wiederholt.

Ergänzend visuell: [`docs/arc/domain.puml`](docs/arc/domain.puml) (Domänen-Klassendiagramm).
Bei Abweichungen zwischen Diagramm und Code gilt der **Code**.

## Worum es geht

Eine Schule führt einen **Elternsprechtag** durch: An einem Nachmittag stehen Lehrkräfte in
festen Zeitfenstern für Gespräche zur Verfügung, Eltern buchen darin ihre Gesprächstermine
selbst. Die App bildet genau diesen Ablauf ab — vom Anlegen des Sprechtags durch den Organizer
bis zur Buchungsbestätigung der Eltern.

Die Kernkette lautet:

**Sprechtag → Termin → Buchung**, mit dem **Lehrauftrag** als Buchungsziel.

Ein Sprechtag bietet Termine an; eine Buchung belegt genau einen Termin und richtet sich dabei
auf einen Lehrauftrag — also auf die konkrete Kombination aus Lehrkraft, Klasse und Fach, über
die gesprochen werden soll.

## Glossar

### Sprechtag

Das Ereignis selbst: ein Datum mit Zeitfenster (Start-/Endzeit), einer **Slot-Dauer** in Minuten,
optional Ort und Hinweistext, den teilnehmenden Klassen und einem **Access-Token** für den
Eltern-Link. Der Sprechtag ist der Aggregatseinstieg — alles Weitere hängt an ihm.

Statuswerte (`SprechtagStatusEnum`) und die erlaubten Übergänge:

| Status           | Bedeutung                                                                 |
|------------------|---------------------------------------------------------------------------|
| `ENTWURF`        | In Vorbereitung, für Eltern nicht erreichbar. → `VEROEFFENTLICHT`          |
| `VEROEFFENTLICHT`| Freigegeben, Eltern können buchen. → `ABGESCHLOSSEN`, `ABGESAGT`           |
| `ABGESCHLOSSEN`  | Vorbei bzw. beendet; Endzustand                                           |
| `ABGESAGT`       | Findet nicht statt; Endzustand, löst die Absage-Benachrichtigung aus       |

Sprachgebrauch: **Sprechtag** ist der Fachbegriff im Modell, **Elternsprechtag** der Produktname
(und der Name der Eltern-Ansicht). Im Code und in Issues bitte **Sprechtag**.

### Klasse

Eine Schulklasse (`10a`). Ein Sprechtag umfasst eine oder mehrere Klassen — daraus leitet sich
ab, welche Lehrkräfte teilnehmen und welche Gespräche buchbar sind.

### Lehrkraft (Entity `Lehrer`)

Die Person, mit der gesprochen wird: Vorname, Nachname, Kürzel.

Sprachgebrauch: Nach außen — in allen UI-Texten — heißt sie **Lehrkraft**. Die Entity heißt aus
historischen Gründen `Lehrer`. **Bevorzugter Begriff in Prosa, Issues und neuen Bezeichnern:
Lehrkraft** (`LehrkraftOption`, `LehrkraftPlan`). „Lehrer" bleibt dort stehen, wo es den
Entity-/Spaltennamen meint.

Lehrkräfte haben **kein Login** — sie sind Stammdaten, keine Benutzer. Siehe Abschnitt „Auth" in
[`ARCHITECTURE.md`](docs/arc/ARCHITECTURE.md).

### Fach

Das Unterrichtsfach (Name + Kürzel). Nur als Teil des Lehrauftrags relevant.

### Lehrauftrag

Die Verknüpfung **Lehrkraft × Klasse × Fach** — „Frau X unterrichtet Mathematik in der 10a".

Der Lehrauftrag ist das **Ziel einer Buchung**: Eltern buchen nicht abstrakt „bei einer
Lehrkraft", sondern zu einem bestimmten Fach in der Klasse ihres Kindes. Er ist damit auch die
Einheit, in der die Eltern-Ansicht auswählt (eine Auswahl pro Lehrauftrag).

### Termin

Ein **materialisierter** Zeit-Slot einer Lehrkraft an einem Sprechtag: Start- und Endzeitpunkt,
Status, Lehrkraft, Sprechtag. Termine sind echte Datensätze, keine berechneten Zeitfenster.

| Status   | Bedeutung                                   |
|----------|---------------------------------------------|
| `FREI`   | Buchbar                                     |
| `BELEGT` | Durch eine aktive Buchung vergeben          |

**Materialisierung:** Beim Veröffentlichen erzeugt der Sprechtag für jede teilnehmende Lehrkraft
× jeden Zeit-Slot einen freien Termin. Jede Lehrkraft bekommt **einen** Slot-Satz, geteilt über
all ihre Fächer und Klassen an diesem Sprechtag — sie kann pro Zeitfenster nur ein Gespräch
führen. Teilnehmend ist, wer über einen Lehrauftrag an einer der Sprechtag-Klassen hängt.

Sprachgebrauch: **Termin** ist der Datensatz, **Slot** dasselbe aus Sicht der Auswahl-UI
(`SlotOption`, `slotInMinutes`). Beides ist in Ordnung; „Zeitfenster" meint dagegen die
Öffnungszeit des ganzen Sprechtags, nicht den einzelnen Termin.

### Buchung

Die Eltern-Buchung eines Termins gegen einen Lehrauftrag. Trägt Elternname, Schülername, die
Pflicht-**E-Mail der Eltern** und eine optionale Notiz.

| Status      | Bedeutung                                                   |
|-------------|-------------------------------------------------------------|
| `ZUGESAGT`  | Aktive Buchung; belegt ihren Termin                         |
| `ABGESAGT`  | Storniert; im Modell vorgesehen, bislang nicht ausgelöst     |

Eigenschaften, die zur Domäne gehören (nicht nur zur Technik):

- **Alles oder nichts:** Ein Eltern-Submit umfasst mehrere Wünsche (mehrere Lehrkräfte an einem
  Nachmittag) und wird als Einheit gebucht. Ist auch nur ein Slot inzwischen vergeben, kommt
  **keine** der Buchungen zustande und die Eltern wählen neu.
- **Kein Zeitkonflikt:** Zwei Buchungen derselben Auswahl dürfen nicht auf dieselbe Uhrzeit
  fallen — man kann nicht an zwei Tischen gleichzeitig sitzen. Die Regel wird heute beim
  Auswählen durchgesetzt (`BookingSession`), nicht beim Speichern.
- **Es gibt keine Eltern-Entity.** Namen und E-Mail hängen denormalisiert an der Buchung.

### Organizer

Die einzige angemeldete Rolle: legt Sprechtage an, pflegt sie, veröffentlicht, schließt ab oder
sagt ab, teilt den Eltern-Link und sieht die Auswertung. Genau **eine** Identität, in-memory
konfiguriert — bewusst kein Multi-User-Modell (siehe „Auth" in `ARCHITECTURE.md`).

Sprachgebrauch: **Organizer**, nicht „Admin" und nicht „Sekretariat".

### Eltern-Zugang per Access-Token

Eltern melden sich **nicht** an. Jeder Sprechtag trägt ein `accessToken`; daraus entsteht der
**Zugangs-Link** (`/elternsprechtag/{token}`), den der Organizer verteilt. Wer den Link hat,
darf buchen — das Token ist der gesamte Zugangsschutz, und das ist eine bewusste Entscheidung.

Der Link führt je nach Sprechtag-Status zu einem von drei Ergebnissen: **buchbar** (nur bei
`VEROEFFENTLICHT`), **abgesagt** oder **nicht verfügbar** (unbekanntes Token, Entwurf,
abgeschlossen).

Sprachgebrauch: **Access-Token** (Feldname) bzw. **Zugangs-Link** (was die Eltern bekommen).
Nicht „Einladungscode", nicht „Passwort".

### Auswertung

Die Organizer-Sicht auf einen Sprechtag: pro Lehrkraft der Terminplan mit den gebuchten
Gesprächen — die Liste, die am Sprechtag tatsächlich benutzt wird.

### Benachrichtigungen

Zwei E-Mails an die bei der Buchung hinterlegte Adresse:

- **Absage-Benachrichtigung** — der Organizer sagt einen veröffentlichten Sprechtag ab, alle
  Eltern mit zugesagten Buchungen werden informiert
  ([ADR 0001](docs/adr/0001-eltern-email-pflicht-fuer-absage-benachrichtigung.md)).
- **Buchungsbestätigung** — direkt nach erfolgreicher Buchung, als Beleg über Datum, Uhrzeiten
  und Lehrkräfte
  ([ADR 0002](docs/adr/0002-zweckerweiterung-eltern-email-buchungsbestaetigung.md)).

Die E-Mail-Adresse ist **zweckgebunden** auf genau diese beiden Fälle; eine weitere Nutzung wäre
eine neue Entscheidung und braucht einen ADR.

## Begriffe, die wir nicht benutzen

| Nicht                          | Sondern                                                |
|--------------------------------|--------------------------------------------------------|
| Lehrer (in Prosa/UI)           | **Lehrkraft** (Entity heißt weiterhin `Lehrer`)         |
| Admin, Sekretariat             | **Organizer**                                          |
| Einladungscode, Passwort       | **Access-Token** / **Zugangs-Link**                    |
| Elternsprechtag (als Entity)   | **Sprechtag**                                          |
| Zeitfenster (für einen Termin) | **Termin** bzw. **Slot**                               |
| Konto, Account (für Eltern)    | gibt es nicht — anonymer Zugang per Token              |
