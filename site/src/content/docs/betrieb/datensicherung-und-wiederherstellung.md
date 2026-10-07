---
title: Datensicherung und Wiederherstellung
description: Die Datenbank mit pg_dump sichern, regelmäßig per cron, und eine Sicherung zurückspielen — samt dem, was die Anonymisierung in Sicherungen nicht erreicht.
sidebar:
  order: 5
---
Alles, was die Anwendung weiß, steht in ihrer **PostgreSQL**-Datenbank: Stammdaten, Sprechtage,
Termine und Buchungen. Die Anwendung selbst hält keine Daten; ihr Container lässt sich jederzeit
verwerfen und neu starten. Gesichert wird also die Datenbank, und zwar mit `pg_dump`.

Die Befehle unten gehen vom Stack aus der [Installation](/betrieb/installation/) aus und laufen im
Verzeichnis `/opt/elternsprechtag`.

## Sichern

```sh
docker compose exec -T database pg_dump -U elternsprechtag -d elternsprechtag -Fc \
  > sicherung-$(date +%F).dump
```

`-Fc` schreibt das komprimierte Format von PostgreSQL, das `pg_restore` zurückspielt. Die
Sicherung entsteht im laufenden Betrieb und ist in sich stimmig; die Anwendung muss dafür nicht
anhalten.

Sichern Sie außerdem die Datei `.env`. Sie enthält die Zugangsdaten und die Einstellungen Ihrer
Schule und liegt nicht in der Datenbank.

### Regelmäßig sichern

Eine Sicherung jede Nacht genügt. Ein Eintrag in der crontab des Benutzers, der den Stack betreibt
(`crontab -e`), sichert täglich um 2 Uhr und löscht Sicherungen, die älter als 14 Tage sind:

```sh
0 2 * * * cd /opt/elternsprechtag && mkdir -p sicherungen && docker compose exec -T database pg_dump -U elternsprechtag -d elternsprechtag -Fc > sicherungen/sicherung-$(date +\%F).dump && find sicherungen -name 'sicherung-*.dump' -mtime +14 -delete
```

In der crontab muss jedes `%` als `\%` stehen. Kopieren Sie die Sicherungen zusätzlich auf einen
anderen Rechner. Eine Sicherung, die nur auf demselben Server liegt, geht mit ihm verloren.

## Wiederherstellen

Eine Sicherung ersetzt den ganzen Inhalt der Datenbank. Alles, was nach der Sicherung gebucht,
angelegt oder geändert wurde, ist danach weg.

1. Halten Sie die Anwendung an, damit sie während des Zurückspielens nichts schreibt:

   ```sh
   docker compose stop app
   ```

2. Spielen Sie die Sicherung zurück:

   ```sh
   docker compose exec -T database pg_restore -U elternsprechtag -d elternsprechtag \
     --clean --if-exists --no-owner --exit-on-error < sicherung-2026-10-07.dump
   ```

   `--clean --if-exists` entfernt vorher, was in der Datenbank steht. So landet die Sicherung in
   einer leeren Datenbank, auch wenn dort noch Daten liegen.

3. Starten Sie die Anwendung wieder:

   ```sh
   docker compose start app
   docker compose logs -f app
   ```

   Steht im Log `Started ElternsprechtagApplication`, ist sie bereit.

Zum Wiederherstellen auf einem **neuen Server** setzen Sie zuerst die
[Installation](/betrieb/installation/) mit der gesicherten `.env` auf. Starten Sie dann nur die
Datenbank, spielen Sie die Sicherung wie in Schritt 2 zurück und starten Sie danach alles:

```sh
docker compose up -d --wait database
docker compose exec -T database pg_restore -U elternsprechtag -d elternsprechtag \
  --clean --if-exists --no-owner --exit-on-error < sicherung-2026-10-07.dump
docker compose up -d
```

### Sicherung und Fassung der Anwendung

Die Sicherung enthält auch, wie weit Flyway das Schema migriert hat. Spielen Sie sie in dieselbe
oder eine **neuere** Fassung der Anwendung zurück: Eine neuere bringt das Schema beim Start auf ihren
Stand, siehe [Updates](/betrieb/updates/). Eine ältere Fassung als die, aus der die Sicherung stammt,
kennt die neueren Tabellen nicht und ist nicht vorgesehen.

## Sicherungen und Datenschutz

**Das Entfernen der personenbezogenen Angaben erfasst keine Sicherungen.** Nach Ablauf der
Aufbewahrungsfrist und nach einem Löschverlangen ersetzt die Anwendung Namen und E-Mail-Adressen in
der Datenbank durch Platzhalter. Eine Sicherung, die vorher entstand, enthält die Angaben weiter,
bis Sie sie löschen.

- **Bewahren Sie Sicherungen nicht länger auf als nötig.** Wie lange, legen Sie im Löschkonzept der
  Schule fest. Die 14 Tage im Beispiel oben sind ein Vorschlag.
- **Nach dem Wiederherstellen** kann eine ältere Sicherung Angaben zurückbringen, die inzwischen
  entfernt waren. Ist die Aufbewahrungsfrist abgelaufen, entfernt der nächste nächtliche Lauf sie
  wieder. Nach einem Löschverlangen tut er das nicht: Wiederholen Sie es, wie unter
  [Einem Löschverlangen nachkommen](/anwendungsfaelle/einem-loeschverlangen-nachkommen/) beschrieben.
- **Schützen Sie die Sicherungen** wie die Datenbank selbst. Sie enthalten Namen von Kindern und
  Eltern, E-Mail-Adressen und die Notizen der Eltern.

Mehr dazu unter
[Wann personenbezogene Angaben entfernt werden](/datenschutz/wann-personenbezogene-angaben-entfernt-werden/).
