---
title: Logs und Fehlersuche
description: Wo die Anwendung protokolliert, was die nächtlichen Läufe melden und was hinter den häufigsten Fehlern beim Start, bei der Anmeldung und beim Versand steckt.
sidebar:
  order: 6
---
Die Anwendung schreibt ihr Log auf die Standardausgabe, eine Logdatei legt sie nicht an. Im Stack
aus der [Installation](/betrieb/installation/) sammelt Docker das Log; die Befehle unten laufen im
Verzeichnis `/opt/elternsprechtag`.

## Logs lesen

```sh
docker compose logs -f app             # Anwendung, laufend
docker compose logs --since 24h app    # Anwendung, letzte 24 Stunden
docker compose logs caddy              # Reverse Proxy: Zertifikat, Weiterleitung
docker compose logs database           # Datenbank
```

Jede Zeile beginnt mit dem Zeitpunkt, in der Zeitzone aus `TZ`, und der Stufe: `INFO` für den gewöhnlichen Betrieb, `WARN` für
etwas, das nicht geklappt hat, ohne die Anwendung aufzuhalten, etwa eine gescheiterte E-Mail, und
`ERROR` für einen Fehler, dem Sie nachgehen sollten.

Im Normalbetrieb finden Sie:

| Meldung | Bedeutung |
|---|---|
| `Started ElternsprechtagApplication in … seconds` | Die Anwendung ist gestartet und bereit. |
| `Successfully applied … migrations` oder `Schema "public" is up to date` | Das Schema der Datenbank ist auf dem Stand dieser Fassung, siehe [Updates](/betrieb/updates/). |
| `Erinnerungs-Lauf: … Buchung(en) erinnert` | Der tägliche Versand der Erinnerungen. |
| `Abschluss-Lauf: … Sprechtag(e) abgeschlossen` | Vorbei gegangene Sprechtage wurden *Abgeschlossen*. |
| `Anonymisierungs-Lauf: … Sprechtag(e) anonymisiert` | Nach Ablauf der Aufbewahrungsfrist wurden die Angaben der Eltern entfernt. |

Wann die drei Läufe stattfinden, steht unter
[Konfiguration › Zeitgesteuerte Läufe](/betrieb/konfiguration/#zeitgesteuerte-läufe).

### Wie lange Logs bleiben

Die `compose.yaml` der Installation begrenzt das Log der Anwendung auf fünf Dateien zu je 10 MB;
ältere Einträge fallen von selbst weg. Ohne diese Begrenzung wächst das Log, bis die Festplatte voll
ist. Beim Neuanlegen des Containers, etwa bei einem Update, beginnt das Log von vorn.

**Das Log enthält E-Mail-Adressen von Eltern:** bei jeder gescheiterten E-Mail und, solange kein
Mailserver eingetragen ist, bei jeder E-Mail. Das Entfernen der personenbezogenen Angaben nach der
Aufbewahrungsfrist erreicht das Log nicht. Leiten Sie das Log deshalb nicht in ein System weiter,
das es länger aufbewahrt, ohne das im Löschkonzept der Schule zu berücksichtigen.

### Mehr protokollieren

Für die Fehlersuche lässt sich das Log einzelner Teile ausführlicher schalten, über eine
Umgebungsvariable `LOGGING_LEVEL_<Paket>` in der `.env`, etwa für den Mailversand:

```sh
LOGGING_LEVEL_ORG_ECLIPSE_ANGUS_MAIL=DEBUG
```

Nach `docker compose up -d` gilt die Einstellung. Das Log zeigt dann jeden Schritt der Verbindung
zum Mailserver, etwa `trying to connect to host "smtp.schule.de", port 587`. Entfernen Sie die Zeile
nach der Fehlersuche wieder, sonst wächst das Log schnell.

## Häufige Fehler

### Die Anwendung startet nicht

`docker compose ps` zeigt die Anwendung als `Restarting`, oder sie fehlt. Die Ursache steht am Ende
von `docker compose logs app`, meist nach `APPLICATION FAILED TO START` oder in der letzten Zeile mit
`Caused by:`.

| Im Log | Ursache und Abhilfe |
|---|---|
| `elternsprechtag.oeffentliche-url ist nicht gesetzt` | Die Adresse für die Eltern fehlt. Setzen Sie `ELTERNSPRECHTAG_OEFFENTLICHE_URL`, siehe [Konfiguration](/betrieb/konfiguration/#öffentliche-adresse). |
| `elternsprechtag.oeffentliche-url ist keine absolute http(s)-Adresse` | Die Adresse hat keinen `https://`-Anfang oder enthält `?` oder `#`. |
| `password authentication failed for user` | Das Datenbank-Passwort stimmt nicht. Es wird nur beim ersten Start der Datenbank übernommen; wer `POSTGRES_PASSWORD` danach ändert, ändert es nicht in der Datenbank. |
| `Connection to database:5432 refused` oder `UnknownHostException` | Die Datenbank läuft nicht oder heißt anders. Prüfen Sie `docker compose ps` und `docker compose logs database`. |
| `FlywayValidateException` oder `Migration … failed` | Eine Migration des Schemas ist gescheitert, siehe [Updates › Wenn die Anwendung nach dem Update nicht startet](/betrieb/updates/#-die-anwendung-nach-dem-update-nicht-startet). |

### Die Anmeldung als Organisator scheitert

Die Anmeldeseite meldet falsche Zugangsdaten, obwohl Benutzername und Passwort stimmen:

- **Der Hash steht mit `{bcrypt}` in der `.env`**, oder **die `$` im Hash sind nicht verdoppelt.**
  Das Log meldet dann bei jedem Versuch `Encoded password does not look like BCrypt`. Tragen Sie
  den Hash ohne Präfix ein und schreiben Sie jedes `$` als `$$`.
- **Die Variablen heißen anders als `ORGANIZER_USERNAME` und `ORGANIZER_PASSWORD_HASH`**, etwa
  `ELTERNSPRECHTAG_SECURITY_ORGANIZER_PASSWORD`. Das Log meldet dann
  `There is no default password encoder configured`.

Die Hintergründe beschreibt [Konfiguration › Zugang des Organisators](/betrieb/konfiguration/#zugang-des-organisators).
Nach einer Änderung an der `.env` starten Sie neu: `docker compose up -d`.

### Der Browser meldet ein ungültiges Zertifikat

Caddy hat noch kein Zertifikat bekommen. `docker compose logs caddy` nennt den Grund, meist einen
dieser:

- Der DNS-Eintrag der Adresse zeigt nicht auf diesen Server, oder er ist noch nicht überall bekannt.
- Port 80 oder 443 ist von außen nicht erreichbar, etwa wegen einer Firewall.
- Zu viele Versuche in kurzer Zeit: Let's Encrypt sperrt dann für eine Weile. Warten Sie, statt den
  Stack immer wieder neu zu starten.

### Nach der Anmeldung landet der Browser auf `http://` oder einem Port wie `:8080`

Die Anwendung kennt die Adresse nicht, unter der sie von außen erreicht wird. Prüfen Sie, ob
`SERVER_FORWARD_HEADERS_STRATEGY: framework` gesetzt ist und der Reverse Proxy die Header
`X-Forwarded-Proto` und `X-Forwarded-Host` mitschickt, siehe
[Installation › Einen anderen Reverse Proxy nutzen](/betrieb/installation/#einen-anderen-reverse-proxy-nutzen).

### Die Oberfläche meldet immer wieder eine unterbrochene Verbindung

Der Reverse Proxy reicht die WebSocket-Verbindung der Oberfläche nicht durch oder trennt sie nach
kurzer Zeit. Mit Caddy wie in der Installation tritt das nicht auf; bei einem anderen Proxy siehe
[Installation › Einen anderen Reverse Proxy nutzen](/betrieb/installation/#einen-anderen-reverse-proxy-nutzen).

### Eltern bekommen keine E-Mails

Suchen Sie im Log nach `fehlgeschlagen` und `kein SMTP`. Was die Meldungen bedeuten und was dann
zu tun ist, steht unter [Mail](/betrieb/mail/#wenn-der-versand-scheitert).

### Der Zugangs-Link, den die Eltern bekommen haben, führt ins Leere

Der Zugangs-Link beginnt mit `ELTERNSPRECHTAG_OEFFENTLICHE_URL`. Stimmt die Adresse dort nicht mit
der überein, unter der die Anwendung von außen erreichbar ist, zeigt jeder Link daneben. Korrigieren
Sie den Wert und starten Sie neu; bereits verteilte Links bleiben falsch und müssen neu verteilt
werden.
