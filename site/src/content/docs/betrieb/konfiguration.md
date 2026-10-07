---
title: Konfiguration
description: Jeder Wert, den die Anwendung aus der Umgebung liest, mit Umgebungsvariable, Default und Bedeutung — dazu die Profile und die Fallstricke beim Setzen.
sidebar:
  order: 2
---
Diese Seite listet jeden Wert, den die Anwendung aus der Konfiguration liest: mit
Umgebungsvariable, Default und Bedeutung. Sie richtet sich an die Schul-IT, die eine eigene Instanz
einrichtet. Wie eine Instanz mit Datenbank, Reverse Proxy und TLS aufgesetzt wird, steht unter
[Installation](/betrieb/installation/).

Quelle der Angaben ist die Datei
[`application.properties`](https://github.com/openClassware/elternsprechtag/blob/main/src/main/resources/application.properties)
im Quellcode der Anwendung.

## Wie Werte gesetzt werden

Die Anwendung ist eine Spring-Boot-Anwendung. Jeder Wert unten wird als **Umgebungsvariable**
gesetzt: im Container über `environment:` oder eine `.env`-Datei, beim Start per JAR über die
Umgebung des Prozesses. Eine eigene `application.properties` neben dem JAR funktioniert ebenfalls,
ist aber nicht der vorgesehene Weg.

Zwei Mechanismen greifen dabei nebeneinander:

- **Explizite Platzhalter.** Steht in `application.properties` `${SPRING_MAIL_PORT:587}`, liest
  die Anwendung genau diese Variable und fällt sonst auf `587` zurück.
- **Relaxed Binding.** Zusätzlich übersetzt Spring jede Umgebungsvariable in Großbuchstaben mit
  Unterstrichen in den gleichnamigen Property-Namen: `SPRING_MAIL_HOST` → `spring.mail.host`,
  `ELTERNSPRECHTAG_SCHOOLNAME` → `elternsprechtag.schoolname`. So lassen sich auch Werte setzen,
  für die in `application.properties` kein Platzhalter steht.

Die Defaults sind **Entwicklungswerte**. Für eine echte Instanz setzen Sie mindestens die
Datenbankverbindung und den Zugang des Organisators. Ohne jeden Default ist die
[öffentliche Adresse](#öffentliche-adresse): Ohne sie startet die Anwendung gar nicht.

## Datenbank

Die Anwendung erwartet eine **PostgreSQL**-Datenbank. Das Schema legt **Flyway** an: Die Skripte
liegen im Quellcode unter
[`src/main/resources/db/migration`](https://github.com/openClassware/elternsprechtag/tree/main/src/main/resources/db/migration)
und laufen beim Start der Anwendung von selbst — beim ersten Start auf einer leeren Datenbank
ebenso wie bei jedem Update. Sie sind vor dem Update lesbar. Flyway führt jedes Skript genau einmal
aus und vermerkt das in der Tabelle `flyway_schema_history` derselben Datenbank.

Der Datenbank-Benutzer braucht deshalb Rechte zum Anlegen und Ändern von Tabellen, nicht nur zum
Lesen und Schreiben von Daten.

| Umgebungsvariable | Default | Bedeutung |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/elternsprechtag` | JDBC-URL der Datenbank. |
| `SPRING_DATASOURCE_USERNAME` | `myuser` | Datenbank-Benutzer. |
| `SPRING_DATASOURCE_PASSWORD` | `mysecret` | Passwort des Datenbank-Benutzers. |

Das Schema selbst ist kein Konfigurationspunkt: Es gehört allein Flyway. Die Anwendung erzeugt und
prüft kein Schema, es gibt hier also nichts einzustellen.

## Zugang des Organisators

Es gibt genau **eine** Anmeldung: die des Organisators. Sie liegt nicht in der Datenbank, sondern
kommt aus der Konfiguration. Lehrkräfte und Eltern haben **kein** Login; Eltern buchen ohne
Anmeldung über den Zugangs-Link eines Sprechtags.

| Umgebungsvariable | Default | Bedeutung |
|---|---|---|
| `ORGANIZER_USERNAME` | `user` | Benutzername des Organisators. |
| `ORGANIZER_PASSWORD_HASH` | im Quellcode hinterlegter Hash | **Nackter bcrypt-Hash** des Passworts, ohne Präfix. |

### Fallstrick: Der Hash wird ohne `{bcrypt}`-Präfix gesetzt

Spring Security erwartet Passwörter in der Form `{bcrypt}$2a$10$…`. Das Präfix `{bcrypt}` steht
bereits **fest in `application.properties`**, direkt vor dem Platzhalter:

```properties
elternsprechtag.security.organizer.password={bcrypt}${ORGANIZER_PASSWORD_HASH:…}
```

Der Grund: Der Default eines Platzhalters `${VAR:default}` darf selbst kein `}`
enthalten — stünde `{bcrypt}` im Platzhalter, endete der Ausdruck zu früh. Die Umgebungsvariable
liefert deshalb **nur den Hash**. Wer das Präfix mitgibt, erzeugt `{bcrypt}{bcrypt}$2a$…` und kann
sich nicht anmelden.

Einen Hash erzeugen:

```bash
docker run --rm httpd:alpine htpasswd -nbBC 10 "" 'GeheimesPasswort' | cut -d: -f2
```

Der Hash enthält `$`-Zeichen. Steht er in einer `.env`-Datei für Docker Compose, muss jedes `$`
als `$$` verdoppelt werden, sonst liest Compose die Teile als leere Variablen.

### Fallstrick: Die Variablen heißen bewusst nicht wie die Properties

Die Variablen heißen `ORGANIZER_USERNAME` und `ORGANIZER_PASSWORD_HASH`, **nicht**
`ELTERNSPRECHTAG_SECURITY_ORGANIZER_USERNAME` und `…_PASSWORD`. Mit den langen Namen greift
Relaxed Binding: Der Wert landet direkt auf der Property und verdrängt die Zeile mit dem
`{bcrypt}`-Präfix. Die Anmeldung scheitert dann mit „no default password encoder configured“. Nur
die kurzen Namen sind richtig.

### Fallstrick: Der Default-Zugang ist öffentlich bekannt

Ohne gesetzte Variablen gilt `user` mit dem im Quellcode hinterlegten Hash. Dieser Zugang ist für
die Entwicklung gedacht und für jeden nachlesbar. Für eine erreichbare Instanz setzen Sie beide Werte.

## Mail

Die Anwendung schickt den Eltern E-Mails: die **Buchungsbestätigung** nach einer Buchung oder
Umbuchung, die **Erinnerung** vor dem Sprechtag, die Nachricht beim **Ausfall** einer Lehrkraft und
die **Absage**, wenn ein Sprechtag abgesagt wird. Ihren Wortlaut zeigt
[Welche E-Mails Eltern bekommen](/referenz/welche-e-mails-eltern-bekommen/).

| Umgebungsvariable | Default | Bedeutung |
|---|---|---|
| `SPRING_MAIL_HOST` | *(nicht gesetzt)* | SMTP-Server. **Schaltet den Versand überhaupt erst ein.** |
| `SPRING_MAIL_PORT` | `587` | SMTP-Port. |
| `SPRING_MAIL_USERNAME` | *(leer)* | SMTP-Benutzer. |
| `SPRING_MAIL_PASSWORD` | *(leer)* | SMTP-Passwort. |
| `SPRING_MAIL_SMTP_AUTH` | `true` | Am SMTP-Server anmelden. |
| `SPRING_MAIL_STARTTLS_ENABLE` | `true` | Verbindung per STARTTLS verschlüsseln. |
| `ELTERNSPRECHTAG_MAIL_ABSENDER` | `elternsprechtag@example.com` | Absenderadresse der E-Mails. |
| `ELTERNSPRECHTAG_MAIL_ABLAGE` | *(nicht gesetzt)* | Nur für den Bau dieser Doku-Site: Verzeichnis, in das die Anwendung ohne SMTP jede E-Mail als Textdatei legt. In einer Schulinstanz nicht setzen. |

Die Zeichenkodierung der E-Mails ist fest UTF-8, damit Umlaute und `ß` unabhängig vom System
richtig ankommen.

Weitere Eigenschaften der Mail-Bibliothek setzen Sie über `SPRING_MAIL_PROPERTIES_…`, etwa
Zeitgrenzen und SMTPS auf Port 465. Welche sinnvoll sind, steht unter [Mail](/betrieb/mail/#anbinden).

### Fallstrick: Ohne Mailhost verschickt die Anwendung nichts

`SPRING_MAIL_HOST` hat **absichtlich keinen Default**. Davon hängt ab, wohin eine E-Mail geht:

- **`SPRING_MAIL_HOST` gesetzt:** Die Anwendung versendet über diesen Server.
- **Nicht gesetzt:** Empfänger und Betreff landen nur im Anwendungslog, es geht keine E-Mail
  hinaus. Die Oberfläche verhält sich unverändert, die Eltern bekommen aber nichts.
- **Nicht gesetzt, aber `ELTERNSPRECHTAG_MAIL_ABLAGE`:** Jede E-Mail landet als Textdatei im
  angegebenen Verzeichnis, auch hier geht nichts hinaus. Das braucht nur der Bau dieser Doku-Site,
  der die Mailtexte daraus abgreift. Ein gesetzter Mailhost hat immer Vorrang.

Ein *leer* gesetztes `SPRING_MAIL_HOST=` ist der schlimmste Fall: Spring wertet die Variable dann
als gesetzt, schaltet den Versand ein, und jede E-Mail scheitert zur Laufzeit. Setzen Sie die
Variable mit einem echten Host oder lassen Sie sie ganz weg.

Für den Echtbetrieb ist sie faktisch Pflicht: Die Buchungsbestätigung ist der einzige Beleg, den
Eltern über ihre Termine bekommen.

## Schule

| Umgebungsvariable | Default | Bedeutung |
|---|---|---|
| `ELTERNSPRECHTAG_SCHOOLNAME` | `Gesamtschule Lindenhof` | Name der Schule. Er erscheint in der Oberfläche und als Grußformel in den E-Mails. |

Für diesen Wert steht in `application.properties` kein Platzhalter, er wird über Relaxed Binding
gesetzt. Er ist der Wert, den jede Schule an sich anpasst.

## Öffentliche Adresse

Die Adresse, unter der die **Eltern** die Anwendung erreichen. Mit ihr beginnt der Zugangs-Link
jedes Sprechtags: im Dialog **Link teilen** unter **Elternsprechtage verwalten**, im Formular des
Sprechtags und in der E-Mail beim Ausfall einer Lehrkraft, die vor dem Anmeldeschluss zurück in die
Buchung führt.

| Umgebungsvariable | Default | Bedeutung |
|---|---|---|
| `ELTERNSPRECHTAG_OEFFENTLICHE_URL` | *(keiner — Pflicht)* | Absolute `http(s)`-Adresse, etwa `https://elternsprechtag.schule.de`. Ein Unterpfad ist erlaubt, ein abschließender `/` egal. |

### Fallstrick: Ohne Adresse startet die Anwendung nicht

Fehlt der Wert oder ist er keine absolute `http(s)`-Adresse, bricht der Start mit einer Meldung zu
`elternsprechtag.oeffentliche-url` ab. Das ist Absicht: Mit einem Beispiel-Default verschickte eine
nicht eingerichtete Instanz still Links ins Leere.

Die Adresse kommt bewusst **nicht** aus der Adresszeile des Organisators. Das Sekretariat arbeitet
womöglich im internen Netz unter einer anderen Adresse (`http://elternsprechtag.intern:8080`) als
die Eltern und hätte diese sonst in den Elternbrief kopiert.

## Nachtragen

Trägt der Organisator eine Familie **ohne eigene E-Mail-Adresse** nach, setzt ein Häkchen beim
Nachtragen statt ihrer Adresse eine Stellvertreteradresse der Schule ein — etwa ein Postfach des
Sekretariats, das die E-Mails an diese Familie entgegennimmt.

| Umgebungsvariable | Default | Bedeutung |
|---|---|---|
| `ELTERNSPRECHTAG_STELLVERTRETERADRESSE` | *(nicht gesetzt)* | Stellvertreteradresse für Familien ohne eigene E-Mail-Adresse. |

### Fallstrick: Ohne Adresse erscheint das Häkchen gar nicht

Die Stellvertreteradresse hat **absichtlich keinen Beispiel-Default**, anders als der Name der
Schule. Ein Beispielwert wäre hier kein harmloser Platzhalter: Echte Absagen gingen an eine
erfundene Adresse.

Ist die Variable nicht gesetzt, bietet das Nachtragen das Häkchen
**Familie hat keine eigene E-Mail-Adresse** deshalb gar nicht erst an. Für jede nachgetragene
Familie ist dann eine echte E-Mail-Adresse nötig.

## Aufbewahrungsfrist

Wie lange die personenbezogenen Angaben der Buchungen nach einem Sprechtag gespeichert bleiben.
Gezählt wird ab dem **Ende des Sprechtags** (Datum und Endzeit), auch bei abgesagten Sprechtagen.
Danach ersetzt der nächtliche Lauf die Angaben durch Platzhalter. Was das für die Familien und die
Schule bedeutet, steht unter
[Wann personenbezogene Angaben entfernt werden](/datenschutz/wann-personenbezogene-angaben-entfernt-werden/).

| Umgebungsvariable | Default | Bedeutung |
|---|---|---|
| `ELTERNSPRECHTAG_AUFBEWAHRUNGSFRIST_TAGE` | `30` | Frist in Tagen, mindestens 1. |
| `ELTERNSPRECHTAG_ANONYMISIERUNG_EMAIL` | `noreply@openclassware.de` | Die E-Mail-Adresse, die jede Buchung nach dem Entfernen statt der Elternadresse trägt — nach Ablauf der Frist wie nach einem Löschverlangen. Sie sollte keine Post annehmen und darf nicht leer sein. |

## Zeitgesteuerte Läufe

Drei Vorgänge startet die Anwendung von selbst, jeder zu einer eigenen Uhrzeit. Die Zeiten sind
Spring-Cron-Ausdrücke mit sechs Feldern: Sekunde, Minute, Stunde, Tag, Monat, Wochentag.

| Umgebungsvariable | Default | Bedeutung |
|---|---|---|
| `ELTERNSPRECHTAG_ERINNERUNG_CRON` | `0 0 7 * * *` | Versand der Erinnerungen, täglich um 7 Uhr. |
| `ELTERNSPRECHTAG_ABSCHLUSS_CRON` | `0 30 0 * * *` | Abschluss: Jeder Sprechtag im Status *Aktiv*, dessen Ende vorbei ist, wird *Abgeschlossen*. Täglich um 0:30 Uhr. |
| `ELTERNSPRECHTAG_ANONYMISIERUNG_CRON` | `0 0 1 * * *` | Entfernen der personenbezogenen Angaben nach Ablauf der [Aufbewahrungsfrist](#aufbewahrungsfrist). Täglich um 1 Uhr. |

Ein verpasster Lauf wird nicht nachgeholt; der nächste Lauf erledigt, was dann fällig ist. Eine
Erinnerung, deren Tag verpasst ist, geht allerdings nicht mehr hinaus.

### Fallstrick: Das Entfernen muss nachts laufen

Die Liste **Elternsprechtage verwalten** nennt unter **Personenbezogene Angaben bis …** den letzten
Tag, an dem die Angaben noch gespeichert sind. Das stimmt nur, wenn das Entfernen nach Mitternacht
läuft. Ein Lauf am Abend fände die Angaben schon an diesem letzten Tag fällig.

## Weitere Werte

| Umgebungsvariable | Default | Bedeutung |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | *(keins)* | Aktive [Profile](#profile), durch Komma getrennt. |
| `SERVER_FORWARD_HEADERS_STRATEGY` | *(keins)* | Auf `framework` setzen, wenn ein Reverse Proxy TLS terminiert — sonst leitet die Anwendung auf `http://` und den internen Port weiter. |
| `VAADIN_LAUNCH_BROWSER` | `true` | Öffnet beim Start einen Browser. Im Container auf `false` setzen; das Profil `demo` tut das bereits. |
| `SERVER_PORT` | `8080` | Port, auf dem die Anwendung lauscht (Spring-Boot-Standard, nicht eigens gesetzt). |

## Profile

Profile werden über `SPRING_PROFILES_ACTIVE` eingeschaltet, mehrere durch Komma getrennt
(`SPRING_PROFILES_ACTIVE=local,demo`).

| Profil | Datei | Zweck |
|---|---|---|
| *(keins)* | `application.properties` | Standard. Gilt immer und ist die Grundlage aller Profile. Eine Schulinstanz braucht **kein** Profil, die Werte kommen aus der Umgebung. |
| `local` | `application-local.properties`, nicht im Quellcode | Persönliche Einstellungen für die Entwicklung, etwa ein echter SMTP-Zugang zum Testen oder `elternsprechtag.oeffentliche-url=http://localhost:8080`. Die Datei legt jeder Entwickler selbst an. |
| `demo` | `application-demo.properties` | Die öffentliche Demo: erfundene Stammdaten und Beispiel-Sprechtage, kein Browser beim Start. |

Das Profil `demo` ist **nicht** für den Echtbetrieb: Es füllt die Datenbank mit erfundenen
Lehrkräften, Klassen und Sprechtagen. Das Schema baut es nicht selbst auf, es läuft durch dieselben
Migrationen wie eine Schulinstanz. Die öffentliche Demo setzt ihre Datenbank stattdessen bei jedem
Ausrollen zurück.
