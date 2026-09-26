# Abdeckung der wesentlichen Anwendungsfälle

**Stand: 2026-09-14.** Dieser Maßstab bezieht sich auf den *realen Ablauf eines Elternsprechtags
an einer Schule* — nicht auf die dokumentierte Domäne und nicht auf den vorhandenen Code. Er
listet für die Phasen 2–6 dieses Ablaufs die Fälle jenseits des Happy Path auf und stuft jeden
ein.

**Was dieses Dokument ist:** ein *Maßstab*, kein Bauauftrag. Es sagt, was dieses Produkt an
Nicht-Happy-Path abdecken muss, damit künftige Features sich daran messen lassen. Ein Feature ist
fertig, wenn die Fälle seiner Phase, die auf `muss` stehen, sich verhalten wie hier beschrieben.

**Was dieses Dokument nicht ist:** eine Liste überwachter Bedingungen. Es ist eine
**Momentaufnahme**. Es gibt genau **einen Revisionsanlass** für die gesamte Stufe `darf fehlen`:
*bevor zum ersten Mal ein echter Sprechtag an einer Schule damit gefahren wird.* Alle Fälle dieser
Stufe tragen dieselbe Annahme — dass kein Ernstfall bevorsteht. Fällt sie, fallen sie gemeinsam.

Die Begriffe (Sprechtag, Termin, Buchung, Lehrauftrag, Organizer, Access-Token) kommen aus den
beiden Glossaren, auf die [`CONTEXT-MAP.md`](../../CONTEXT-MAP.md) verweist; die Architekturregeln
aus [`ARCHITECTURE.md`](ARCHITECTURE.md). Beides wird von hier **verwiesen, nicht wiederholt**.

Entstanden aus der Wayfinder-Karte
[#93](https://github.com/openClassware/elternsprechtag/issues/93); die Begründungen einzelner
Einstufungen stehen ausführlich in den dortigen Tickets.

---

## Bewertungsraster

### Härtegrad — hängt am Akteur

Vier denkbare Stufen von „behandelt": (1) stürzt nicht ab, (2) erklärt sich in der Sprache des
Nutzers, (3) bietet einen Weg raus, (4) ist durch einen Test belegt. Welche Stufe gilt, entscheidet
der betroffene Akteur:

- **Eltern → Stufe 3.** Sie sind anonym, haben keine Rechte und können nichts reparieren. Führt
  die App sie nicht weiter, ist der Fall für sie verloren.
- **Organizer → Stufe 2.** Er hat die Rechte, den Zustand selbst zu ändern; er braucht zu wissen,
  was los ist, nicht geführt zu werden.

### Die drei Stufen

| Stufe | Bedeutung |
|---|---|
| **muss** | Tritt im realen Ablauf auf, und ohne Behandlung bleibt jemand ohne Ausweg stecken oder es gehen Daten verloren. |
| **darf fehlen** | Tritt auf, aber es gibt einen Weg drumherum außerhalb der App (Anruf in der Schule, der Organizer ändert es von Hand). Kein Prinzip spricht dagegen, es später zu bauen. |
| **bewusst nein** | Wird nicht gebaut, obwohl der Fall auftritt, weil etwas dagegen spricht: eine Zweckbindung (Eltern-E-Mail, [ADR 0002](../adr/0002-zweckerweiterung-eltern-email-buchungsbestaetigung.md)), das Auth-Modell (kein Lehrer-Login) oder es gehört nicht zu diesem Produkt. |

Der Unterschied zwischen den letzten beiden ist **Absicht, nicht Priorität**: `darf fehlen` heißt
*vielleicht später*, `bewusst nein` heißt *nie* — und wenn es doch kommt, braucht es einen ADR.
Jedes `bewusst nein` trägt unten eine Anmerkung; ohne Begründung wäre die Einstufung wertlos.

**Zum Weg drumherum.** Die Rolle Organizer schließt das Sekretariat mit ein — ein Anruf in der
Schule zählt als gültiger Weg drumherum. Er setzt voraus, dass die Eltern wissen, wen sie anrufen:
Deshalb wird der **Schulkontakt** ein Pflichtfeld am Sprechtag (Phase 2). Solange das
fehlt, ruht jede Stufe `darf fehlen` mit Akteur *Eltern* auf einer Annahme, die die App selbst
nicht einlöst.

### Tests

Der Test folgt der Schichtung aus [`CLAUDE.md`](../../CLAUDE.md), statt einen zweiten Maßstab zu
erfinden: Sitzt die Regel eines `muss`-Falls im Service, gehört ein Test dazu. Liegt der Fall rein
im View, nicht — Views bleiben dumm und testfrei. Vaadin-freie UI-Modelle wie `BookingSession`
zählen zur testpflichtigen Seite.

### Form eines Eintrags

Je Phase eine Tabelle, darunter Anmerkungen nur zu den Fällen, die eine Begründung brauchen.
Spalte **Erwartet** trägt das Verhalten auf dem Härtegrad des Akteurs, Spalte **Heute** den am Code
belegten Ist-Stand — keine Vermutung.

---

## Phase 1 — Stammdaten: außerhalb des Maßstabs

Klassen, Lehrkräfte, Fächer und Lehraufträge entstehen heute nur per SQL und Seed-Daten, ohne jede
Oberfläche. Diese Phase wird hier **bewusst nicht eingestuft**: Es ist kein Vorsystem der Schule
bekannt und damit auch keine Schnittstelle; ob die Stammdaten künftig importiert, gepflegt oder
weiter geskriptet werden, ist derzeit nicht entscheidbar. Der Maßstab beginnt dort, wo ein
Organizer einen Sprechtag anlegt.

---

## Phase 2 — Sprechtag vorbereiten und veröffentlichen

| Fall | Akteur | Erwartet | Stufe | Heute |
|---|---|---|---|---|
| Zeitfenster oder Slot-Dauer nach dem Veröffentlichen ändern | Organizer | gesperrt, mit Begründung | muss | **erfüllt** — `Sprechtag.legeZeitstrukturFest` friert ab `VEROEFFENTLICHT` ein (`ZeitstrukturEingefrorenException`), die Oberfläche zeigt die Begründung |
| Klasse hinzufügen, nachdem veröffentlicht wurde | Organizer | gesperrt, mit Begründung | muss | **erfüllt** — dieselbe Sperre: Die Klassenliste gehört zur Zeitstruktur, aus ihr entstehen die Termine |
| Klasse entfernen, deren Eltern gebucht haben | Organizer | gesperrt, mit Begründung | muss | **erfüllt** — gebucht wird erst nach dem Veröffentlichen, dieselbe Sperre deckt den Fall ab |
| Titel, Ort, Hinweistext nach dem Veröffentlichen ändern | Organizer | bleibt erlaubt | muss | **erfüllt** — `Sprechtag.beschreibeNeu` bleibt offen, nur Endzustände sind ausgenommen |
| Zu früh veröffentlicht, zurück auf Entwurf, keine Buchung vorhanden | Organizer | erlaubt | muss | **erfüllt** — eigener Weg `ZurueckAufEntwurf` im Zeilenmenü; verwirft die Termine, damit das nächste Veröffentlichen neu rechnet |
| Zurück auf Entwurf, obwohl gebucht wurde | Organizer | verhindert, Verweis auf Absage | muss | **erfüllt** — `SprechtagHatBuchungenException`; auch eine stornierte Buchung zählt, benachrichtigt wurde trotzdem |
| Abgesagten oder abgeschlossenen Sprechtag über „Speichern" wiederbeleben | Organizer | verhindert | muss | **erfüllt** — Speichern setzt keinen Status mehr; Endzustände weisen jede Änderung ab (`StatusuebergangException`) |
| Keine der gewählten Klassen hat einen Lehrauftrag | Organizer | Meldung beim Veröffentlichen: keine Termine erzeugt | muss | **erfüllt** — `Veroeffentlichen.Ergebnis.ohneTermine()`, die Oberfläche meldet es |
| Slot-Dauer geleert (`null`) | Organizer | Pflichtfeld, Speichern nicht möglich | muss | **erfüllt** — `asRequired` am Feld, `Slotdauer` verlangt zusätzlich einen positiven Wert |
| Schulkontakt im Sprechtag | Eltern | eigenes Freitextfeld, Pflicht ab dem Entwurf | muss | **erfüllt** — eigene Spalte seit V3, in der Domäne der Wert `Schulkontakt` (nicht leer, getrimmt), im Formular `asRequired` |
| Zeitfenster geht nicht glatt auf, Rest-Slot entfällt | Organizer | — | darf fehlen | `Sprechtag.slots()` verwirft ihn kommentarlos |
| Datum liegt in der Vergangenheit | Organizer | — | darf fehlen | keine Prüfung, `DatePicker` ohne Minimum |
| Eltern haben den Zugangs-Link verloren | Eltern | — | darf fehlen | Weg drumherum: Anruf in der Schule |
| Zwei Sprechtage am selben Tag oder überlappend | Organizer | — | darf fehlen | keine Prüfung |
| Zugangs-Link neu ausstellen | Organizer | — | bewusst nein | weiterhin vorhanden (`EditSprechtagView`, `Sprechtag.beschreibeNeu`) — **wird entfernt**, Issue #117 |
| Link gerät an Fremde, die Slots blockieren | Organizer | — | bewusst nein | Token ist der gesamte Zugangsschutz |

### Anmerkungen

**Sperren statt Nachziehen.** Was nach dem Veröffentlichen die Terminstruktur berührt —
Zeitfenster, Slot-Dauer, Klassen — ist unveränderlich. Wer sich vertan hat, sagt ab und legt neu
an; das Duplizieren gibt es bereits (`Duplizieren.dupliziere`). Nachmaterialisieren wäre die
Alternative, wirft aber bei jeder Variante die Frage auf, was mit betroffenen Buchungen geschieht,
und wiegt für dieses Produkt zu schwer. Was nur Text ist — Titel, Ort, Hinweistext — bleibt frei
änderbar, weil es keine Termine anfasst.

**Die Grenze ist „es gibt eine Buchung".** Vor der ersten Buchung hält niemand etwas in der Hand,
was kaputtgehen kann; danach ist jeder Rückweg ein stiller Verlust einer Zusage. Der Rückweg auf
Entwurf ist deshalb genau bis zur ersten Buchung erlaubt und danach gesperrt, mit dem Verweis auf
die Absage als dem einzigen verbleibenden Weg. Dieselbe Grenze gilt in Phase 6 für das Löschen.

**Der Schulkontakt ist die Voraussetzung der Stufe `darf fehlen`.** Drei Einstufungen dieser Phase
lauten „darf fehlen, die Eltern rufen an". Diese Annahme trägt nur, wenn die Eltern wissen, wen sie
anrufen. Ein freier Hinweistext, in den der Organizer eine Nummer schreiben *kann*, reicht dafür
nicht. Deshalb ein **eigenes Feld** am Sprechtag — der **Schulkontakt** —, das **ab dem Entwurf**
Pflicht ist. Es bleibt bewusst **Freitext** und wird nicht in Ansprechpartner, Telefon und E-Mail
zerlegt: Kontakt aufnehmen ist mehr als anrufen oder schreiben, und Sekretariatszeiten, eine
Durchwahl mit Bedingung oder „kommen Sie vorbei" müssen hineinpassen. Die Pflichtprüfung heißt
damit zwangsläufig nur „nicht leer" — den Unterschied zum Hinweistext macht nicht die Prüfung,
sondern die Führung eines eigenen, benannten, erzwungenen Feldes. Fiele dieser Fall weg, fielen
alle `darf fehlen` für Eltern auf `muss` zurück.

Die Pflicht greift **ab dem Entwurf**, nicht erst beim Veröffentlichen: Ein Sprechtag ohne
Schulkontakt ist unvollständig, egal in welchem Status. Die Unterscheidung kaufte nichts ein, hätte
aber jede Stelle verteuert — einen bedingten Check-Constraint, eine Prüfung in beiden schreibenden
Wegen, ein Formularfeld, das erst auf Knopfdruck erforderlich wird, und eine eigene Fehlermeldung
in der Sprechtag-Liste.

**Wo er erscheint, ist eine eigene Frage.** Der Schulkontakt steht in der **Bestätigungs-** und der
**Absagemail** und in den Elternansichten nach Anmeldeschluss (Phase 5) und nach dem Sprechtag
(Phase 6) — dort ist er die Hauptaussage der Seite. Auf der **Buchungsseite steht er bewusst
nicht**: Dort soll nichts von der Terminwahl ablenken, und die Eltern haben ihn schwarz auf weiß in
der Bestätigungsmail, sobald sie ihn brauchen. Der Kontakt ist ein Rückfallweg, kein Aufruf zum
Anrufen.

**Zugangs-Link neu ausstellen — bewusst nein, und zwar rückwärts.** Die Funktion sollte bei
bemerktem Missbrauch billig einen neuen Link liefern, ohne den Sprechtag abzusagen und Buchungen zu
verlieren. Sie ist ohne konkreten Anlassfall entstanden, und ihre Nebenwirkung ist erheblich: Nach
dem Neuwürfeln laufen alle bereits verteilten Links ins Leere, ohne dass die betroffenen Eltern
erfahren, warum. Sie über eine E-Mail an die gebuchten Eltern zu heilen, wäre eine dritte
Verwendung der Eltern-Adresse und damit laut ADR 0002 ein eigener ADR. Deshalb: **entfernen** und
bei einem realen Missbrauchsfall neu bewerten. Das ist einer von zwei Einträgen dieses Dokuments,
die Abbau statt Aufbau verlangen; der andere ist der Handabschluss in Phase 6.

**Missbrauch des Links — bewusst nein.** Wer den Link hat, darf buchen; das Token ist der gesamte
Zugangsschutz und laut `docs/contexts/sprechtag/CONTEXT.md` eine bewusste Entscheidung. Personenbezogene Daten gibt die
Elternsicht dabei nicht preis — `Buchungsoptionen.SlotOption` trägt nur Termin-Id, Uhrzeit und ein
`belegt`-Flag. Festzuhalten bleibt: Gegen mutwilliges Blockieren hat der Organizer kein
Mittel außer der Absage des ganzen Sprechtags.

**Rest-Slot und Vergangenheitsdatum — weich eingestuft.** Beides ist im Entwurf sichtbar und vom
Organizer selbst korrigierbar, bevor Schaden entsteht.

---

## Phase 3 — Buchungsphase

| Fall | Akteur | Erwartet | Stufe | Heute |
|---|---|---|---|---|
| Buchung stornieren, Slot wird wieder frei | Organizer | Storno in der Auswertung, Termin geht auf `FREI` zurück | muss | **erfüllt** — Storno-Aktion je Zeile mit Bestätigungsdialog, `Stornieren`-Use-Case setzt die Buchung auf `STORNIERT` und gibt den Slot frei; ohne Mail, nur bei `VEROEFFENTLICHT` |
| Organizer bucht im Namen einer Familie (Nachtragen, Umbuchen) | Organizer | eigene Buchungsstrecke mit denselben Regeln | muss | fehlt vollständig — die einzige Buchungsstrecke ist die Eltern-Ansicht hinter dem Zugangs-Link |
| Familie hat keine E-Mail-Adresse | Organizer | Stellvertreteradresse der Schule eintragen | muss | `eltern_email` ist `not null` (`Buchung:41`), aber es gibt keine Strecke, über die der Organizer bucht |
| Gewählter Slot wird während des Absendens vergeben | Eltern | Meldung, übrige Auswahl bleibt, nur der verlorene Slot neu | muss | **erfüllt** — `TerminBelegtException` wird gefangen, Optionen neu geladen, ungültige Slots verworfen (`ElternsprechtagView:530`, `BookingSession.reload`) |
| Vergangene Slots sind noch buchbar | Eltern | vergangene Slots nicht mehr wählbar | muss | keine Zeitprüfung — um 15:30 lässt sich ein Slot für 15:00 buchen |
| Alle Slots einer Lehrkraft belegt | Eltern | sichtbar, dass nichts frei ist | muss | teilweise — belegte Slots werden als `BELEGT` gerendert (`BookingSession.slotState`), ein eigener Hinweistext fehlt |
| Buchungsschluss vor dem Sprechtag | Organizer | Anmeldeschluss am Sprechtag | muss | **erfüllt** — Anmeldefrist am `Sprechtag`, in Phase 5 erhoben und eingestuft |
| Eltern stornieren ihre Buchung selbst | Eltern | — | darf fehlen | fehlt; bräuchte ein Token je Buchung. Weg drumherum: Anruf, der Organizer storniert — der Weg drumherum ist gebaut |
| Dieselbe Familie bucht zweimal | Eltern | — | darf fehlen | keine Dublettenprüfung in `buchen()`; heilbar, weil der Organizer eine der beiden Zeilen storniert |
| Geschwisterkinder in zwei Klassen | Eltern | — | darf fehlen | ein Kind je Buchungsvorgang; die Zeitkonfliktprüfung greift nur innerhalb eines Vorgangs |
| Eltern wollen ihre Buchung später einsehen | Eltern | — | darf fehlen | die Bestätigungsmail ist der Beleg; sonst Anruf |
| Tippfehler in der E-Mail, Bestätigung kommt nie an | Eltern | — | darf fehlen | `EmailField` prüft nur das Format; die Buchung steht, die Bestätigungsseite hat sie gezeigt |

### Anmerkungen

**Storno gehört dem Organizer, nicht den Eltern.** Eltern rufen an, der Organizer nimmt die Buchung
zurück, der Slot wird frei. Die Selbstbedienung der Eltern bräuchte einen Identitätsanker, den das
Produkt bewusst nicht hat — der Zugangs-Link gehört dem *Sprechtag*, nicht der Familie. Ein Token
je Buchung samt eigenem Routenzweig wäre das zweite Zugangskonzept im Produkt; dafür fehlt der
Anlass.

Das Storno **verschickt nichts**. Der Anlass ist praktisch immer der Anruf der Familie; beim
Tippfehler in der Adresse ginge eine Mail erneut an einen Dritten, beim Löschverlangen wäre sie
widersinnig — und sie wäre der dritte Zweck der Eltern-Adresse und damit nach
[ADR 0002](../adr/0002-zweckerweiterung-eltern-email-buchungsbestaetigung.md) ein eigener ADR. Der
Fall, in dem die Familie *nichts* weiß, ist der Ausfall einer Lehrkraft; er hat einen eigenen
Termin-Zustand und eine eigene Mail. Storniert wird nur an einem **veröffentlichten** Sprechtag:
Danach bucht niemand mehr, „wieder frei" wäre eine Lüge, und ein Löschverlangen nach dem Sprechtag
ist die Anonymisierung aus Phase 6.

**Der Organizer muss buchen können — das ist eine Zugangsfrage, kein Komfort.** Ohne eigene Strecke
gibt es für eine Familie ohne E-Mail oder ohne Gerät *keinen* Weg in einen Termin, auch nicht über
das Sekretariat. Dieselbe Strecke trägt zugleich das Umbuchen (Storno plus Neubuchung in einem Zug,
ohne dass der Slot zwischendurch an jemand anderen geht) und das Nachtragen. Sie muss dieselben
Regeln durchsetzen wie die Eltern-Ansicht: kein Zeitkonflikt, alles oder nichts.

**Die E-Mail bleibt Pflicht, auch für die Organizer-Buchung.** Hat die Familie keine Adresse, trägt
der Organizer die **Stellvertreteradresse der Schule** ein — einen konfigurierten Wert, nicht
Freitext. Die Absage-Benachrichtigung landet dann im Sekretariat, und dort liegt die Pflicht, diese
Familie anzurufen: Die letzte Meile ist menschlich.
[ADR 0001](../adr/0001-eltern-email-pflicht-fuer-absage-benachrichtigung.md) bleibt unangetastet:
Seine Absicht ist, dass niemand zu spät von einer Absage erfährt; hier wird der Empfänger bewusst
zur Schule verlegt, statt die Pflicht aufzuweichen.

**Diese Meile kann das Sekretariat heute nicht gehen — die Kette reißt am Ende.** Der Satz „formal
intakt" stand hier und war falsch. Die Empfänger werden per `distinct` auf der Adresse ermittelt,
also entsteht für drei Familien an derselben Stellvertreteradresse **eine** Mail; ihr Text nennt
keinen Familiennamen; und die Auswertung führt die E-Mail-Adresse gar nicht, sodass auch kein
Nachschlagen hilft. Das Sekretariat erfährt, dass der Sprechtag ausfällt, aber nicht, wen es
anrufen muss — und niemand bemerkt die Lücke. Erhoben als [#148](https://github.com/openClassware/elternsprechtag/issues/148);
der allgemeine Fall steht als Zustellzustand in Phase 4.

**Der Konfliktfall ist bereits fertig.** Er war der Musterfall für Härtegrad 3 beim Aufstellen des
Rasters und ist genau so gebaut: Meldung, Neuladen, gezieltes Verwerfen nur der ungültig gewordenen
Slots. Er steht hier als Beleg, nicht als Lücke — und trägt zugleich die Nebenläufigkeit, die
technisch am optimistischen Locking über `@Version` auf `Termin` hängt (siehe
[`ARCHITECTURE.md`](ARCHITECTURE.md)).

**Geschwisterkinder — bewusst zurückgestellt; neu zu bewerten, sobald sie real auftreten.** Der
Fall ist real und wäre über einen Buchungsvorgang für mehrere Kinder abbildbar. Vorerst bleibt es
bei einem Kind je Vorgang. Folge, die hier stehen muss: Die Domänenregel „kein Zeitkonflikt" aus
`docs/contexts/sprechtag/CONTEXT.md` ist damit nur *innerhalb* eines Vorgangs durchgesetzt. Eltern können für zwei Kinder
dieselbe Uhrzeit buchen; bemerken kann das niemand, auch der Organizer nicht, weil die Auswertung
nach Lehrkraft gruppiert und nicht nach Familie.

**Dubletten und Tippfehler — weich, weil heilbar.** Beide werden erst durch das Storno heilbar: Der
Organizer sieht die doppelte Zeile in der Auswertung und nimmt eine zurück. Ohne Storno wären beide
`muss` gewesen.

---

## Phase 4 — Kurz vor dem Termin

Abgrenzung der Phase: nicht zeitlich, sondern nach Anlass — alles, was den *geplanten Ablauf
nachträglich verändert*, nachdem Buchungen existieren, bis zum Vorabend. Das Buchen selbst bleibt
in Phase 3.

| Fall | Akteur | Erwartet | Stufe | Heute |
|---|---|---|---|---|
| Einzelne Lehrkraft fällt aus (ganz oder teilweise) | Eltern | Termine entfallen, betroffene Familien werden benachrichtigt | muss | **erfüllt** — `EntfallenLassen`-Use-Case setzt `Termin.lassEntfallen()` je gewähltem Termin, storniert eine aktive Buchung mit und benachrichtigt betroffene Familien nach Commit; Sammelaktion „Lehrkraft fällt aus" im `AusfallDialog` der Auswertung |
| Ein Termin ist nicht buchbar, weil er entfällt | Eltern | nicht buchbar | muss | **erfüllt** — `Verfuegbarkeit.ENTFAELLT` ist gespeichert, `Termin.istBuchbar()` liefert `false`; die Eltern-Ansicht fasst „belegt" und „entfällt" bewusst zu `buchbar=false` zusammen (`Buchungsoptionen.SlotOption`), ein dritter, für die Eltern unterscheidbarer Zustand ist nicht geplant |
| Absage-Nachricht führt zurück in die Buchung | Eltern | Zugangs-Link in der Mail, Familie bucht selbst neu | muss | fehlt; der Link existiert nirgends — die Bestätigungsmail ist ein reiner Beleg ohne Aktion, und es fehlt eine konfigurierte öffentliche Basis-URL, aus der sich eine absolute Adresse bauen ließe (#109) |
| Erinnerung vor dem Sprechtag | Eltern | automatischer Versand zum gewählten Vorlauf | muss | fehlt vollständig — jeder Mailversand hängt heute an einer Organizer-Handlung |
| Erinnerungszeitpunkt wählbar | Organizer | Auswahl fester Optionen am `Sprechtag`, auch nach dem Veröffentlichen änderbar | muss | kein Feld |
| Keine Erinnerung für abgesagten Sprechtag oder stornierte Buchung | Eltern | Versand überspringt sie | muss | fehlt (mit der Erinnerung selbst) |
| E-Mail-Versand schlägt fehl, niemand erfährt es | Organizer | Liste der nicht erreichten Familien in der Auswertung | muss | fehlt — best-effort mit `log.warn` je Adresse (`AbsageBenachrichtigungService`), `@Async` nach Commit, die UI erfährt nichts |
| Tippfehler in der Adresse beim Erfassen | Eltern | zweite Eingabe „E-Mail wiederholen" | muss | fehlt; `EmailField` prüft nur das Format |
| Einzelnen Termin verschieben, Buchung behalten | Organizer | Umbuchen in einem Zug, neue Bestätigungsmail | muss | fehlt — fällt mit der Organizer-Buchungsstrecke aus Phase 3 ab |
| Absage-Dialog nennt die Zahl der Betroffenen | Organizer | Zahl vor dem Bestätigen | muss | **erfüllt** — `Absagen.zaehleBetroffeneEltern`, je Adresse einmal gezählt |
| Absage ohne jede Buchung | Organizer | kein Versand, kein Fehler | muss | **erfüllt** — `benachrichtige` steigt bei leerer Adressliste aus |
| Versand rollt die Absage zurück | Organizer | kann nicht passieren | muss | **erfüllt** — `AFTER_COMMIT` im `AbsageBenachrichtigungListener` |
| Sprechtag wird verschoben statt abgesagt | Organizer | — | darf fehlen | kein Weg; `startDate`/`startTime` nach Phase 2 gesperrt. Weg drumherum: absagen → `duplicate` → neu veröffentlichen |
| Bounce der Absage- oder Bestätigungsmail auswerten | Organizer | — | darf fehlen | fehlt; `JavaMailSender.send` meldet nur die sofortige SMTP-Ablehnung |
| Ort ändert sich kurzfristig | Eltern | — | darf fehlen | `location` bleibt änderbar; die Buchungsseite zeigt den aktuellen Stand, eine Nachricht geht nicht raus |
| Ersatztermin wird automatisch zugeteilt | Eltern | — | bewusst nein | fehlt |
| Double-Opt-In der Eltern-Adresse | Eltern | — | bewusst nein | fehlt |
| Lehrkraft meldet ihren Ausfall selbst | Lehrkraft | — | bewusst nein | fehlt; `Lehrer` ist reines Stammdatum, kein Login |
| Absage rückgängig machen | Organizer | — | bewusst nein | `ABGESAGT` ist Endzustand — das Aggregat weist jeden weiteren Übergang ab |

### Anmerkungen

**Der Ausfall braucht einen eigenen Weg — begründet über die Eltern, nicht über den Komfort des
Organizers.** Aus den Bausteinen ließe er sich scheinbar zusammensetzen: Der Organizer storniert die
acht Buchungen der Lehrkraft von Hand und telefoniert. Das ist nicht nur mühsam, es ist **falsch**.
Erstens erfahren die Eltern nichts — die Buchung verschwindet still, während die Komplettabsage eine
Mail schickt; genau die Lücke, die ADR 0001 schließen soll. Zweitens gibt das Storno die Slots
wieder frei, sodass eine andere Familie in einen Termin bucht, der nicht stattfindet.

**Der Baustein ist der Termin, nicht die Lehrkraft.** Der Ausfall kommt real selten ganztägig — wer
erst ab 16 Uhr da ist, darf nicht ganz abgesagt werden müssen. Deshalb bekommt der `Termin` einen
dritten Zustand `ENTFAELLT` (nicht buchbar; eine daran hängende `Buchung` geht auf `ABGESAGT`), und
„Lehrkraft fällt aus" ist die **Sammelaktion** über eine Auswahl ihrer Termine — mit *einer* Mail je
betroffener Familie. `FREI` wäre die Lüge, die der Bausteinweg erzeugt: Es sagt „buchbar". Die
Lehrkraft bleibt damit Stammdatum ohne Tageszustand; andernfalls bräuchte sie einen Zustand *je
Sprechtag* und damit eine neue Zwischenentität, für die es sonst keinen Anlass gibt.

**Die Einzelabsage braucht keinen neuen ADR.** ADR 0001 bindet die Adresse an „Benachrichtigung
über Änderungen an genau dem gebuchten Sprechtag" — der Ausfall einer Lehrkraft ist genau so eine
Änderung. Die Komplettabsage war der damalige Anlass, nicht die Grenze.

**Erinnerung — muss, und damit ADR 0006.** Terminvergessen ist die Hauptursache leerer Slots;
zwischen Buchung und Sprechtag liegen oft drei Wochen. Die Erinnerung meldet aber **keine Änderung**
und ist damit der dritte Zweck der Eltern-Adresse nach Absage (ADR 0001) und Bestätigung (ADR 0002)
— die Zweckerweiterung ist **Voraussetzung** des Features, nicht sein Nachtrag. Sie ist zugleich der
erste zeitgesteuerte Vorgang im Produkt. Ausbaustufe: täglicher `@Scheduled`-Lauf, Zeitstempel an
der `Buchung` gegen Doppelversand, und **verfallen statt nachholen** — eine Erinnerung, die am
Sprechtagmorgen eintrifft, weil der Server nachholt, hilft niemandem. Ein Knopf beim Organizer wurde
verworfen: Wer den Sprechtag drei Wochen vorher vorbereitet hat, ist genau der, der am Vortag
anderes zu tun hat.

**Der Erinnerungszeitpunkt ist eine Auswahl am `Sprechtag`.** Feste Optionen (*keine Erinnerung · 1
· 2 · 3 Tage vorher*), die Uhrzeit steckt in der Option statt in einem zweiten Feld. Eine freie
Datumsangabe wurde verworfen, weil sie einen neuen Fehlerfall erzeugt, den feste Optionen nicht
haben können — einen Zeitpunkt nach dem Sprechtag. Das Feld ist **nicht terminrelevant** im Sinne
von Phase 2 (es verschiebt keinen Termin, macht keine Buchung ungültig) und bleibt nach dem
Veröffentlichen änderbar; Änderungen wirken ab dem nächsten Lauf. Das Feld selbst entsteht in
Phase 2, gehört fachlich aber hierher.

**Der Versandfehler reißt die Kette aus ADR 0001.** Heute sagt die Oberfläche „erledigt", während in
einer Logdatei steht, dass 3 von 20 Familien nichts erfahren haben. Phase 3 hat die Kette zusätzlich
belastet: Familien ohne eigene Adresse bekommen die Stellvertreteradresse der Schule — diese letzte
Meile kann das Sekretariat nur gehen, wenn es weiß, wen es anrufen muss. Also ein **Zustellzustand
an der `Buchung`** und eine Liste „nicht erreicht" in der Auswertung. Der Weg drumherum (Blick ins
Log) scheidet aus, weil er den Betreiber voraussetzt, nicht die Schule. Preis: Das ist der erste
Fall, in dem der Versand **zurückschreibt** — der Lauf ist heute `readOnly`, die Naht „nach Commit,
asynchron" braucht eine eigene Transaktion. Derselbe Zustand trägt zugleich die Einzelabsage und den
Zeitstempel der Erinnerung.

**Adressprüfung, dreifach gestaffelt.** Technische Vorbemerkung: `JavaMailSender.send` meldet nur die
**sofortige SMTP-Ablehnung**; der typische Fehlschlag kommt als Bounce Minuten später an den Absender
zurück, und den sieht das Produkt nirgends. Der Zustellzustand zeigt also „abgeschickt", nicht
„angekommen". Daher: (1) **zweite Eingabe** beim Erfassen — muss, fängt den Buchstabendreher, der der
reale Hauptfall ist, und kostet ein Feld statt Infrastruktur; (2) die **Bestätigungsmail** ist schon
die passive Verifizierung, drei Wochen vor dem Sprechtag — kommt sie nicht an, merkt die Familie es
selbst; (3) **Bounce-Auswertung** — darf fehlen, weil sie eine Betriebsentscheidung ist (Postfach
anbinden, Rückläufer zuordnen) und der Zustellzustand den Anruf schon ermöglicht; neu zu bewerten,
sobald ein Postfach für Rückläufer betrieblich existiert.

**Double-Opt-In — bewusst nein.** Es gefährdet die Buchung selbst, um eine Adresse zu sichern, die
erst Wochen später gebraucht wird: Schwebezustand der Buchung, gehaltener Slot, verlorener Termin für
jede Familie, die den Code im Handy-Posteingang nicht findet. Für anonyme Eltern wäre es zudem der
Einstieg in ein zweites Zugangskonzept — genau das, was in Phase 3 beim Eltern-Storno abgelehnt
wurde.

**Ersatztermin automatisch — bewusst nein.** Die Zuteilung ist keine Rechenaufgabe, sondern eine
Familienentscheidung. Ein zugeteilter Slot, zu dem niemand kommt, ist schlechter als ein Loch: Er
blockiert ihn für andere. Der Weg raus ist der Zugangs-Link in der Absage-Mail — die Familie sieht
selbst, was frei ist, und bucht neu.

**Umbuchen erzeugt eine neue Bestätigungsmail, keine Absage plus Bestätigung.** Zwei Mails für einen
Vorgang verwirren. Gedeckt durch ADR 0002: Es ist die Bestätigung derselben Buchung mit geänderter
Zeit. Der Fall bekommt kein eigenes Lücken-Issue — er fällt mit der Organizer-Buchungsstrecke aus
Phase 3 ab.

**Verschieben — darf fehlen, und zwar aus einem stärkeren Grund als Aufwand.** Ein verschobener
Sprechtag *kann* seine Buchungen nicht mitnehmen: Der Slot am Dienstag um 16:20 sagt nichts darüber,
ob dieselbe Familie zwei Wochen später um 16:20 kann. Ein „Verschieben, das die Buchungen behält"
würde einen Terminplan retten, der fachlich schon tot ist. Neu buchen ist die richtige Antwort, nicht
der Notbehelf. Was fehlt, ist höchstens Führung: ein Hinweis im Absage-Dialog auf „absagen, dann
duplizieren".

**Die Lehrkraft meldet sich nicht selbst — bewusst nein.** Wer morgen ausfällt, meldet sich **ohnehin
krank**: im Sekretariat, telefonisch, vor 7 Uhr, weil der Vertretungsplan daran hängt. Der Organizer
*ist* das Sekretariat und erfährt es auf dem Weg, den die Schule sowieso geht. Eine Selbstmeldung
stellte einen zweiten Meldeweg neben den etablierten und bräuchte dafür ein drittes Zugangskonzept.
Ehrliche Folge: Die Lehrkraft hat damit **keinerlei Sicht auf ihren eigenen Tag** — was Phase 5
auffängt, nicht diese Phase.

**Absage rückgängig — bewusst nein.** Nicht wegen des Endzustands in der Datenbank, sondern wegen der
Wirklichkeit: 20 Familien haben gelesen „fällt aus" und disponiert. Ein Zurücknehmen stellt einen
Zustand her, an den draußen niemand mehr glaubt. Die Bremse davor existiert bereits — der Dialog
nennt die Zahl der Betroffenen.

**Ortswechsel — darf fehlen.** Der Weg raus liegt in der App: Die Buchungsseite hinter dem
Zugangs-Link zeigt immer den aktuellen Stand — vorausgesetzt, die Familie hat den Link noch, etwa
aus der ursprünglichen Einladung. Dazu der reale Weg, den jede Schule geht — ein Zettel an der
Aulatür.

---

## Phase 5 — Am Tag des Sprechtags

Grundhaltung: Die App ist **Planungswerkzeug, nicht Tagesprotokoll**. Am Nachmittag arbeitet die
Lehrkraft auf Papier, der Organizer am Rechner. Es entsteht **kein zweiter Zugangsweg** — die
Lehrkraft bekommt ihren Plan als Datei, nicht als Login.

| Fall | Akteur | Erwartet | Stufe | Heute |
|---|---|---|---|---|
| Lehrkraft braucht ihren Tagesplan | Lehrkraft | PDF-Export aus der Auswertung: ohne Filter ein ZIP mit einem PDF je Lehrkraft, mit gesetztem Lehrkraft-Filter genau dieses eine PDF | muss | `AuswertungView` hat den Filter je Lehrkraft, aber **keinen** Export |
| Inhalt des Blatts | Lehrkraft | Kopf mit Lehrkraft, Sprechtag, Datum, Ort und „Stand: \<Zeitstempel\>"; alle Slots chronologisch — **auch die freien** — mit Zeit, Schüler, Klasse, Fach, Elternname, Notiz; rechts eine leere Spalte für Handschrift | muss | `Auswerten.BuchungsZeile` liefert nur aktive Buchungen, freie Slots erscheinen nicht |
| Anmeldeschluss | Organizer | Pflichtfeld am `Sprechtag`, beim Anlegen mit dem Vortag vorbelegt, änderbar | muss | **erfüllt** — `Anmeldefrist` (0–28 Tage vor dem Sprechtag, vorbelegt mit 1), bis zum Endzustand änderbar, auch zum Wiederöffnen; das Formular zeigt den errechneten Anmeldeschluss, das Veröffentlichen meldet einen schon verstrichenen |
| Elternlink nach Fristablauf | Eltern | nicht mehr buchbar; Hinweis „Anmeldung beendet" plus Datum, Ort und Schulkontakt | muss | teilweise — nicht mehr buchbar, beim Öffnen wie beim Abschicken (`Sprechtag.nimmtElternbuchungenAn`, `ElternbuchungGeschlossenException`); die eigene Ansicht fehlt, der Link landet auf „nicht verfügbar" (#123) |
| Familie ruft am Tag selbst an, jemand steht spontan vor der Tür | Organizer | die Frist schließt nur den Elternlink; die Organizer-Buchungsstrecke bleibt bis zum Abschluss offen | muss | **erfüllt** — die Frist prüft allein `Buchen`; `Nachtragen` fragt sie nicht und bleibt bis zum Abschluss offen |
| Telefonauskunft „wann habe ich meinen Termin?" | Organizer | Suche nach Schüler- oder Elternname in der Auswertung | muss | nur Lehrkraft-Filter, **keine** Namenssuche |
| Eltern sehen ihre eigene Buchung wieder | Eltern | — | darf fehlen | Token hängt am Sprechtag, nicht an der Familie; Beleg bleibt die Bestätigungsmail, Weg drumherum der Anruf |
| Änderungen nach dem Druck erreichen die Lehrkraft | Lehrkraft | — | darf fehlen | der Zeitstempel im PDF-Kopf macht das Alter des Blattes sichtbar; der Rest ist mündliche Organisation |
| Entfallene Termine auf dem Blatt (durchgestrichen statt verschwunden) | Lehrkraft | — | darf fehlen | fehlt mit dem Export |
| Aushang für die Tür aus der App drucken | Organizer | — | darf fehlen | fehlt |
| Eigener digitaler Zugang für die Lehrkraft | Lehrkraft | — | bewusst nein | fehlt |
| Nicht erschienen, Verspätung, Spontanbesuch erfassen | Organizer | — | bewusst nein | fehlt |
| Eltern-Mailadresse auf dem Lehrkraft-Blatt | Lehrkraft | — | bewusst nein | — |
| Ganzer Terminplan über den Zugangs-Link lesbar | Eltern | — | bewusst nein | Elternseite ist reine Buchungsmaske |
| Kurzfristigen Ausfall am Tag selbst zu den Eltern bringen | Eltern | — | bewusst nein | es bleibt bei der Phase-4-Mechanik plus Aushang und Sekretariat |

### Anmerkungen

**Der PDF-Export ersetzt den Lehrkraft-Zugang vollständig.** Der Versand geschieht außerhalb der App
(Mail, Cloud, Chat) — `Lehrer` hat bewusst kein Mailfeld, und eines einzuführen hieße, einen
Verteilerkanal zu betreiben, für den es keinen Anlass gibt. Der gesetzte Lehrkraft-Filter dient dem
Nachreichen an eine einzelne Person.

**Eigener digitaler Zugang für die Lehrkraft — bewusst nein.** Er würde ein zweites Zugangskonzept
neben dem Access-Token aufreißen (siehe Auth-Modell in [`ARCHITECTURE.md`](ARCHITECTURE.md)).
Zusammen mit „die Lehrkraft meldet sich nicht selbst" aus Phase 4 heißt das: Die Lehrkraft ist im
Produkt **Stammdatum und Empfängerin eines Blattes**, mehr nicht. Das ist die bewusste Ausgestaltung,
kein Versäumnis.

**Kein Tagesprotokoll — bewusst nein.** Nicht erschienen, Verspätung und Spontanbesuch teilen eine
Begründung: Ohne Gerät am Tisch müsste der Organizer die Häkchen hinterher vom Papier abtippen —
Aufwand ohne Abnehmer. Die Handschriftspalte auf dem Blatt ist die Antwort.

**Eltern-Mailadresse nicht auf das Blatt — bewusst nein.** Kontaktdaten auf einem Blatt, das im
Klassenzimmer liegen bleibt, sind eine Datenweitergabe ohne Zweck.

**Ganzer Terminplan über den Zugangs-Link — bewusst nein.** Jeder mit dem Link sähe, welche Familie
wann bei wem sitzt. Das Token schützt den Sprechtag, nicht die einzelne Familie.

**Kurzfristigen Ausfall am Tag selbst — bewusst nein.** Kein digitaler Kanal holt jemanden aus dem
Auto. Ein SMS-Kanal verlangte Telefonnummern als Pflichtangabe, einen Dienstleister und eine neue
Datenschutzfläche.

**Die Namenssuche trägt die fehlende Elternsicht.** Weil Eltern ihre Buchung nicht wieder aufrufen
können (`darf fehlen`, oben), ist die Telefonauskunft der Weg drumherum — und die funktioniert nur,
wenn der Organizer nach dem Namen suchen kann. Ohne sie fiele „Eltern sehen ihre eigene Buchung
wieder" auf `muss` zurück.

**Rückwirkung auf Phase 2.** Der Anmeldeschluss ist ein Feld am `Sprechtag` und entsteht in der
Sprechtag-Pflege; er wurde in Phase 2 noch nicht erhoben und ist hier nachgetragen.

---

## Phase 6 — Nach dem Sprechtag

| Fall | Akteur | Erwartet | Stufe | Heute |
|---|---|---|---|---|
| Sprechtag schließt sich nach Ablauf der Endzeit selbst ab | Organizer | Statuswechsel durch den Tagesjob — der einzige Weg in den Abschluss | muss | **erfüllt** — `AbschlussScheduler` ruft nachts `Abschliessen.schliesseVorbeiAb`; `Sprechtag.schliesseAbWennVorbei` schließt nur Veröffentlichte ab, deren Endzeit verstrichen ist, jeder in eigener Transaktion; das Menü bietet den Abschluss nicht an (`SprechtagStatus.waehlbareUebergaenge`) |
| Elternlink nach dem Sprechtag | Eltern | Ansicht „Der Sprechtag ist vorbei" plus Schulkontakt, keine Buchungsauskunft | muss | `NICHT_VERFUEGBAR` — dieselbe Seite wie bei unbekanntem Token oder Entwurf |
| Aufbewahrungsfrist | — | ab Ende des Sprechtags, Default 30 Tage, als Property verstellbar | muss | fehlt |
| Ablauf der Frist | — | Elternname, Schülername, E-Mail und Notiz werden geleert, die Buchung bleibt; gilt auch für `ABGESAGT` | muss | fehlt — kein Löschen im ganzen Projekt |
| Auswertung nach Fristablauf | Organizer | `anonymisiertAm` am Sprechtag, Hinweis mit Datum statt scheinbarem Datenverlust | muss | fehlt |
| Vorwarnung vor der Anonymisierung | Organizer | abgeschlossene Sprechtage zeigen in der Liste, wann ihre Daten fallen | muss | fehlt |
| Löschverlangen einer Familie nach dem Sprechtag | Organizer | Einzelaktion „Daten dieser Buchung entfernen" mit Rückfrage — zieht dieselbe Anonymisierung vor | muss | fehlt |
| Alter Sprechtag als Vorlage | Organizer | `duplicate` kopiert alle Vorlagefelder und leitet Datumsabhängiges neu ab | muss | teilweise — die drei neuen Felder fehlen ihm |
| Versehentlich angelegter Sprechtag | Organizer | löschbar, solange `ENTWURF` **und** ohne Buchung | muss | fehlt — kein Löschen im Projekt |
| Auskunftsverlangen einzelner Eltern | Organizer | Namenssuche in der Auswertung, vorlesen oder drucken | muss | fällt mit Phase 5 ab |
| Löschverlangen vor dem Sprechtag | Organizer | fachlich eine Stornierung | muss | fällt mit Phase 3 ab |
| Eltern sehen ihre Buchung über den alten Link wieder | Eltern | — | darf fehlen | Beleg ist die Bestätigungsmail, Weg drumherum der Anruf — dieselbe Einstufung wie in Phase 5 |
| Archivierung gegen das Anwachsen der Liste über Jahre | Organizer | — | darf fehlen | der Statusfilter blendet Abgeschlossenes weg; anonymisierte Sprechtage sind nur noch Zahlen |
| Dauerhafter namentlicher Gesprächsbeleg | Organizer | — | bewusst nein | — |
| Getrennte, frühere Frist für die Notiz | — | — | bewusst nein | — |
| Protokoll, wer wann welche Buchung entfernt hat | Organizer | — | bewusst nein | — |
| Vorwarnung oder Löschmitteilung per E-Mail | Organizer / Eltern | — | bewusst nein | — |
| Von Hand abschließen — und der Rückweg für den verfrühten Klick | Organizer | — | bewusst nein | gegenstandslos: Den Abschluss stellt allein der Tagesjob fest; der Menüpunkt ist entfernt (#166) |

### Anmerkungen

**Abschluss ist eine Tatsache, keine Absicht.** „Der Nachmittag ist vorbei" stellt die Maschine fest,
nicht der Organizer — und eine Löschfrist, die an einem Handgriff hängt, den man vergessen kann, ist
keine Frist.

**Deshalb kein Handabschluss — Abbau statt Aufbau.** Ursprünglich blieb der Menüpunkt zum Vorziehen,
und ein Rückweg `ABGESCHLOSSEN → VEROEFFENTLICHT` sollte den verfrühten Klick reparieren (#125). Mit
dem Tagesjob hat der Handgriff aber keine Wirkung mehr, die jemand will: Die Frist läuft ab der
Endzeit, nicht ab dem Statuswechsel; die Auswertung ist in beiden Status lesbar; den Elternlink
schließt der Anmeldeschluss. Übrig bliebe allein, dass ein zu früher Klick die Buchungsstrecke aus
Phase 3 schließt — den einzigen Weg, eine Familie ohne E-Mail oder Laufkundschaft noch in einen
freien Slot zu setzen. Ein Knopf, dessen einzige Folge ein Schaden ist, bekommt keinen Rückweg,
sondern wird entfernt (#166). Die Stunden zwischen Endzeit und nächtlichem Lauf, in denen der
Sprechtag noch als veröffentlicht dasteht, sind harmlos; wer es anders will, stellt
`elternsprechtag.scheduler.abschluss-cron` um.

**Anonymisieren statt Löschen.** Die Buchung bleibt als Datensatz stehen, der Termin bleibt `BELEGT`.
Damit verschwindet alles Personenbezogene, aber die Auslastung bleibt — genau das Wissen, aus dem der
nächste Sprechtag geplant wird (Slot-Dauer, welche Lehrkraft überrannt wurde, wie viele Slots leer
blieben). Es braucht dafür kein zweites Aggregat-Modell, nur ein Leeren von Feldern. Das
Löschverlangen einer Familie benutzt denselben Mechanismus vorgezogen auf eine einzelne Buchung —
kein Sonderweg, und die Auslastungszahl bleibt unverfälscht.

**Frist ab dem Ende des Sprechtags, nicht ab dem Statuswechsel.** Der Zeitpunkt steht am Datensatz,
ist unabhängig davon, ob jemand rechtzeitig geklickt hat, und ist auch für einen abgesagten Sprechtag
definiert — dessen Buchungen sind genauso personenbezogen und liegen heute genauso ewig.

**Hausregel zum Duplizieren.** `duplicate` kopiert, was Vorlage ist, und leitet neu ab, was am Datum
hängt. Die drei Felder, die dieser Maßstab beschlossen hat, werden alle **mitkopiert**: Schulkontakt
(Phase 2), Erinnerungszeitpunkt (Phase 4) und die **Anmeldefrist** (Phase 5). Bei der Frist geht das
nur, weil sie **relativ** gespeichert ist — als Tage vor dem Sprechtag, nicht als Datum. Ein
absoluter Anmeldeschluss läge in der Kopie ein Halbjahr vor dem neuen Sprechtag und machte ihn tot
geboren; der Abstand dagegen passt zu jedem Datum, auf das der Organizer die Kopie setzt (#122). Wer
künftig ein Feld an den `Sprechtag` hängt, muss sich in diese eine Frage einsortieren — und ein Feld,
das am Datum hängt, am besten relativ dazu speichern.

**Löschen nur für buchungsfreie Entwürfe.** Dieselbe Grenze wie in Phase 2: „es gibt eine Buchung"
ist der Punkt, ab dem nichts mehr zurückgeht. Ein veröffentlichter, abgesagter oder abgeschlossener
Sprechtag wird nicht gelöscht — er anonymisiert sich und bleibt als Auslastungszahl stehen. Löschen
ist eine Aufräumfunktion für Fehlgriffe (das Duplizieren erzeugt sie mit einem Klick), kein Weg,
Belege verschwinden zu lassen.

**Dauerhafter namentlicher Gesprächsbeleg — bewusst nein.** Die Frist gilt für alles
Personenbezogene; ein Register „wer hat mit wem gesprochen" wäre Aufbewahrung ohne Zweck. Wer den
Beleg braucht, hat ihn auf Papier — die Lehrkraft-PDFs aus Phase 5 werden ohnehin gedruckt.

**Getrennte, frühere Frist für die Notiz — bewusst nein.** Zwei Fristen sind zwei Regeln mit zwei
Erklärungen; der Gewinn ist gering, wenn der Rest in 30 Tagen ohnehin fällt.

**Protokoll über das Entfernen — bewusst nein.** Es gibt genau eine Organizer-Identität; ein
Protokoll mit immer demselben Namen ist Zierde.

**Vorwarnung oder Löschmitteilung per E-Mail — bewusst nein.** An den Organizer nicht, weil er gar
keine Adresse hinterlegt hat — die Warnung steht deshalb in der Sprechtag-Liste. An die Eltern nicht,
weil ihre Adresse per ADR 0002 auf Absage und Bestätigung zweckgebunden ist; eine dritte Verwendung
bräuchte einen eigenen ADR.

**Der tägliche Scheduler ist der größte gemeinsame Baustein dieses Maßstabs.** Er trägt drei
Aufgaben: die Erinnerung vor dem Sprechtag (Phase 4), den automatischen Abschluss und die
Anonymisierung. ADR 0006 (aus Phase 4) ist seine Voraussetzung. Die Anonymisierung stellt die
E-Mail-Zweckbindung nicht in Frage — sie **beendet** sie.

---

## Was dieser Maßstab nicht abdeckt

Drei Themen sind bedacht und **bewusst nicht** hier eingestuft. Sie bekommen keine Falltabellen und
keine abfallenden Lücken-Issues; dies ist der Wegweiser an den Ort, an den sie gehören.

- **Barrierefreiheit.** Braucht ein eigenes Raster (Prüfkriterien statt `muss` / `darf fehlen` /
  `bewusst nein`); die Stufen dieses Dokuments passen darauf nicht. Festgeschrieben ist heute allein
  die responsive Untergrenze 375 × 667 px ([`CLAUDE.md`](../../CLAUDE.md)). Eine Aussage zur
  Konformität trifft dieses Dokument nicht.
- **Gleichzeitige Zugriffe.** Fachlich in Phase 3 abgehandelt — der Konfliktfall beim Buchen steht
  dort ausdrücklich als Beleg, nicht als Lücke; technisch getragen von optimistischem Locking über
  `@Version` auf `Termin` ([`ARCHITECTURE.md`](ARCHITECTURE.md)). Kein offener Rest.
- **Datenschutz jenseits der Aufbewahrung.** Aufbewahrungsfrist und Anonymisierung stehen in Phase 6.
  Die Zweckbindung der Eltern-E-Mail liegt in
  [ADR 0001](../adr/0001-eltern-email-pflicht-fuer-absage-benachrichtigung.md) und
  [ADR 0002](../adr/0002-zweckerweiterung-eltern-email-buchungsbestaetigung.md); der Ort für neue
  Datenschutzentscheidungen ist ein weiterer ADR, nicht dieses Dokument.

Schweigen wäre zweideutig — es ließe sich als „ist abgedeckt" oder als „hat keiner bedacht" lesen.
Diese Abgrenzung macht daraus die dritte, eindeutige Aussage: bedacht, bewusst nicht hier drin, und
hier ist der Ort dafür.

---

## Lücken-Issues

Für jeden Fall der Stufe `muss`, der heute fehlt, liegt ein Issue im Tracker. Fälle, die als
**erfüllt** in den Tabellen stehen, tauchen hier nicht auf; Fälle, die mit einem anderen Issue
abfallen (Umbuchen, Auskunfts- und Löschverlangen vor dem Sprechtag), sind dort genannt statt
doppelt geführt.

**Phase 2 — Sprechtag vorbereiten und veröffentlichen**

| Issue | Fall |
|---|---|
| [#102](https://github.com/openClassware/elternsprechtag/issues/102) | Schulkontakt als Pflichtfeld am Sprechtag |
| [#112](https://github.com/openClassware/elternsprechtag/issues/112) | Terminrelevante Felder nach dem Veröffentlichen sperren |
| [#113](https://github.com/openClassware/elternsprechtag/issues/113) | Rückweg auf Entwurf regeln: erlaubt ohne Buchung, sonst gesperrt |
| [#114](https://github.com/openClassware/elternsprechtag/issues/114) | Statusübergänge auch in `createOrUpdate` erzwingen |
| [#115](https://github.com/openClassware/elternsprechtag/issues/115) | Meldung beim Veröffentlichen, wenn keine Klasse einen Lehrauftrag hat |
| [#116](https://github.com/openClassware/elternsprechtag/issues/116) | Slot-Dauer als Pflichtfeld |
| [#117](https://github.com/openClassware/elternsprechtag/issues/117) | Token-Neuausstellung **entfernen** |

**Phase 3 — Buchungsphase**

| Issue | Fall |
|---|---|
| [#103](https://github.com/openClassware/elternsprechtag/issues/103) | Buchung durch den Organizer stornieren |
| [#104](https://github.com/openClassware/elternsprechtag/issues/104) | Buchungsstrecke für den Organizer (Nachtragen, Umbuchen, Familien ohne E-Mail) |
| [#118](https://github.com/openClassware/elternsprechtag/issues/118) | Vergangene Slots nicht mehr buchbar |
| [#119](https://github.com/openClassware/elternsprechtag/issues/119) | Hinweistext, wenn bei einer Lehrkraft nichts mehr frei ist |
| [#148](https://github.com/openClassware/elternsprechtag/issues/148) | Absage an die Stellvertreteradresse nennt die betroffenen Familien nicht |

**Phase 4 — Kurz vor dem Termin**

| Issue | Fall |
|---|---|
| [#105](https://github.com/openClassware/elternsprechtag/issues/105) | ADR 0006: Zweckerweiterung der Eltern-E-Mail auf die Erinnerung |
| [#106](https://github.com/openClassware/elternsprechtag/issues/106) | Erinnerungszeitpunkt als Auswahl am Sprechtag |
| [#107](https://github.com/openClassware/elternsprechtag/issues/107) | Erinnerung vor dem Sprechtag versenden (täglicher Scheduler) |
| [#108](https://github.com/openClassware/elternsprechtag/issues/108) | Terminzustand `ENTFAELLT` und Sammelaktion „Lehrkraft fällt aus" |
| [#109](https://github.com/openClassware/elternsprechtag/issues/109) | Zugangs-Link in die Absage-Mail aufnehmen |
| [#110](https://github.com/openClassware/elternsprechtag/issues/110) | Zustellzustand an der Buchung und Liste „nicht erreicht" |
| [#111](https://github.com/openClassware/elternsprechtag/issues/111) | Zweite Eingabe „E-Mail wiederholen" |

**Phase 5 — Am Tag des Sprechtags**

| Issue | Fall |
|---|---|
| [#120](https://github.com/openClassware/elternsprechtag/issues/120) | PDF-Export der Lehrkraft-Pläne (inkl. Inhalt des Blatts) |
| [#121](https://github.com/openClassware/elternsprechtag/issues/121) | Namenssuche in der Auswertung |
| [#122](https://github.com/openClassware/elternsprechtag/issues/122) | Anmeldeschluss als Pflichtfeld am Sprechtag |
| [#123](https://github.com/openClassware/elternsprechtag/issues/123) | Elternansicht nach Anmeldeschluss |

**Phase 6 — Nach dem Sprechtag**

| Issue | Fall |
|---|---|
| [#124](https://github.com/openClassware/elternsprechtag/issues/124) | Sprechtag automatisch abschließen, wenn die Endzeit verstrichen ist |
| [#166](https://github.com/openClassware/elternsprechtag/issues/166) | Handabschluss entfernen (ersetzt #125, den Rückweg `ABGESCHLOSSEN → VEROEFFENTLICHT`) |
| [#126](https://github.com/openClassware/elternsprechtag/issues/126) | Aufbewahrungsfrist und Anonymisierung der Buchungsdaten |
| [#127](https://github.com/openClassware/elternsprechtag/issues/127) | Auswertung nach Fristablauf: `anonymisiertAm` und Hinweis |
| [#128](https://github.com/openClassware/elternsprechtag/issues/128) | Vorwarnung in der Sprechtag-Liste, wann die Daten fallen |
| [#129](https://github.com/openClassware/elternsprechtag/issues/129) | Einzelaktion „Daten dieser Buchung entfernen" |
| [#130](https://github.com/openClassware/elternsprechtag/issues/130) | `duplicate`: neue Pflichtfelder mitführen, Datumsabhängiges neu ableiten |
| [#131](https://github.com/openClassware/elternsprechtag/issues/131) | Elternansicht nach dem Sprechtag |
| [#132](https://github.com/openClassware/elternsprechtag/issues/132) | Buchungsfreie Entwürfe löschen können |

Die Reihenfolge und die Triage-Labels dieser Issues sind noch offen; sie tragen zunächst
`needs-triage`, blockierte zusätzlich `blocked`. Der tiefste Abhängigkeitsstrang läuft über
ADR 0006 (#105) und den Erinnerungszeitpunkt (#106) zur Erinnerung (#107), von dort in den
täglichen Scheduler mit automatischem Abschluss (#124) und Anonymisierung (#126).
