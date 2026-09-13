-- V4: Termin und Buchung werden ein Aggregat (Issue #138, ADR 0003/0004).
--
-- Drei Änderungen, alle aus demselben Entwurf:
--
-- 1. `termin.status` war eine denormalisierte Zweitschrift der Frage „gibt es eine aktive
--    Buchung?" — zwei Wahrheiten für einen Fakt, und die Ursache dafür, dass ein Storno den Slot
--    nur dann freigab, wenn jemand daran dachte, den Status mitzuziehen. Der Zustand wird künftig
--    abgeleitet. Erhalten bleibt eine Spalte für das, was NICHT ableitbar ist: `verfuegbarkeit`,
--    die Absicht des Organizers, einen Slot anzubieten oder entfallen zu lassen (ABDECKUNG.md
--    Z. 230). Bestandszeilen sind angeboten — auch die bisher als BELEGT geführten, denn belegt
--    ist kein Gegenteil von angeboten.
--
-- 2. Der Buchungsstatus ABGESAGT heißt STORNIERT. Er kollidierte mit
--    `sprechtage.status = 'ABGESAGT'` bei anderer Bedeutung: Eine Buchung wird storniert, ein
--    Sprechtag wird abgesagt.
--
-- 3. Die Buchung friert ihr Ziel ein — Lehrkraft, Klasse und Fach als Stand zum Buchungszeitpunkt
--    statt als Verweis. Grund ist der kommende Stammdaten-Import: Er lässt Lehraufträge
--    verschwinden, während Buchungen daran hängen, und die Auswertung eines vergangenen
--    Sprechtags muss davon unberührt bleiben. `lehrauftrag_id` bleibt als Herkunftsspur stehen.
--
-- Der Backfill in Schritt 3 ist der einzige Moment, in dem diese Daten noch aus dem Lehrauftrag
-- zu holen sind; danach ist die Verbindung fachlich gekappt.

-- 1. termin.status -> termin.verfuegbarkeit -------------------------------------------------
-- Der Check stammt aus der Spaltendefinition in V1 und trägt deshalb den von PostgreSQL
-- vergebenen Namen. Er muss vor dem Umbenennen weg: FREI/BELEGT sind keine Verfügbarkeiten.
alter table termin
    drop constraint termin_status_check;

alter table termin
    rename column status to verfuegbarkeit;

update termin
   set verfuegbarkeit = 'VERFUEGBAR';

alter table termin
    add constraint chk_termin_verfuegbarkeit
        check (verfuegbarkeit in ('VERFUEGBAR', 'ENTFAELLT'));

-- Ein Termin ohne Lehrkraft hat es nie gegeben (die Materialisierung setzt sie immer), das Schema
-- ließ ihn aber zu. Das Aggregat verlangt sie; ab hier tut es die Spalte auch.
alter table termin
    alter column lehrer_id set not null;

-- 2. buchungen.status: ABGESAGT -> STORNIERT ------------------------------------------------
alter table buchungen
    drop constraint buchungen_status_check;

update buchungen
   set status = 'STORNIERT'
 where status = 'ABGESAGT';

alter table buchungen
    add constraint chk_buchungen_status
        check (status in ('ZUGESAGT', 'STORNIERT'));

-- 3. Eingefrorenes Buchungsziel --------------------------------------------------------------
-- Erst nullable anlegen, dann füllen, dann Pflicht setzen: Ohne diesen Dreischritt ließe sich die
-- Spalte an einer Tabelle mit Bestandszeilen gar nicht als `not null` hinzufügen.
alter table buchungen
    add column lehrkraft_id      uuid,
    add column lehrkraft_name    varchar(255),
    add column lehrkraft_kuerzel varchar(255),
    add column klasse_name       varchar(255),
    add column fach_name         varchar(255);

update buchungen b
   set lehrkraft_id      = l.id,
       lehrkraft_name    = l.vorname || ' ' || l.nachname,
       lehrkraft_kuerzel = l.kuerzel,
       klasse_name       = k.name,
       fach_name         = f.name
  from lehrauftrag la
  join lehrer  l on l.id = la.lehrer_id
  join klassen k on k.id = la.klasse_id
  join faecher f on f.id = la.fach_id
 where la.id = b.lehrauftrag_id;

alter table buchungen
    alter column lehrkraft_id      set not null,
    alter column lehrkraft_name    set not null,
    alter column lehrkraft_kuerzel set not null,
    alter column klasse_name       set not null,
    alter column fach_name         set not null;

-- Und damit fällt der Fremdschlüssel auf den Lehrauftrag: Er würde genau das verhindern, worum es
-- beim Einfrieren geht. Der Import soll Lehraufträge löschen können, während alte Buchungen daran
-- hängen — mit der Referenz scheiterte das Löschen, und die Auswertung eines vergangenen Sprechtags
-- hinge weiter an Daten, die dem anderen Kontext gehören. `lehrauftrag_id` bleibt als
-- Herkunftsspur stehen, jetzt ohne Zusicherung, dass die Zeile noch existiert.
--
-- `termin.lehrer_id` behält seinen Fremdschlüssel: Ein Termin ohne Lehrkraft ist keiner, und die
-- Frage, was beim Löschen einer Lehrkraft mit ihren Terminen geschieht, gehört zur Scheibe, die
-- den Stammdaten-Kontext schneidet (#140).
alter table buchungen
    drop constraint fk_buchung_lehrauftrag;
