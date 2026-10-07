---
title: Mail
description: Die Anwendung an einen SMTP-Server anbinden, den Versand prüfen — und was geschieht, wenn eine E-Mail nicht hinausgeht.
sidebar:
  order: 3
---
Die Anwendung schreibt den Eltern E-Mails: die **Buchungsbestätigung** nach einer Buchung oder
Umbuchung, die **Erinnerung** vor dem Sprechtag, die Nachricht beim **Ausfall** einer Lehrkraft und
die **Absage** eines Sprechtags. Den Wortlaut zeigt
[Welche E-Mails Eltern bekommen](/referenz/welche-e-mails-eltern-bekommen/). Lehrkräfte und
Organisator bekommen keine E-Mails.

Für den Versand braucht die Anwendung einen **SMTP-Server**, über den sie sich anmeldet und
versendet. Einen eigenen Mailserver bringt sie nicht mit.

## Anbinden

Sie tragen den Mailserver in der `.env` ein, wie unter [Installation](/betrieb/installation/)
gezeigt. Jeden Wert mit Default und Bedeutung listet
[Konfiguration › Mail](/betrieb/konfiguration/#mail).

```sh
SPRING_MAIL_HOST=smtp.schule.de
SPRING_MAIL_USERNAME=elternsprechtag@schule.de
SPRING_MAIL_PASSWORD=
ELTERNSPRECHTAG_MAIL_ABSENDER=elternsprechtag@schule.de
```

- **Port und Verschlüsselung:** Ohne weitere Angabe verbindet sich die Anwendung auf Port 587 und
  verschlüsselt per STARTTLS. Das passt zu den meisten Mailservern. Verlangt Ihrer Port 465 mit
  sofortiger Verschlüsselung (SMTPS), setzen Sie `SPRING_MAIL_PORT=465`,
  `SPRING_MAIL_STARTTLS_ENABLE=false` und `SPRING_MAIL_PROPERTIES_MAIL_SMTP_SSL_ENABLE=true`.
- **Absender:** Die Adresse in `ELTERNSPRECHTAG_MAIL_ABSENDER` muss der Mailserver für das
  angemeldete Konto zulassen. Viele Server lehnen sonst jede E-Mail ab. Antworten die Eltern auf eine
  E-Mail, gehen die Antworten an diese Adresse; nehmen Sie also ein Postfach, das jemand liest, oder
  nennen Sie im Schulkontakt des Sprechtags, wie die Eltern die Schule erreichen.
- **Zeitgrenzen:** Von sich aus setzt die Anwendung keine; bei einem Mailserver, der nicht
  antwortet, wartet sie Minuten oder unbegrenzt. Setzen Sie deshalb Zeitgrenzen in Millisekunden, etwa
  `SPRING_MAIL_PROPERTIES_MAIL_SMTP_CONNECTIONTIMEOUT=10000`,
  `SPRING_MAIL_PROPERTIES_MAIL_SMTP_TIMEOUT=30000` und
  `SPRING_MAIL_PROPERTIES_MAIL_SMTP_WRITETIMEOUT=30000`. Eine E-Mail, die länger braucht, gilt dann
  als gescheitert.
- **Zustellbarkeit:** Damit die E-Mails nicht im Spam landen, muss der Mailserver für die Domain
  der Absenderadresse versenden dürfen (SPF, DKIM). Mit dem Mailserver der Schule ist das in der
  Regel schon so eingerichtet.

Nach einer Änderung an der `.env` starten Sie die Anwendung neu: `docker compose up -d`.

## Prüfen

Ob der Versand funktioniert, sehen Sie an einer echten Buchung. Legen Sie einen Sprechtag an, buchen
Sie über seinen Zugangs-Link einen Termin mit Ihrer eigenen Adresse und warten Sie auf die
Buchungsbestätigung. Die Buchung stornieren Sie danach wieder, den Sprechtag sagen Sie ab.

Kommt keine E-Mail an, sehen Sie im Log nach:

```sh
docker compose logs app | grep -i -E "fehlgeschlagen|kein SMTP"
```

## Wenn der Versand scheitert

**Die Anwendung versendet jede E-Mail im Hintergrund, nach der Handlung, die sie auslöst.** Die
Buchung, die Absage oder der Ausfall gilt in jedem Fall; die Eltern sehen auf der Seite, dass ihre
Buchung angenommen ist, auch wenn die Bestätigung danach scheitert.

Lehnt der Mailserver eine E-Mail ab oder ist er nicht erreichbar:

- **Die Anwendung versucht es nicht noch einmal.** Die E-Mail ist verloren.
- **Das Log** nennt die Empfängeradresse und den Grund, etwa
  `Buchungsbestätigung an anna@example.org fehlgeschlagen: Mail server connection failed`.
- **Der Organisator sieht es in der Anwendung.** In **Elternsprechtage verwalten** steht beim
  Sprechtag ein Hinweis wie **1 Nachricht nicht zugestellt**, und die Auswertung listet die Familie
  im Block **Nicht erreicht**. Was er dann tut, steht unter
  [Nicht erreichte Familien nachfassen](/anwendungsfaelle/nicht-erreichte-familien-nachfassen/).

**Nicht erkannt** wird, was erst später scheitert: Nimmt der Mailserver die E-Mail an und meldet
danach, dass die Adresse nicht existiert, geht diese Meldung an die Absenderadresse, nicht an die
Anwendung. Ein Postfach, das jemand liest, fängt sie auf.

### Wenn der Mailserver länger ausfällt

Jede E-Mail, die in dieser Zeit fällig wird, scheitert und erscheint unter **Nicht erreicht**.
Erinnerungen werden nicht nachgeholt; ihr Tag ist danach verpasst. Sagen Sie dem Organisator
Bescheid, damit er die betroffenen Familien anruft.

## Ohne Mailserver

Ist `SPRING_MAIL_HOST` nicht gesetzt, versendet die Anwendung **nichts** und schreibt stattdessen
Empfänger und Betreff jeder E-Mail ins Log:

```
Benachrichtigung (kein SMTP konfiguriert) an anna@example.org — "Ihre Termine am …"
```

Die Oberfläche verhält sich dabei unverändert, und der Hinweis auf nicht zugestellte Nachrichten
erscheint nicht. Für eine Instanz, in der Eltern buchen, ist der Mailserver deshalb faktisch Pflicht:
Die Buchungsbestätigung ist der einzige Beleg, den die Eltern über ihre Termine bekommen.

Ein **leer** gesetztes `SPRING_MAIL_HOST=` schaltet den Versand dagegen ein, und jede E-Mail
scheitert. Setzen Sie die Variable mit einem echten Server oder lassen Sie die Zeile ganz weg.
