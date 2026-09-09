-- V3: Der Sprechtag trägt den Schulkontakt (Issue #102).
--
-- Ein einzelnes, mehrzeiliges Freitextfeld: „wen rufen die Eltern an" ist mehr als eine Nummer —
-- Sekretariatszeiten, eine Durchwahl mit Bedingung oder „kommen Sie vorbei" müssen hineinpassen.
-- Deshalb kein strukturiertes Trio aus Ansprechpartner, Telefon und E-Mail und deshalb mehr als
-- die 255 Zeichen der übrigen Textspalten.
--
-- Die Spalte ist NULL-fähig, denn Pflicht ist der Schulkontakt erst beim Veröffentlichen: Ein
-- Entwurf darf ohne ihn gespeichert werden. Ein glattes NOT NULL wäre daher falsch,
-- NOT NULL DEFAULT '' wirkungslos. Die Regel steht stattdessen als bedingter Check-Constraint
-- daneben — „Status ist nicht VEROEFFENTLICHT oder der getrimmte Schulkontakt ist nicht leer".
-- Hibernate prüft Check-Constraints nicht; die Entity trägt das Feld folgerichtig ohne
-- `nullable = false`, sonst scheiterte `ddl-auto=validate` beim Start.
--
-- Kein Platzhalter-Backfill: Bestehende veröffentlichte Sprechtage ohne Schulkontakt gibt es
-- produktiv nicht (keine produktive Instanz, der Demo-Seed enthält keine Sprechtage). Lokal ist
-- eine solche Zeile von Hand zu füllen — erfundene Kontaktdaten landen sonst in der Elternansicht,
-- und das ist schlimmer als gar keine.
alter table sprechtage
    add column schulkontakt varchar(1000);

alter table sprechtage
    add constraint chk_sprechtage_schulkontakt_veroeffentlicht
        check (status <> 'VEROEFFENTLICHT' or btrim(coalesce(schulkontakt, '')) <> '');
