---
title: Wann personenbezogene Angaben entfernt werden
description: Nach Ablauf der Aufbewahrungsfrist ersetzt die Anwendung die Angaben der Familien von selbst durch Platzhalter — wann das geschieht, woran man es sieht und was es nicht erfasst.
sidebar:
  order: 2
---
Die Angaben der Familien bleiben nur eine begrenzte Zeit nach dem Sprechtag gespeichert. Danach
ersetzt die Anwendung sie in der Nacht von selbst durch Platzhalter. Sie müssen dafür nichts tun.

## Die Aufbewahrungsfrist

- **Gezählt wird ab dem Ende des Sprechtags**, also ab Datum und Endzeit, nicht ab dem Tag, an dem
  er *Abgeschlossen* wurde. Das gilt auch für abgesagte Sprechtage.
- **Üblich sind 30 Tage.** Die Schul-IT stellt die Frist ein, mindestens einen Tag; siehe
  [Konfiguration](/betrieb/konfiguration/#aufbewahrungsfrist). Im Formular des Sprechtags gibt es
  sie nicht, sie gilt für alle Sprechtage gleich.
- **Eine eigene, frühere Frist für die Notiz gibt es nicht.** Sie fällt zusammen mit den übrigen
  Angaben.

## Was entfernt wird und was bleibt

In der Nacht nach Ablauf der Frist ersetzt die Anwendung bei jeder Buchung des Sprechtags

- den **Namen des Kindes** und den **Namen der Eltern** durch Platzhalter,
- die **E-Mail-Adresse** durch eine Adresse, die keine Post annimmt,

und löscht die **Notiz**. Das gilt auch für stornierte Buchungen.

**Stehen bleiben** der Termin, die Lehrkraft, die Klasse und das Fach. Daraus sieht die Schule,
wie der Sprechtag ausgelastet war, und plant den nächsten. In einer kleinen Klasse lässt sich eine
einzelne Zeile mit der Klassenliste unter Umständen wieder einem Kind zuordnen. Ob das hinnehmbar
ist, beurteilen Sie als Verantwortliche.

Das Entfernen ist **endgültig**. Auch eine später verlängerte Frist holt die Namen nicht zurück.

## Woran man es sieht

- **Vorher, in der Liste:** In **Elternsprechtage verwalten** steht unter dem Titel eines
  abgeschlossenen oder abgesagten Sprechtags **Personenbezogene Angaben bis …** mit dem letzten Tag,
  an dem die Angaben noch gespeichert sind.
- **Danach, in der Liste:** Dort steht **Personenbezogene Angaben entfernt am …**.
- **Danach, in der Auswertung:** Sie zeigt den Hinweis **Personenbezogene Angaben entfernt**. Die
  Namen in der Tabelle sind dann Platzhalter. Auch die Tagespläne tragen diesen Vermerk im Kopf.

## Was das Entfernen nicht erfasst

- **Datensicherungen.** Eine Sicherung der Datenbank, die vor dem Entfernen entstand, enthält die
  Angaben weiter. Wie lange Sie Sicherungen aufbewahren, regeln Sie selbst.
- **Versandte E-Mails.** Was in den Postfächern der Eltern, im Sekretariat oder beim Mailanbieter
  liegt, erreicht die Anwendung nicht.
- **Ausgedruckte Tagespläne und heruntergeladene Dateien.** Was Sie ausgedruckt oder gespeichert
  haben, entsorgen Sie selbst.

## Früher entfernen

Verlangt eine Familie die Löschung ihrer Daten vor Ablauf der Frist, entfernen Sie als Organisator
ihre Angaben sofort. Es wirkt dieselbe Ersetzung wie nach der Frist. Siehe
[Löschverlangen](/datenschutz/loeschverlangen/).
