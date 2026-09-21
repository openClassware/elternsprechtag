-- V7: Erinnerungszeitpunkt als Auswahl am Sprechtag (Issue #106, ADR 0006).
--
-- Feste Optionen statt einer freien Datumsangabe: Eine freie Angabe erzeugt einen Fehlerfall, den
-- feste Optionen nicht haben können — einen Erinnerungszeitpunkt nach dem Sprechtag. Die Uhrzeit
-- des Versands steckt in der Option, nicht in einer zweiten Spalte.
--
-- Nicht Teil der Zeitstruktur: Das Feld verschiebt keinen Termin und macht keine Buchung
-- ungültig, bleibt deshalb auch für bereits veröffentlichte Sprechtage änderbar.
--
-- `default 'KEINE'`, damit jeder bestehende Sprechtag denselben Stand bekommt wie vor dieser
-- Migration — es gab bislang keine Erinnerung.
alter table sprechtage
    add column erinnerung_vorlauf varchar(20) not null default 'KEINE'
        check (erinnerung_vorlauf in ('KEINE', 'EIN_TAG', 'ZWEI_TAGE', 'DREI_TAGE'));
