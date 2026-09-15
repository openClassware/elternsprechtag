# Domänen-Kontext — Sprechtag

Das Vokabular des **Ablaufs**: anlegen, veröffentlichen, buchen, auswerten. Der erste der beiden
Kontexte des Projekts — die Übersicht steht in [`CONTEXT-MAP.md`](../../../CONTEXT-MAP.md), die
Stammdaten (Lehrkraft, Klasse, Fach, Lehrauftrag) im zweiten:
[`../schulorganisation/CONTEXT.md`](../schulorganisation/CONTEXT.md).

**Was hier _nicht_ steht:** Architekturregeln (Schichtung, DTO-Grenze, Auth-Modell, i18n, Tests)
stehen in [`docs/arc/ARCHITECTURE.md`](../../arc/ARCHITECTURE.md), die Kurzfassung in
[`CLAUDE.md`](../../../CLAUDE.md), einzelne Entscheidungen in [`docs/adr/`](../../adr/). Von hier
wird darauf verwiesen, nicht wiederholt.

Ergänzend visuell: [`docs/arc/domain.puml`](../../arc/domain.puml) (Domänen-Klassendiagramm).
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

**Lehrkraft, Klasse, Fach und Lehrauftrag gehören nicht diesem Kontext.** Sie sind Stammdaten der
[Schulorganisation](../schulorganisation/CONTEXT.md); dieser Kontext liest sie und schreibt sie nie.
Was er von ihnen kennt, sind ihre Ids und die Namen, die er anzeigt.

## Glossar

### Sprechtag

Das Ereignis selbst: ein Datum mit Zeitfenster (Start-/Endzeit), einer **Slot-Dauer** in Minuten,
optional Ort und Hinweistext, dem verpflichtenden **Schulkontakt**, den teilnehmenden Klassen und einem
**Access-Token** für den Eltern-Link.

Der Sprechtag setzt den **Rahmen**: Aus Zeitfenster, Slot-Dauer und teilnehmenden Klassen entstehen
beim Veröffentlichen die Termine. Danach steht der Rahmen fest — Datum, Zeitfenster, Slot-Dauer und
Klassenliste sind ab `VEROEFFENTLICHT` unveränderlich, weil die bereits erzeugten Termine sonst aus
ihm herausfielen. Titel, Ort und Hinweistext bleiben änderbar. Ein Sprechtag, der so nicht mehr
stattfinden kann, wird **abgesagt**, nicht umgeschrieben.

Statuswerte (`SprechtagStatus`) und die erlaubten Übergänge — durchgesetzt vom Aggregat, nicht
vom Aufrufer:

| Status           | Bedeutung                                                                 |
|------------------|---------------------------------------------------------------------------|
| `ENTWURF`        | In Vorbereitung, für Eltern nicht erreichbar. → `VEROEFFENTLICHT`          |
| `VEROEFFENTLICHT`| Freigegeben, Eltern können buchen. → `ABGESCHLOSSEN`, `ABGESAGT`           |
| `ABGESCHLOSSEN`  | Vorbei bzw. beendet; Endzustand                                           |
| `ABGESAGT`       | Findet nicht statt; Endzustand, löst die Absage-Benachrichtigung aus       |

Sprachgebrauch: **Sprechtag** ist der Fachbegriff im Modell, **Elternsprechtag** der Produktname
(und der Name der Eltern-Ansicht). Im Code und in Issues bitte **Sprechtag**.

### Schulkontakt

Wie die Eltern die Schule erreichen: ein einzelnes, mehrzeiliges **Freitextfeld** am Sprechtag —
bewusst nicht in Ansprechpartner, Telefon und E-Mail zerlegt, denn Kontakt aufnehmen ist mehr als
anrufen (Sekretariatszeiten, eine Durchwahl mit Bedingung, „kommen Sie vorbei"). Er ist die
Voraussetzung dafür, dass alles, was die App **nicht** kann, an der Schule landet: Umbuchen,
Stornieren, Rückfragen zu einer Absage.

Deshalb ist er **Pflicht ab dem Entwurf**: Ein Sprechtag ohne Schulkontakt ist unvollständig, egal
in welchem Status. Die Regel steht im Wert `Schulkontakt` der Domäne und zusätzlich als `not null` plus
Check-Constraint gegen den leeren String in der Datenbank. Gezeigt wird er in der Bestätigungs- und
in der Absagemail — bewusst **nicht** auf der Buchungsseite: Dort soll nichts von der Terminwahl
ablenken. Die Eltern haben ihn schwarz auf weiß, sobald sie ihn brauchen.

Nicht zu verwechseln mit der **E-Mail der Eltern** an der Buchung: Das ist die Adresse, an die
*wir* schreiben. Der Schulkontakt ist die Richtung zurück.

### Buchungsziel

Das, worauf sich eine Buchung richtet: ein **Lehrauftrag** — also die konkrete Kombination aus
Lehrkraft, Klasse und Fach. Eltern buchen nicht abstrakt „bei einer Lehrkraft", sondern zu einem
bestimmten Fach in der Klasse ihres Kindes. Der Lehrauftrag ist damit auch die Einheit, in der die
Eltern-Ansicht auswählt (eine Auswahl pro Lehrauftrag).

Der Lehrauftrag selbst gehört der [Schulorganisation](../schulorganisation/CONTEXT.md). Was diesen
Kontext angeht, ist, was er mit ihm tut:

Eine Buchung **kopiert** ihr Buchungsziel, statt darauf zu verweisen: Lehrkraft, Klasse und Fach
werden festgehalten, wie sie im Moment der Buchung galten; mitgeführt wird nur noch die Id des
Lehrauftrags, aus dem sie stammen. Stammdaten ändern sich zwischen Schuljahren — eine vergangene
Auswertung muss trotzdem lesbar bleiben, auch wenn der Lehrauftrag inzwischen stillgelegt ist.

Sprachgebrauch: **Buchungsziel** ist das, worauf sich eine Buchung richtet. Das ist ein Lehrauftrag,
und nach der Buchung dessen festgehaltener Stand.

### Termin

Ein **materialisierter** Zeit-Slot einer Lehrkraft an einem Sprechtag: Start- und Endzeitpunkt,
Lehrkraft, Sprechtag. Termine sind echte Datensätze, keine berechneten Zeitfenster.

Ein Termin ist genau dann **buchbar**, wenn er nicht entfällt und keine aktive Buchung trägt:

| Zustand        | Bedeutung                                                                   |
|----------------|-----------------------------------------------------------------------------|
| **buchbar**    | Wird den Eltern zur Wahl angeboten                                          |
| **vergeben**   | Trägt eine aktive Buchung                                                   |
| **entfällt**   | Vom Organizer zurückgezogen — die Lehrkraft steht in diesem Slot nicht bereit |

**Vergeben ist kein eigener Zustand, sondern eine Folge.** Ob ein Termin belegt ist, ergibt sich aus
seinen Buchungen und wird nicht daneben geführt — sonst gäbe es zwei Wahrheiten, die auseinanderlaufen
können. Nur **entfällt** ist eine eigene Angabe, denn das ist eine Entscheidung des Organizers.

*Noch nicht gebaut:* **entfällt** ist beschlossen
([`ABDECKUNG.md`](../../arc/ABDECKUNG.md) Z. 230), aber noch nicht umgesetzt.

**Materialisierung:** Beim Veröffentlichen erzeugt der Sprechtag für jede teilnehmende Lehrkraft
× jeden Zeit-Slot einen freien Termin. Jede Lehrkraft bekommt **einen** Slot-Satz, geteilt über
all ihre Fächer und Klassen an diesem Sprechtag — sie kann pro Zeitfenster nur ein Gespräch
führen. Teilnehmend ist, wer über einen Lehrauftrag an einer der Sprechtag-Klassen hängt.

Sprachgebrauch: **Termin** ist der Datensatz, **Slot** dasselbe aus Sicht der Auswahl-UI
(`SlotOption`, `slotInMinutes`). Beides ist in Ordnung; „Zeitfenster" meint dagegen die
Öffnungszeit des ganzen Sprechtags, nicht den einzelnen Termin.

### Buchung

Die Buchung eines Termins durch eine **Familie**, gerichtet auf ein **Buchungsziel**. Trägt die
Angaben der Familie, das festgehaltene Buchungsziel und eine optionale Notiz an die Lehrkraft.

| Status       | Bedeutung                                                  |
|--------------|------------------------------------------------------------|
| `ZUGESAGT`   | Aktive Buchung; ihr Termin gilt als vergeben               |
| `STORNIERT`  | Zurückgenommen; der Termin ist wieder buchbar              |

Sprachgebrauch: Eine **Buchung wird storniert**, ein **Sprechtag wird abgesagt**. Beides sind
verschiedene Vorgänge und heißen deshalb verschieden — auch in Statuswerten und Methodennamen.

*Noch nicht gebaut:* `STORNIERT` ist im Modell vorgesehen, wird bislang von nichts ausgelöst
([`ABDECKUNG.md`](../../arc/ABDECKUNG.md) Z. 166).

Eigenschaften, die zur Domäne gehören (nicht nur zur Technik):

- **Alles oder nichts:** Ein Eltern-Submit umfasst mehrere Wünsche (mehrere Lehrkräfte an einem
  Nachmittag) und wird als Einheit gebucht. Ist auch nur ein Slot inzwischen vergeben, kommt
  **keine** der Buchungen zustande und die Eltern wählen neu.
- **Kein Zeitkonflikt:** Zwei Buchungen derselben Auswahl dürfen nicht auf dieselbe Uhrzeit
  fallen — man kann nicht an zwei Tischen gleichzeitig sitzen. Die Regel wird heute beim
  Auswählen durchgesetzt (`BookingSession`), nicht beim Speichern.
- **Die Buchung hält alles fest, was sie bezeugt.** Weder die Familie noch das Buchungsziel sind
  Verweise: Beides steht an der Buchung selbst, mit dem Stand vom Buchungszeitpunkt. Eine Buchung
  bleibt dadurch vollständig lesbar, auch wenn sich die Stammdaten später ändern.

### Familie

Wer bucht und für wen: Name des buchenden Elternteils, Name des Kindes und die Pflicht-**E-Mail der
Eltern**. Die Familie ist die Gegenseite des Gesprächs — die Schule spricht mit ihr, nicht mit einem
Benutzerkonto.

**Es gibt kein Eltern-Aggregat und kein Familien-Aggregat.** Eine Familie ist kein eigener Datensatz,
den man verwalten könnte, sondern schlicht die Angaben an einer Buchung. Zwei Buchungen derselben
Familie wissen nichts voneinander; erkannt wird sie höchstens an der E-Mail-Adresse.

Sprachgebrauch: **Familie** für die buchende Seite. Nicht **Anmeldung** — das Wort ist im Projekt
doppelt vergeben (das Organizer-Login heißt so, und der **Anmeldeschluss** meint den Buchungsschluss
der Eltern). „Eltern" bleibt richtig, wo wirklich die Erwachsenen gemeint sind (Eltern-Ansicht,
E-Mail der Eltern, Eltern-Zugang).

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
  ([ADR 0001](../../adr/0001-eltern-email-pflicht-fuer-absage-benachrichtigung.md)).
- **Buchungsbestätigung** — direkt nach erfolgreicher Buchung, als Beleg über Datum, Uhrzeiten
  und Lehrkräfte
  ([ADR 0002](../../adr/0002-zweckerweiterung-eltern-email-buchungsbestaetigung.md)).

Die E-Mail-Adresse ist **zweckgebunden** auf genau diese beiden Fälle; eine weitere Nutzung wäre
eine neue Entscheidung und braucht einen ADR.

## Begriffe, die wir nicht benutzen

| Nicht                            | Sondern                                                        |
|----------------------------------|----------------------------------------------------------------|
| Lehrer                           | **Lehrkraft** — siehe [Schulorganisation](../schulorganisation/CONTEXT.md) |
| Admin, Sekretariat               | **Organizer**                                                  |
| Einladungscode, Passwort         | **Access-Token** / **Zugangs-Link**                            |
| Elternsprechtag (als Aggregat)   | **Sprechtag**                                                  |
| Zeitfenster (für einen Termin)   | **Termin** bzw. **Slot**                                       |
| Konto, Account (für Eltern)      | gibt es nicht — anonymer Zugang per Token                      |
| Anmeldung (für die Buchungsdaten)| **Familie** — „Anmeldung" ist das Organizer-Login, „Anmeldeschluss" der Buchungsschluss |
| Buchung absagen                  | Buchung **stornieren** (abgesagt wird der Sprechtag)           |
| Belegt (als eigener Zustand)     | **vergeben** — ergibt sich aus den Buchungen, wird nicht geführt |
