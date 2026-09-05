-- V2: Die Notiz einer Buchung darf 500 Zeichen tragen.
--
-- Die Buchungsoberfläche begrenzt jede Notiz auf 500 Zeichen (Issue #88), die Spalte hielt
-- aber nur 255 — eine längere Notiz wäre erst beim Speichern am INSERT gescheitert. Die
-- Erweiterung ist verlustfrei: bestehende Werte passen unverändert in die größere Spalte.
alter table buchungen
    alter column notiz type varchar(500);
