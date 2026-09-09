-- V3: Der Sprechtag trägt den Schulkontakt (Issue #102).
--
-- Ein einzelnes, mehrzeiliges Freitextfeld: „wen rufen die Eltern an" ist mehr als eine Nummer —
-- Sekretariatszeiten, eine Durchwahl mit Bedingung oder „kommen Sie vorbei" müssen hineinpassen.
-- Deshalb kein strukturiertes Trio aus Ansprechpartner, Telefon und E-Mail und deshalb mehr als
-- die 255 Zeichen der übrigen Textspalten.
--
-- Pflicht ist der Schulkontakt ab dem Entwurf, nicht erst beim Veröffentlichen: Ein Sprechtag ohne
-- ihn ist unvollständig, egal in welchem Status. `not null` allein genügt dafür nicht — es ließe
-- den leeren String durch —, deshalb steht der Check-Constraint daneben. Hibernate prüft
-- Check-Constraints nicht; die Nicht-Leerheit ist zusätzlich im SprechtagService abgesichert.
--
-- Kein Platzhalter-Backfill: erfundene Kontaktdaten landeten sonst in der Elternansicht, und das
-- ist schlimmer als gar keine. Produktiv gibt es keinen Altbestand (es existiert keine produktive
-- Version, und der Demo-Seed enthält keine Sprechtage). Enthält eine lokale Entwicklungsdatenbank
-- bereits Sprechtage, scheitert diese Migration — die Zeilen sind vorher von Hand zu füllen
-- (`update sprechtage set schulkontakt = '...' where schulkontakt is null;`) oder die Datenbank
-- ist neu aufzusetzen.
alter table sprechtage
    add column schulkontakt varchar(1000) not null;

alter table sprechtage
    add constraint chk_sprechtage_schulkontakt_nicht_leer
        check (btrim(schulkontakt) <> '');
