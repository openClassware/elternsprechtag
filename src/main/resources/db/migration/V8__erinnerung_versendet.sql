-- V8: Zeitstempel an der Buchung gegen Doppelversand der Erinnerung (Issue #107).
--
-- NULL heißt „noch nicht erinnert" — der Zustand jeder aktiven Buchung, bis der tägliche
-- Erinnerungs-Scheduler sie erreicht. Gesetzt wird er genau einmal, am Tag des fälligen Vorlaufs;
-- „verfallen statt nachholen" sorgt dafür, dass ein verpasster Lauf keine verspätete Erinnerung
-- nachliefert (ErinnerungsVorlauf.istFaelligAm).
alter table buchungen
    add column erinnerung_versendet_am timestamp;
