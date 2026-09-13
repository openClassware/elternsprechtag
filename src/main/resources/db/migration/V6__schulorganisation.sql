-- V6: Die Stammdaten werden ein eigener Kontext (Issue #140, ADR 0003/0004).
--
-- Die vier Tabellen bleiben, wo sie sind — geändert hat sich, wer sie schreibt: statt vier
-- JPA-Entities jetzt vier Aggregate über Spring Data JDBC. Schemaseitig braucht das dreierlei.

-- 1. Optimistisches Sperren am Aggregat.
--
-- `default 1`, nicht 0 — dieselbe Rechnung wie in V5: Spring Data JDBC leitet bei einer primitiven
-- Versions-Eigenschaft "ist die Zeile neu?" aus `version == 0` ab. Eine Bestandszeile mit 0 gälte
-- beim ersten Speichern als neu, bekäme ein INSERT und liefe in eine Verletzung des
-- Primärschlüssels statt in ein UPDATE.
alter table lehrer      add column version bigint not null default 1;
alter table klassen     add column version bigint not null default 1;
alter table faecher     add column version bigint not null default 1;
alter table lehrauftrag add column version bigint not null default 1;

-- 2. Stilllegen statt löschen.
--
-- Stammdaten werden nicht gelöscht: Buchungen und Auswertungen früherer Sprechtage bezeugen sie.
-- Der Datensatz bleibt und nimmt an nichts Neuem mehr teil. Dass das überhaupt geht, hängt an
-- #138 — seit die Buchung ihr Ziel (Lehrkraft, Klasse, Fach) eingefroren hat, hängt keine
-- Auswertung mehr am aktuellen Stand der Stammdaten.
alter table lehrer      add column stillgelegt boolean not null default false;
alter table klassen     add column stillgelegt boolean not null default false;
alter table faecher     add column stillgelegt boolean not null default false;
alter table lehrauftrag add column stillgelegt boolean not null default false;

-- 3. Die Eindeutigkeit des Lehrauftrags.
--
-- Ein Lehrauftrag IST sein Tripel (Lehrkraft, Klasse, Fach); zweimal dasselbe Tripel wären zwei
-- Namen für einen Sachverhalt, und die Eltern-Ansicht böte die Lehrkraft doppelt an. Die Regel
-- steht hier und nicht im Aggregat: Sie spannt über alle Lehraufträge, und ein Aggregat, das sie
-- prüfen wollte, müsste dafür alle laden.
--
-- Die drei Spalten waren seit V1 nullable, weil Hibernate sie aus @ManyToOne ohne `optional=false`
-- erzeugt hat. Fachlich war ein Lehrauftrag ohne Lehrkraft nie etwas; jetzt steht es auch im
-- Schema — und ohne `not null` ließe der Unique-Index Dubletten mit NULL ohnehin durch.
alter table lehrauftrag alter column lehrer_id set not null;
alter table lehrauftrag alter column klasse_id set not null;
alter table lehrauftrag alter column fach_id   set not null;

alter table lehrauftrag
    add constraint uq_lehrauftrag_lehrkraft_klasse_fach unique (lehrer_id, klasse_id, fach_id);
