-- V10: Zeitpunkt der Anonymisierung am Sprechtag (Issue #126).
--
-- NULL heißt „noch nicht anonymisiert" — der Zustand jedes Sprechtags, bis die Aufbewahrungsfrist
-- ab seiner Endzeit verstrichen ist und der tägliche Anonymisierungs-Lauf ihn erreicht. Gesetzt
-- wird er erst, nachdem alle Buchungen des Sprechtags anonymisiert und gespeichert sind; daran
-- erkennt der Lauf erledigte Sprechtage, und die Auswertung zeigt das Datum an (#127).
alter table sprechtage
    add column anonymisiert_am timestamp;
