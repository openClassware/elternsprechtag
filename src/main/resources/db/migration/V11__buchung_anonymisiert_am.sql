-- V11: Zeitpunkt der Anonymisierung an der einzelnen Buchung (Issue #129).
--
-- NULL heißt „trägt noch die Angaben der Familie". Gesetzt wird er vom nächtlichen Lauf nach Ablauf
-- der Aufbewahrungsfrist und — vorgezogen — vom Löschverlangen einer Familie: beim Storno mit
-- entfernten Angaben, beim Umbuchen für die alte Buchung und per Einzelaktion in der Auswertung.
alter table buchungen
    add column anonymisiert_am timestamp;

-- Altbestand: Was der Lauf aus #126 bereits anonymisiert hat, bekommt dessen Zeitpunkt. Sonst böte
-- die Auswertung an längst pseudonymen Zeilen noch „Angaben entfernen" an.
update buchungen b
   set anonymisiert_am = s.anonymisiert_am
  from termin t
  join sprechtage s on s.id = t.sprechtag_id
 where b.termin_id = t.id
   and s.anonymisiert_am is not null;
