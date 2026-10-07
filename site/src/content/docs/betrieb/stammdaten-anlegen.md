---
title: Stammdaten anlegen
description: Klassen, Lehrkräfte, Fächer und Lehraufträge per SQL in die Datenbank schreiben — mit einem Beispiel zum Anpassen.
sidebar:
  order: 7
---
Bevor der Organisator den ersten Sprechtag anlegt, braucht die Anwendung die **Stammdaten** Ihrer
Schule: welche Klassen es gibt, welche Lehrkräfte, welche Fächer und wer welches Fach in welcher
Klasse unterrichtet. Eine Oberfläche dafür gibt es nicht. Die Schul-IT schreibt die Stammdaten per
SQL in die Datenbank, vor dem ersten Sprechtag und danach zu jedem Schuljahr.

## Was angelegt wird

| Tabelle | Inhalt | Spalten |
|---|---|---|
| `klassen` | Eine Zeile je Klasse | `name`, etwa `5a` — eindeutig |
| `lehrer` | Eine Zeile je Lehrkraft | `vorname`, `nachname`, `kuerzel` |
| `faecher` | Eine Zeile je Fach | `name` und `short_name`, etwa `Mathematik` und `M` — beide eindeutig |
| `lehrauftrag` | Wer unterrichtet welches Fach in welcher Klasse | `lehrer_id`, `klasse_id`, `fach_id` |

Jede Zeile hat eine `id` vom Typ `uuid`, die Sie beim Anlegen mit `gen_random_uuid()` erzeugen.

**Der Lehrauftrag ist das, worauf es ankommt.** Aus ihm entstehen die Termine: Veröffentlicht der
Organisator einen Sprechtag, bekommt jede Lehrkraft, die einen Lehrauftrag in einer der gewählten
Klassen hat, ihre Gesprächstermine. Eltern wählen beim Buchen die Klasse ihres Kindes und sehen
genau die Lehrkräfte mit einem Lehrauftrag dort, jeweils mit dem Fach. Eine Lehrkraft ohne
Lehrauftrag erscheint nirgends.

## Mit der Datenbank verbinden

Im Stack aus der [Installation](/betrieb/installation/) öffnen Sie im Verzeichnis
`/opt/elternsprechtag` eine SQL-Sitzung so:

```sh
docker compose exec database psql -U elternsprechtag -d elternsprechtag
```

Ein Skript aus einer Datei spielen Sie so ein:

```sh
docker compose exec -T database psql -U elternsprechtag -d elternsprechtag -v ON_ERROR_STOP=1 < stammdaten.sql
```

Die Anwendung muss dafür nicht angehalten werden. Sie liest die Stammdaten bei jedem Zugriff
neu; der Organisator sieht neue Klassen beim nächsten Öffnen des Formulars.

## Beispiel

Das Skript legt zwei Klassen, drei Lehrkräfte, drei Fächer und ihre Lehraufträge an. Die
Lehraufträge finden Klasse, Lehrkraft und Fach über Name und Kürzel, so brauchen Sie keine `id`
abzuschreiben. Passen Sie die Werte an Ihre Schule an.

```sql
BEGIN;

INSERT INTO klassen (id, name) VALUES
  (gen_random_uuid(), '5a'),
  (gen_random_uuid(), '5b');

INSERT INTO faecher (id, name, short_name) VALUES
  (gen_random_uuid(), 'Deutsch',    'D'),
  (gen_random_uuid(), 'Mathematik', 'M'),
  (gen_random_uuid(), 'Englisch',   'E');

INSERT INTO lehrer (id, vorname, nachname, kuerzel) VALUES
  (gen_random_uuid(), 'Anna',   'Krause', 'KRA'),
  (gen_random_uuid(), 'Jonas',  'Becker', 'BEC'),
  (gen_random_uuid(), 'Sabine', 'Bauer',  'BAU');

-- Wer unterrichtet was wo: Klasse, Kürzel der Lehrkraft, Kurzname des Fachs
INSERT INTO lehrauftrag (id, lehrer_id, klasse_id, fach_id)
SELECT gen_random_uuid(), l.id, k.id, f.id
  FROM (VALUES
          ('5a', 'KRA', 'D'),
          ('5a', 'BEC', 'M'),
          ('5a', 'BAU', 'E'),
          ('5b', 'KRA', 'D'),
          ('5b', 'BAU', 'M'),
          ('5b', 'BAU', 'E')
       ) AS a (klasse, kuerzel, fach)
  JOIN klassen k ON k.name = a.klasse
  JOIN lehrer  l ON l.kuerzel = a.kuerzel AND NOT l.stillgelegt
  JOIN faecher f ON f.short_name = a.fach;

COMMIT;
```

Prüfen Sie danach, ob jeder Lehrauftrag angekommen ist. Eine Zeile mit Tippfehler in Klasse,
Kürzel oder Fach findet keinen Partner und fehlt dann stillschweigend:

```sql
SELECT k.name AS klasse, l.kuerzel, f.short_name AS fach
  FROM lehrauftrag la
  JOIN klassen k ON k.id = la.klasse_id
  JOIN lehrer  l ON l.id = la.lehrer_id
  JOIN faecher f ON f.id = la.fach_id
 WHERE NOT la.stillgelegt
 ORDER BY k.name, l.kuerzel;
```

## Regeln

- **Ein Kürzel je Lehrkraft.** Die Datenbank erzwingt es nicht, das Beispiel verlässt sich aber
  darauf. Zwei aktive Lehrkräfte mit demselben Kürzel bekämen beide jeden Lehrauftrag des Kürzels.
- **Einen Lehrauftrag gibt es je Lehrkraft, Klasse und Fach nur einmal.** Ein zweiter, gleicher
  bricht mit einer Verletzung von `uq_lehrauftrag_lehrkraft_klasse_fach` ab.
- **Stammdaten werden stillgelegt, nicht gelöscht.** Jede der vier Tabellen hat die Spalte
  `stillgelegt`. Eine stillgelegte Zeile nimmt an keinem neuen Sprechtag mehr teil, frühere
  Sprechtage und ihre Buchungen behalten sie. Löschen ist nicht vorgesehen; bei einer Lehrkraft
  oder Klasse, die schon an einem Sprechtag teilgenommen hat, scheitert ein `DELETE` ohnehin.
- **Die Termine entstehen beim Veröffentlichen.** Eine Lehrkraft, die ihren ersten Lehrauftrag in
  einer Klasse erst danach bekommt, hat an diesem Sprechtag keine Termine. Pflegen Sie die
  Stammdaten deshalb, bevor der Organisator veröffentlicht.

## Wenn …

### … ein neues Schuljahr beginnt

Klassennamen wiederholen sich, die Besetzung nicht. Legen Sie die Lehraufträge des alten Jahres
still und die neuen an:

```sql
UPDATE lehrauftrag SET stillgelegt = true WHERE NOT stillgelegt;
```

Danach legen Sie die neuen Lehraufträge an wie im Beispiel, ohne die Klassen, Lehrkräfte und
Fächer, die es schon gibt. Ein Lehrauftrag, der gleich bleibt, wird dabei neu angelegt; das ist so
gewollt, die Regel zur Eindeutigkeit gilt nur unter den nicht stillgelegten.

### … eine Lehrkraft die Schule verlässt

```sql
UPDATE lehrer SET stillgelegt = true WHERE kuerzel = 'BEC';
```

Sie erscheint in keinem neuen Sprechtag mehr, auch nicht über ihre Lehraufträge.

### … eine Klasse oder ein Fach dauerhaft wegfällt

```sql
UPDATE klassen SET stillgelegt = true WHERE name = '5b';
UPDATE faecher SET stillgelegt = true WHERE short_name = 'E';
```

Der Organisator kann die Klasse beim Anlegen eines Sprechtags nicht mehr ankreuzen; das Fach
erscheint in keinem neuen Sprechtag.

Legen Sie nur still, was es **endgültig** nicht mehr gibt. Klassennamen und Fachnamen sind über
alle Zeilen eindeutig, auch über die stillgelegten: Eine Klasse `5b` lässt sich danach nicht neu
anlegen, und einen Weg zurück aus dem Stilllegen gibt es nicht. Zum Schuljahreswechsel bleiben die
Klassen deshalb, wie sie sind; nur die Lehraufträge wechseln.

### … die Meldung kommt, dass keine Termine erzeugt wurden

Beim Veröffentlichen sieht der Organisator:
*Der Sprechtag ist veröffentlicht, aber es wurden keine Termine erzeugt — entweder hat keine der
gewählten Klassen einen Lehrauftrag, oder das Zeitfenster ist kürzer als eine Slot-Dauer.*
Prüfen Sie mit der Abfrage oben, ob die Klassen Lehraufträge haben, und legen Sie die fehlenden an.
Danach nimmt der Organisator das Veröffentlichen zurück und veröffentlicht erneut, siehe
[Ein Veröffentlichen zurücknehmen](/anwendungsfaelle/ein-veroeffentlichen-zuruecknehmen/).
