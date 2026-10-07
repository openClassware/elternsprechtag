---
title: Installation
description: Eine eigene Instanz mit Docker Compose aufsetzen — Anwendung, PostgreSQL und ein Reverse Proxy, der das TLS-Zertifikat selbst holt.
sidebar:
  order: 1
---
Diese Seite führt durch eine eigene Instanz für Ihre Schule: die Anwendung, ihre
**PostgreSQL**-Datenbank und davor einen **Reverse Proxy**, der die Verbindung per TLS verschlüsselt.
Am Ende erreichen Organisator und Eltern die Anwendung unter einer eigenen Adresse mit `https://`.

Der beschriebene Weg nutzt **Docker Compose** und **Caddy** als Reverse Proxy, weil Caddy das
Zertifikat von Let's Encrypt ohne weiteres Zutun holt und erneuert. Einen anderen Reverse Proxy
können Sie ebenso nehmen; was er leisten muss, steht unter
[Einen anderen Reverse Proxy nutzen](#einen-anderen-reverse-proxy-nutzen).

## Voraussetzungen

- **Ein Linux-Server** mit Docker und dem Compose-Plugin: `docker compose version` muss laufen.
  Für eine Schule genügt ein kleiner Server mit 2 GB Arbeitsspeicher.
- **Eine Adresse** wie `elternsprechtag.schule.de`, deren DNS-Eintrag (A-Record, bei IPv6 zusätzlich
  AAAA) auf den Server zeigt. Er muss stehen, **bevor** Sie den Stack starten: Caddy holt das
  Zertifikat beim ersten Start und braucht dafür die Adresse.
- **Offene Ports** 80 und 443 (TCP) von außen, 443 (UDP) für HTTP/3. Die Datenbank bekommt keinen
  Port nach außen.
- **Ein Mailserver** (SMTP), über den die Anwendung den Eltern schreibt, siehe [Mail](/betrieb/mail/).
  Ohne ihn läuft die Anwendung, verschickt aber keine E-Mails.

## Schritte

### 1. Verzeichnis anlegen

```sh
sudo install -d -m 750 /opt/elternsprechtag
cd /opt/elternsprechtag
```

Alle drei Dateien dieser Anleitung liegen dort: `compose.yaml`, `Caddyfile` und `.env`.

### 2. `compose.yaml` anlegen

```yaml
services:
  app:
    image: ghcr.io/openclassware/elternsprechtag:latest
    restart: always
    depends_on:
      database:
        condition: service_healthy
    # Alle Werte aus der .env, dazu die festen Werte darunter.
    env_file: .env
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://database:5432/elternsprechtag
      SPRING_DATASOURCE_USERNAME: elternsprechtag
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD:?POSTGRES_PASSWORD fehlt in .env}
      ELTERNSPRECHTAG_OEFFENTLICHE_URL: https://${DOMAIN:?DOMAIN fehlt in .env}
      # TLS endet bei Caddy: Die Anwendung soll ihre Adressen aus den X-Forwarded-Headern bauen.
      SERVER_FORWARD_HEADERS_STRATEGY: framework
      VAADIN_LAUNCH_BROWSER: 'false'
      # Ohne Zeitzone rechnet der Container in UTC: Anmeldeschluss und nächtliche Läufe
      # verschöben sich um ein bis zwei Stunden.
      TZ: Europe/Berlin
    expose:
      - '8080'
    logging:
      driver: json-file
      options:
        max-size: 10m
        max-file: '5'

  database:
    image: postgres:17
    restart: always
    environment:
      POSTGRES_DB: elternsprechtag
      POSTGRES_USER: elternsprechtag
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:?POSTGRES_PASSWORD fehlt in .env}
    healthcheck:
      test: ['CMD-SHELL', 'pg_isready -U elternsprechtag -d elternsprechtag']
      interval: 5s
      timeout: 5s
      retries: 20
    volumes:
      - postgres_data:/var/lib/postgresql/data

  caddy:
    image: caddy:2-alpine
    restart: always
    depends_on:
      - app
    environment:
      DOMAIN: ${DOMAIN:?DOMAIN fehlt in .env}
      ACME_EMAIL: ${ACME_EMAIL:?ACME_EMAIL fehlt in .env}
    ports:
      - '80:80'
      - '443:443'
      - '443:443/udp'
    volumes:
      - ./Caddyfile:/etc/caddy/Caddyfile:ro
      - caddy_data:/data
      - caddy_config:/config

volumes:
  postgres_data:
  caddy_data:
  caddy_config:
```

Die Datenbank hat absichtlich kein `ports:`. Sie ist nur für die Anwendung im internen Netz des
Stacks erreichbar. Das Volume `caddy_data` bewahrt die Zertifikate über Neustarts hinweg; ohne es
holte Caddy bei jedem Start ein neues und liefe in die Grenzen von Let's Encrypt.

Die Version der Datenbank steht fest auf `17`. So hebt ein `docker compose pull` sie nicht
unbemerkt auf eine neue Hauptversion, deren Datenformat die alte nicht mehr liest.

### 3. `Caddyfile` anlegen

```
{
	email {$ACME_EMAIL}
}

{$DOMAIN} {
	encode zstd gzip
	reverse_proxy app:8080
}
```

Mehr braucht es nicht: Caddy holt das Zertifikat, leitet `http://` auf `https://` um und reicht die
WebSocket-Verbindungen der Oberfläche durch.

### 4. `.env` anlegen

Die Datei hält alle Werte Ihrer Schule und die Geheimnisse. Sie gehört nur dem Benutzer, der den
Stack betreibt:

```sh
touch .env && chmod 600 .env
```

```sh
# Adresse der Instanz und Kontakt für Let's Encrypt (Ablaufwarnungen des Zertifikats)
DOMAIN=elternsprechtag.schule.de
ACME_EMAIL=it@schule.de

# Passwort der Datenbank — frei wählen, nur die Anwendung braucht es
POSTGRES_PASSWORD=ein-langes-zufaelliges-passwort

# Zugang des Organisators: Benutzername und bcrypt-Hash des Passworts, siehe unten
ORGANIZER_USERNAME=sekretariat
ORGANIZER_PASSWORD_HASH=

# Ihre Schule
ELTERNSPRECHTAG_SCHOOLNAME=Gesamtschule Musterstadt
ELTERNSPRECHTAG_STELLVERTRETERADRESSE=sekretariat@schule.de

# Mailserver
SPRING_MAIL_HOST=smtp.schule.de
SPRING_MAIL_USERNAME=elternsprechtag@schule.de
SPRING_MAIL_PASSWORD=
ELTERNSPRECHTAG_MAIL_ABSENDER=elternsprechtag@schule.de
```

Was jeder Wert bewirkt und welche es noch gibt, steht unter
[Konfiguration](/betrieb/konfiguration/). Eine Zeile, die Sie nicht brauchen, löschen Sie ganz.
Lassen Sie keine leer stehen: Ein leeres `SPRING_MAIL_HOST=` etwa schaltet den Versand ein, und jede
E-Mail scheitert.

**Den Hash des Organisator-Passworts** erzeugen Sie so:

```sh
docker run --rm httpd:alpine htpasswd -nbBC 10 "" 'GeheimesPasswort' | cut -d: -f2 | sed 's/\$/$$/g'
```

Das `sed` am Ende verdoppelt jedes `$` zu `$$`. Das braucht die `.env`, sonst liest Compose die Teile
des Hashs als Variablen. Tragen Sie die Ausgabe **ohne** das Präfix `{bcrypt}` hinter
`ORGANIZER_PASSWORD_HASH=` ein. Warum, steht unter
[Konfiguration](/betrieb/konfiguration/#zugang-des-organisators).

### 5. Starten

```sh
docker compose up -d
docker compose logs -f app
```

Beim ersten Start legt die Anwendung das Schema der Datenbank an. Fertig ist sie, wenn im Log
`Started ElternsprechtagApplication` steht. Mit <kbd>Strg</kbd>+<kbd>C</kbd> beenden Sie nur die Anzeige des Logs,
nicht die Anwendung.

### 6. Prüfen

1. Öffnen Sie `https://elternsprechtag.schule.de`. Der Browser zeigt ein gültiges Zertifikat und die
   Anmeldung.
2. Melden Sie sich mit Benutzername und Passwort des Organisators an.
3. Die Anwendung ist leer: Es gibt noch keine Klassen, Lehrkräfte und Fächer. Legen Sie sie an, wie
   unter [Stammdaten anlegen](/betrieb/stammdaten-anlegen/) beschrieben.
4. Richten Sie die [Datensicherung](/betrieb/datensicherung-und-wiederherstellung/) ein, bevor die
   ersten Eltern buchen.

## Einen anderen Reverse Proxy nutzen

Betreiben Sie schon einen Reverse Proxy, etwa nginx oder Apache, lassen Sie den Dienst `caddy` weg
und geben der Anwendung einen Port auf dem Server, zum Beispiel `ports: ['127.0.0.1:8080:8080']`
statt `expose:`. Der Proxy muss dann:

- **TLS terminieren**, mit einem gültigen Zertifikat für die Adresse der Instanz;
- **die Header** `X-Forwarded-For`, `X-Forwarded-Proto` und `X-Forwarded-Host` setzen. Ohne sie leitet
  die Anwendung nach der Anmeldung auf `http://` und den internen Port um;
- **WebSockets durchreichen** (`Upgrade` und `Connection`), denn die Oberfläche hält eine dauerhafte
  Verbindung zum Server;
- **lange Verbindungen zulassen**: Eine Zeitgrenze von wenigen Minuten für Lesen und Schreiben
  trennt die Verbindung der Oberfläche, und der Organisator sieht eine Meldung über den
  Verbindungsverlust.

Die Adresse, unter der die Eltern die Anwendung erreichen, steht in
`ELTERNSPRECHTAG_OEFFENTLICHE_URL`. Sie muss zu dem passen, was der Proxy ausliefert.

## Wie es weitergeht

- [Mail](/betrieb/mail/) — den Versand prüfen.
- [Stammdaten anlegen](/betrieb/stammdaten-anlegen/) — Klassen, Lehrkräfte, Fächer und Lehraufträge.
- [Datensicherung und Wiederherstellung](/betrieb/datensicherung-und-wiederherstellung/).
- [Updates](/betrieb/updates/) — eine neue Fassung einspielen.
- [Logs und Fehlersuche](/betrieb/logs-und-fehlersuche/) — wenn etwas nicht startet.
