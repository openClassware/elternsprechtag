---
title: Updates
description: Eine neue Fassung einspielen — sichern, das neueste Image ziehen, neu starten; das Schema der Datenbank zieht die Anwendung beim Start selbst nach.
sidebar:
  order: 4
---
Eine neue Fassung der Anwendung ist ein neues Image. Releases mit Versionsnummern gibt es noch
nicht: Sie ziehen das **neueste Image** `ghcr.io/openclassware/elternsprechtag:latest`. Es entsteht
aus dem aktuellen Stand des Quellcodes, nachdem alle automatischen Tests bestanden sind.

Die Befehle unten gehen vom Stack aus der [Installation](/betrieb/installation/) aus und laufen im
Verzeichnis `/opt/elternsprechtag`.

## Schritte

1. **Sichern Sie die Datenbank**, wie unter
   [Datensicherung und Wiederherstellung](/betrieb/datensicherung-und-wiederherstellung/)
   beschrieben. Mit dieser Sicherung kehren Sie zurück, falls das Update scheitert.

2. **Merken Sie sich die laufende Fassung** unter einem eigenen Namen, damit Sie zu ihr
   zurückkönnen:

   ```sh
   docker tag ghcr.io/openclassware/elternsprechtag:latest elternsprechtag:vorher
   ```

3. **Ziehen Sie das neueste Image und starten Sie neu:**

   ```sh
   docker compose pull app
   docker compose up -d
   ```

   Compose ersetzt den Container der Anwendung, wenn sich das Image geändert hat. Datenbank und
   Caddy laufen weiter.

4. **Prüfen Sie den Start** im Log:

   ```sh
   docker compose logs -f app
   ```

   Bereit ist die Anwendung, wenn `Started ElternsprechtagApplication` erscheint.

Wählen Sie für das Update eine Zeit, zu der niemand bucht, etwa abends. Während des Neustarts ist
die Anwendung eine knappe Minute nicht erreichbar; wer in diesem Moment eine Seite offen hat, sieht
eine Meldung über die unterbrochene Verbindung und lädt neu.

## Was beim Start passiert

Bringt die neue Fassung Änderungen am Datenbankschema mit, spielt die Anwendung sie beim Start
selbst ein. Das übernimmt **Flyway**: Es vergleicht die Skripte der neuen Fassung mit der Tabelle
`flyway_schema_history` in der Datenbank und führt nur die aus, die noch fehlen. Im Log steht dann etwas wie:

```
Successfully applied 1 migration to schema "public", now at version v13
```

Ist nichts zu tun, steht dort `Schema "public" is up to date. No migration necessary.` Von Hand
ändern Sie am Schema nichts. Welche Skripte es gibt, ist vor dem Update im Quellcode unter
[`src/main/resources/db/migration`](https://github.com/openClassware/elternsprechtag/tree/main/src/main/resources/db/migration)
nachzulesen.

Eine neue Fassung kann neue Einstellungen mitbringen. Die Seite
[Konfiguration](/betrieb/konfiguration/) beschreibt immer den aktuellen Stand; sehen Sie dort vor
dem Update nach, ob etwas hinzugekommen ist, das Sie setzen müssen.

## Wenn …

### … die Anwendung nach dem Update nicht startet

Lesen Sie das Log, wie unter [Logs und Fehlersuche](/betrieb/logs-und-fehlersuche/) beschrieben.
Scheitert eine Migration, nennt das Log das Skript und den Fehler der Datenbank. Melden Sie den
Fehler mit dieser Meldung als
[Issue im Projekt](https://github.com/openClassware/elternsprechtag/issues).

Bis dahin kehren Sie zur vorigen Fassung zurück:

1. Halten Sie die Anwendung an: `docker compose stop app`.
2. Spielen Sie die Sicherung aus Schritt 1 zurück, wie unter
   [Wiederherstellen](/betrieb/datensicherung-und-wiederherstellung/#wiederherstellen) beschrieben.
   Das ist nötig, weil ein Teil der Migrationen schon gelaufen sein kann.
3. Holen Sie die gemerkte Fassung zurück und starten Sie sie:

   ```sh
   docker tag elternsprechtag:vorher ghcr.io/openclassware/elternsprechtag:latest
   docker compose up -d
   ```

Ziehen Sie danach kein neues Image, bis der Fehler behoben ist.

### … Sie die Datenbank auf eine neue Hauptversion von PostgreSQL heben wollen

Die Version steht in `compose.yaml` fest (`postgres:17`), damit ein Update der Anwendung sie nicht
mitzieht. Eine neue Hauptversion liest die Daten der alten nicht. Der Weg führt über eine Sicherung:
sichern, den Stack anhalten, das Volume `postgres_data` verwerfen, die Version in `compose.yaml`
erhöhen, die Datenbank neu starten und die Sicherung zurückspielen, wie unter
[Wiederherstellen](/betrieb/datensicherung-und-wiederherstellung/#wiederherstellen) beschrieben.
Löschen Sie die Sicherung erst, wenn die Anwendung mit der neuen Version läuft.
