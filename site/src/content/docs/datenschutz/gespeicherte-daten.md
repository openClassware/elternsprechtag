---
title: Gespeicherte Daten
description: Welche Daten die Anwendung speichert, woher sie kommen und an wen sie gehen — als Grundlage für die Frage, ob eine Datenschutzprüfung nötig ist.
sidebar:
  order: 1
---
Diese Seite hilft einzuschätzen, ob und in welchem Umfang eine Datenschutzprüfung nötig ist. Sie
ist **keine Muster-Datenschutzerklärung**; warum es keine gibt, steht unter
[Verantwortlichkeit](/datenschutz/verantwortlichkeit/).

## Stammdaten

Die Schul-IT legt sie an. Sie beschreiben die Schule:

- **Lehrkräfte**: Vorname, Nachname, Kürzel.
- **Klassen**: Bezeichnung, etwa `10a`.
- **Fächer**: Name und Kürzel.
- **Lehraufträge**: welche Lehrkraft welche Klasse in welchem Fach unterrichtet.

## Sprechtage und Termine

Der Organisator legt sie an:

- **Sprechtage**: Titel, Datum, Zeitfenster, Dauer eines Termins, Ort, Hinweistext, Schulkontakt,
  teilnehmende Klassen, Status und der **Zugangs-Link**. Über ihn erreichen Eltern den Sprechtag
  ohne Anmeldung — wer den Link kennt, sieht den Sprechtag.
- **Termine**: die Zeitfenster je Lehrkraft und Sprechtag.

## Angaben der Eltern

Je Buchung geben die Eltern ein:

- den **Namen des Kindes**,
- ihren **eigenen Namen**,
- ihre **E-Mail-Adresse**. Sie ist Pflicht: Nur über sie erreicht die Schule die Familie, wenn
  ein Sprechtag abgesagt wird oder eine Lehrkraft ausfällt, und an sie gehen Buchungsbestätigung
  und Erinnerung.
- eine **freiwillige Notiz** an die Lehrkraft.

Dazu speichert die Anwendung den Zeitpunkt der Buchung, den gebuchten Termin und den Lehrauftrag.
Trägt der Organisator eine Familie nach, gibt er dieselben Angaben ein.

Die Notiz ist ein freies Textfeld: Was Eltern hineinschreiben, bestimmt mit, wie sensibel die
Daten sind. Weisen Sie die Eltern deshalb darauf hin, dort keine Angaben zur Gesundheit oder
andere besonders schützenswerte Daten einzutragen.

## Weitere Verarbeitung

- **Kein Konto für Eltern.** Name und E-Mail-Adresse hängen an der einzelnen Buchung, nicht an
  einer Person. Auch Lehrkräfte haben keine Konten und keine Passwörter.
- **E-Mails.** Buchungsbestätigung und Absage enthalten den Sprechtag, den Ort, den Namen des
  Kindes und die Klasse, die gebuchten Zeiten mit Lehrkraft und Fach sowie die Notiz. Alle E-Mails
  gehen über den Mailserver, den die Schul-IT einträgt — dessen Anbieter verarbeitet die Daten mit.
  Den Wortlaut zeigt [Welche E-Mails Eltern bekommen](/referenz/welche-e-mails-eltern-bekommen/).
- **Logs.** Ist kein Mailserver eingetragen, schreibt die Anwendung Empfängeradresse und Betreff
  jeder nicht versandten E-Mail ins Anwendungslog. Siehe
  [Konfiguration](/betrieb/konfiguration/#mail).
- **Kein Tracking.** Die Anwendung bindet keine externen Dienste, keine Analysewerkzeuge und keine
  Inhalte fremder Server ein. Die Sitzung hält der Server; dafür setzt die Anwendung ein
  Sitzungs-Cookie.
- **Entfernen nach der Aufbewahrungsfrist.** Die Angaben der Eltern bleiben nicht dauerhaft
  gespeichert. Siehe
  [Wann personenbezogene Angaben entfernt werden](/datenschutz/wann-personenbezogene-angaben-entfernt-werden/).
- **Löschen auf Verlangen.** Verlangt eine Familie vorher die Löschung, entfernt der Organisator
  ihre Angaben sofort. Siehe [Löschverlangen](/datenschutz/loeschverlangen/).
