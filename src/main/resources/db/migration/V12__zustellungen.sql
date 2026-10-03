-- V12: Ausgang der Nachrichten an die Familien (Issue #110).
--
-- Je Buchung und Nachrichtenart höchstens eine Zeile; eine neue Nachricht derselben Art ersetzt
-- sie. Eigene Tabelle statt Spalten an buchungen: Der Versand schreibt asynchron zurück und soll die
-- Version des Termins nicht berühren.
--
-- ABGESCHICKT heißt „vom Mailserver angenommen", nicht „angekommen". Kein Fehlertext: Der steht im
-- Log, und hier soll kein Freitext stehen, den die Anonymisierung leeren müsste.
--
-- Bewusst KEIN Fremdschlüssel auf buchungen: Spring Data JDBC schreibt die Buchungen bei jedem
-- Speichern eines Termins neu (Delete-and-Insert, siehe TerminZeile). Ein Fremdschlüssel würde
-- dieses Speichern blockieren, einer mit ON DELETE CASCADE die Zustellungen bei jedem Storno am
-- selben Termin still mitlöschen. Verwaiste Zeilen entstehen nicht: Buchungen verschwinden nur mit
-- dem Rückweg zum Entwurf, und der gelingt nur, solange nie gebucht wurde.
--
-- Kein Altbestand: Buchungen von vor diesem Feature haben schlicht keine Zeile.
create table zustellungen (
    buchung_id uuid         not null,
    art        varchar(20)  not null check (art in ('BESTAETIGUNG', 'ERINNERUNG', 'ABSAGE', 'AUSFALL')),
    ergebnis   varchar(20)  not null check (ergebnis in ('ABGESCHICKT', 'FEHLGESCHLAGEN')),
    zeitpunkt  timestamp(6) not null,
    primary key (buchung_id, art)
);
