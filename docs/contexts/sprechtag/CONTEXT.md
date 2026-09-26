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
| `VEROEFFENTLICHT`| Freigegeben, Eltern können buchen. → `ABGESAGT`, `ENTWURF` (nur ungebucht), `ABGESCHLOSSEN` (nur der Tagesjob) |
| `ABGESCHLOSSEN`  | Vorbei — festgestellt vom Tagesjob nach der Endzeit, nie von Hand; Endzustand |
| `ABGESAGT`       | Findet nicht statt; Endzustand, löst die Absage-Benachrichtigung aus       |

Sprachgebrauch: **Sprechtag** ist der Fachbegriff im Modell, **Elternsprechtag** der Produktname
(und der Name der Eltern-Ansicht). Im Code und in Issues bitte **Sprechtag**.

### Anmeldefrist und Anmeldeschluss

Zwei Begriffe, einer gespeichert, einer errechnet:

- **Anmeldefrist** — der Abstand „N Tage vor dem Sprechtag", den der Organizer einträgt und der am
  Sprechtag steht (`Anmeldefrist`). Pflichtfeld, vorbelegt mit **1** (Vortag), erlaubt sind 0
  (am Tag selbst) bis 28 — die Obergrenze fängt den Tippfehler 40 statt 4 ab.
- **Anmeldeschluss** — das Datum, das daraus folgt: Datum des Sprechtags minus Anmeldefrist. Er
  gilt **einschließlich**: „Anmeldung bis 19.03." heißt, Eltern buchen den ganzen 19.03. über.
  Tagesgranularität, keine Uhrzeit.

Der Anmeldeschluss schließt **nur den Elternlink** — beim Öffnen der Seite ebenso wie beim
Abschicken. Die Organizer-Strecke (**Nachtragen**) bleibt bis zum Abschluss offen: Wer am Tag
selbst anruft oder vor der Tür steht, bekommt über den Organizer noch einen Termin. Ob der Link
bucht, entscheidet genau eine Stelle, `Sprechtag.nimmtElternbuchungenAn(heute)`: veröffentlicht
**und** der Anmeldeschluss nicht vorbei.

Die Anmeldefrist gehört **nicht** zur Zeitstruktur — sie erzeugt keinen Termin und macht keine
Buchung ungültig. Deshalb bleibt sie wie der Erinnerungsvorlauf auch nach dem Veröffentlichen
änderbar, ausdrücklich auch, um eine abgelaufene Frist wieder zu öffnen; erst die Endzustände
`ABGESAGT` und `ABGESCHLOSSEN` frieren sie ein.

**Relativ statt absolut**, und zwar wegen des Duplizierens: Eine Kopie für das nächste Halbjahr
übernimmt den Abstand, und der passt zu jedem Datum, auf das der Organizer sie danach setzt. Ein
gespeichertes Datum wäre in der Kopie längst verstrichen — „tot geboren".

Sprachgebrauch: **Anmeldefrist** für den eingetragenen Abstand, **Anmeldeschluss** für das Datum.
Nicht „Deadline".

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
| `STORNIERT`  | Vom Organizer zurückgenommen; der Termin ist wieder buchbar. Endzustand |

Sprachgebrauch: Eine **Buchung wird storniert**, ein **Sprechtag wird abgesagt**. Beides sind
verschiedene Vorgänge und heißen deshalb verschieden — auch in Statuswerten und Methodennamen.

**Wer storniert:** ausschließlich der Organizer, aus der Auswertung heraus. Eltern rufen an; ein
Eltern-Storno bräuchte ein Token je Buchung und ist bewusst nicht vorgesehen (ADR 0002). Das Storno
gibt den Termin auf `FREI` zurück und ist der Endzustand der Buchung — ein zweites Storno derselben
Buchung scheitert. Der Datensatz bleibt erhalten, die Zeile verschwindet nur aus dem Plan.

**Das Storno benachrichtigt niemanden.** Keine Mail, kein Ereignis über die Kontextgrenze hinaus:
Der Anlass ist praktisch immer der Anruf der Familie; beim Tippfehler in der Adresse ginge eine Mail
erneut an einen Dritten, beim Löschverlangen wäre sie widersinnig. Der Fall, in dem die Familie
nichts weiß (Ausfall einer Lehrkraft), ist eine eigene Strecke mit eigenem Termin-Zustand.

Eigenschaften, die zur Domäne gehören (nicht nur zur Technik):

- **Alles oder nichts:** Ein Eltern-Submit umfasst mehrere Wünsche (mehrere Lehrkräfte an einem
  Nachmittag) und wird als Einheit gebucht. Ist auch nur ein Slot inzwischen vergeben, kommt
  **keine** der Buchungen zustande und die Eltern wählen neu.
- **Kein Zeitkonflikt:** Zwei Buchungen derselben Auswahl dürfen nicht auf dieselbe Uhrzeit
  fallen — man kann nicht an zwei Tischen gleichzeitig sitzen. Die Regel wird beim Speichern
  durchgesetzt (`Buchen`-Use-Case, `ZeitkonfliktException`) und gilt innerhalb eines Vorgangs;
  `BookingSession` zeigt sie zusätzlich vorab an, damit Eltern gar nicht erst in den Konflikt
  laufen.
- **Die Buchung hält alles fest, was sie bezeugt.** Weder die Familie noch das Buchungsziel sind
  Verweise: Beides steht an der Buchung selbst, mit dem Stand vom Buchungszeitpunkt. Eine Buchung
  bleibt dadurch vollständig lesbar, auch wenn sich die Stammdaten später ändern.

### Nachtragen

Der Organizer bucht **im Namen einer Familie**, die noch keine Buchung hat: Sie ruft an, steht vor
der Tür, hat kein Gerät oder keine E-Mail-Adresse. Fachlich ist es derselbe Vorgang wie der
Eltern-Submit — mehrere Wünsche, alles oder nichts, kein Zeitkonflikt —, nur tippt ihn jemand
anderes. Ohne diesen Weg gäbe es für eine Familie ohne Gerät **keinen** Zugang zu einem Termin;
das ist eine Zugangsfrage, kein Komfort.

**Die E-Mail bleibt Pflicht.** Hat die Familie keine Adresse, trägt der Organizer die
**Stellvertreteradresse der Schule** ein. Absage und Bestätigung landen dann im Sekretariat, und
dort liegt die Pflicht, die Familie anzurufen — die letzte Meile ist menschlich, nicht technisch.
Die Pflicht selbst wird dadurch nicht aufgeweicht (ADR 0001), nur der Empfänger verlegt.

Sprachgebrauch: **Nachtragen** für den Vorgang, **Stellvertreteradresse** für die Adresse der
Schule. Nicht „Organizer-Buchung" — gebucht wird für die Familie, nicht für den Organizer.

Der Kern — eigener Port `Nachtragen`, geteilte Mechanik mit `Buchen` über einen package-privaten
Service, bis einschließlich Mailversand — ist gebaut
([#151](https://github.com/openClassware/elternsprechtag/issues/151)). Ohne Oberfläche und ohne
Stellvertreteradresse noch
([#104](https://github.com/openClassware/elternsprechtag/issues/104)).

### Umbuchen

Eine **bestehende** Buchung wechselt auf einen anderen Slot **derselben Lehrkraft**. Anlass ist
fast immer, dass die Familie zur gebuchten Zeit doch nicht kann. Alles außer der Uhrzeit wird
mitgenommen: Familie, Buchungsziel und Notiz. Das Buchungsziel wird dabei **übernommen, nicht neu
ermittelt** — deshalb lässt sich auch eine Buchung noch umbuchen, deren Lehrauftrag in den
Stammdaten inzwischen stillgelegt ist.

**In einem Zug.** Storno und Neubuchung gehören zu einem Vorgang; der alte Slot wird nicht
freigegeben, solange der neue nicht sicher ist. Sonst stünde die Familie ohne Termin da, wenn der
Wunschslot zwischendurch vergeben wird.

**Eine Mail, keine zwei.** Die Familie erhält die Bestätigung der geänderten Buchung, keine Absage
plus Bestätigung — zwei Nachrichten für einen Vorgang verwirren.

Sprachgebrauch: **Umbuchen** für die einzelne Buchung. Nicht **verschieben** — das Wort ist für
den ganzen Sprechtag vergeben („einen Sprechtag verschieben"), und das ist ein anderer, bewusst
nicht vorgesehener Vorgang. Die neue Buchung ist eine **neue** Buchung; die alte bleibt als
`STORNIERT` stehen und verschwindet aus dem Plan.

Noch nicht gebaut ([#104](https://github.com/openClassware/elternsprechtag/issues/104)).

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

Der Link führt je nach Sprechtag-Status zu einem von vier Ergebnissen (`Zugangsstand`): **buchbar**
(nur bei `VEROEFFENTLICHT` und bis einschließlich zum **Anmeldeschluss**), **Anmeldung beendet**
(`VEROEFFENTLICHT`, Anmeldeschluss vorbei — Datum, Ort und Schulkontakt, bis der Tagesjob den
Sprechtag abschließt), **abgesagt** oder **nicht verfügbar** (unbekanntes Token, Entwurf,
abgeschlossen — eine eigene Ansicht nach dem Sprechtag folgt mit
[#131](https://github.com/openClassware/elternsprechtag/issues/131)).

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
| Buchung verschieben              | Buchung **umbuchen** — „verschieben" meint den ganzen Sprechtag |
| Deadline                         | **Anmeldeschluss** (das Datum) bzw. **Anmeldefrist** (die Tage davor) |
| Organizer-Buchung                | **Nachtragen** — gebucht wird für die Familie, nicht für den Organizer |
