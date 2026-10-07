---
title: Zustände von Sprechtag und Termin
description: Welche Status ein Sprechtag, ein Termin und eine Buchung haben, was sie wechseln lässt und was die Anwendung dabei von selbst erledigt.
sidebar:
  order: 2
---

Ein Sprechtag durchläuft vom Anlegen bis nach dem Gespräch mehrere Status. Manche Wechsel lösen
Sie aus, andere erledigt die Anwendung von selbst in der Nacht. Diese Seite fasst sie zusammen,
dazu die Zustände der einzelnen Termine und Buchungen.

## Sprechtag

In **Elternsprechtage verwalten** steht der Status in jeder Zeile; die Filter oben zeigen nur
Sprechtage eines Status.

| Status | Was er bedeutet | Was Eltern über den Zugangs-Link sehen |
|---|---|---|
| *Entwurf* | Der Sprechtag wird vorbereitet. Termine gibt es noch keine. | *Nicht verfügbar* |
| *Aktiv* | Der Sprechtag ist veröffentlicht, die Termine stehen fest. | Bis zum Anmeldeschluss die Buchungsseite, danach *Die Anmeldung ist beendet* |
| *Abgesagt* | Der Sprechtag findet nicht statt. Endgültig. | *Dieser Elternsprechtag wurde abgesagt* |
| *Abgeschlossen* | Der Sprechtag ist vorbei. Endgültig. | *Der Elternsprechtag ist vorbei* |

Wie die Seiten für Eltern aussehen, zeigt [Was Eltern über den Zugangs-Link sehen](/referenz/was-eltern-ueber-den-zugangs-link-sehen/).

### Übergänge

```txt
             Aktiv setzen
  Entwurf ─────────────────────▶ Aktiv ───── Absagen ─────▶ Abgesagt
     ▲                            │  │
     └──── Zurück auf Entwurf ────┘  └── nachts nach dem ──▶ Abgeschlossen
           (nur ohne Buchung)            Sprechtag, von selbst
```

| Von | Nach | Was ihn auslöst | Was dabei passiert |
|---|---|---|---|
| — | *Entwurf* | **Als Entwurf speichern** beim Anlegen oder **Duplizieren** eines Sprechtags | Der Zugangs-Link entsteht und ändert sich danach nie mehr. |
| *Entwurf* | *Aktiv* | **Aktiv setzen** in der Liste, oder **Sprechtag anlegen** im Formular, das anlegt und veröffentlicht in einem Zug | Je Lehrkraft der eingeladenen Klassen entsteht für jeden Slot ein freier Termin. Datum, Zeitfenster, Slot-Länge und Klassen liegen ab jetzt fest. |
| *Aktiv* | *Entwurf* | **Zurück auf Entwurf** — nur, solange nie gebucht wurde | Die Termine werden verworfen und beim nächsten Veröffentlichen neu erzeugt. Niemand bekommt eine E-Mail. |
| *Aktiv* | *Abgesagt* | **Absagen** | Jede Familie mit Termin bekommt eine E-Mail. Erinnerungen gehen keine mehr hinaus. |
| *Aktiv* | *Abgeschlossen* | Die Anwendung, in der Nacht nach dem Ende des Sprechtags | Niemand bekommt eine E-Mail. Nachtragen, Umbuchen und Stornieren sind danach nicht mehr möglich. |
| *Entwurf* | gelöscht | **Löschen** | Der Entwurf verschwindet ganz. Das geht nur mit einem Entwurf. |

Die Fallseiten dazu: [Einen Sprechtag anlegen](/anwendungsfaelle/einen-sprechtag-anlegen/),
[Einen Sprechtag veröffentlichen](/anwendungsfaelle/einen-sprechtag-veroeffentlichen/),
[Ein Veröffentlichen zurücknehmen](/anwendungsfaelle/ein-veroeffentlichen-zuruecknehmen/),
[Einen Sprechtag absagen](/anwendungsfaelle/einen-sprechtag-absagen/) und
[Einen Entwurf löschen](/anwendungsfaelle/einen-entwurf-loeschen/).

### Was nach dem Veröffentlichen fest liegt

Ab *Aktiv* lassen sich **Datum**, **Startzeit**, **Endzeit**, **Slot-Länge** und **Klassen** nicht
mehr ändern, denn aus ihnen sind die Termine entstanden, und Familien haben darauf gebucht. Titel,
Ort, Beschreibung, Schulkontakt, Erinnerung und Anmeldefrist bleiben änderbar. *Abgesagt* und
*Abgeschlossen* sind Endzustände: Von dort führt kein Weg zurück. Siehe
[Einen Sprechtag bearbeiten](/anwendungsfaelle/einen-sprechtag-bearbeiten/).

### Anmeldung beendet

*Anmeldung beendet* ist kein eigener Status. Der Sprechtag bleibt *Aktiv*; nur der Zugangs-Link
nimmt nach dem Anmeldeschluss keine Buchungen mehr an. Sie können weiter
[nachtragen](/anwendungsfaelle/eine-familie-nachtragen/), umbuchen und stornieren, bis der
Sprechtag abgeschlossen ist. Mit einer kürzeren Anmeldefrist öffnet sich der Link wieder:
[Die Anmeldung verlängern oder wieder öffnen](/anwendungsfaelle/die-anmeldung-verlaengern-oder-wieder-oeffnen/).

### Was die Anwendung von selbst erledigt

Drei Abläufe laufen ohne Ihr Zutun, jeweils einmal am Tag:

- **Erinnerung**, am Morgen: An *aktiven* Sprechtagen mit Erinnerung bekommt jede Familie mit Termin
  so viele Tage vorher eine E-Mail, wie beim Sprechtag eingetragen sind. Wer erst nach diesem
  Versand bucht, bekommt keine Erinnerung mehr; ein verpasster Tag wird nicht nachgeholt.
- **Abschluss**, in der Nacht: Jeder *aktive* Sprechtag, dessen Ende vorbei ist, wird
  *Abgeschlossen*.
- **Angaben entfernen**, in der Nacht: Ist die Aufbewahrungsfrist nach dem Ende eines Sprechtags
  abgelaufen, üblicherweise nach 30 Tagen, ersetzt die Anwendung die Namen und E-Mail-Adressen
  aller seiner Buchungen durch Platzhalter und löscht die Notizen. Das gilt für abgeschlossene
  ebenso wie für abgesagte Sprechtage. In der Liste steht bis dahin
  *Personenbezogene Angaben bis …*.

Die Uhrzeiten stellt die Schul-IT ein, siehe [Konfiguration](/betrieb/konfiguration/#zeitgesteuerte-läufe).

## Termin

Ein Termin ist ein Slot einer Lehrkraft, etwa 14:20 bis 14:30 Uhr. Er entsteht beim
Veröffentlichen.

| Zustand | Was er bedeutet | Wie er entsteht |
|---|---|---|
| frei | Eltern können ihn wählen. | Beim Veröffentlichen; wieder, wenn seine Buchung storniert oder umgebucht wird |
| belegt | Eine geltende Buchung liegt darauf. | Eine Familie bucht, oder Sie tragen nach oder buchen um |
| entfallen | Die Lehrkraft steht nicht bereit. Eltern sehen ihn nicht mehr. | [Eine Lehrkraft fällt aus](/anwendungsfaelle/eine-lehrkraft-faellt-aus/) |

Ob ein Termin belegt ist, ergibt sich allein aus seinen Buchungen. Ein entfallener Termin wird
nicht wieder frei, und eine Buchung darauf wird dabei storniert.

## Buchung

| Zustand | Was er bedeutet |
|---|---|
| zugesagt | Die Buchung gilt. Sie steht im Plan der Auswertung und auf dem Tagesplan-Blatt. |
| storniert | Die Buchung gilt nicht mehr, ihr Termin ist frei. In der Auswertung erscheint sie nur mit **Stornierte anzeigen**. Endgültig. |

Eine Buchung wird storniert, wenn Sie sie [stornieren](/anwendungsfaelle/eine-buchung-stornieren/),
wenn ihre Lehrkraft ausfällt oder wenn Sie sie [umbuchen](/anwendungsfaelle/eine-buchung-umbuchen/)
— dann ersetzt sie eine neue Buchung auf dem neuen Termin. Die Angaben der Familie können in
beiden Zuständen entfernt sein; die Buchung bleibt dann mit Platzhaltern stehen und zählt weiter
zur Auslastung.

Welche Wechsel eine E-Mail an die Familie auslösen, steht unter
[Welche E-Mails Eltern bekommen](/referenz/welche-e-mails-eltern-bekommen/).
