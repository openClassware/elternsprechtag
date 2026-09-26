-- V9: Anmeldefrist am Sprechtag (Issue #122).
--
-- Gespeichert wird der Abstand „N Tage vor dem Sprechtag", nicht das Datum des Anmeldeschlusses:
-- Relativ ist die Frist beim Duplizieren für jedes neue Datum richtig. 0 heißt „am Tag selbst",
-- mehr als 28 Tage fängt den Tippfehler ab.
--
-- Nicht Teil der Zeitstruktur: Die Frist erzeugt keinen Termin und macht keine Buchung ungültig,
-- bleibt deshalb auch für bereits veröffentlichte Sprechtage änderbar.
--
-- `default 1`, damit jeder bestehende Sprechtag den Vortag bekommt — denselben Wert, den jeder neue
-- Sprechtag vorgeschlagen bekommt.
alter table sprechtage
    add column anmeldefrist_tage integer not null default 1
        constraint sprechtage_anmeldefrist_tage_check check (anmeldefrist_tage between 0 and 28);
