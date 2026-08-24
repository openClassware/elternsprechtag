-- V1: Ausgangsschema.
--
-- Diese Migration bildet den Stand ab, den bis hierher Hibernate zur Laufzeit erzeugt hat
-- (`ddl-auto=update`). Sie ist aus genau diesem generierten Schema abgeleitet und inhaltlich
-- identisch dazu; lediglich die Constraint-Namen sind sprechend statt generiert.
--
-- Ab hier ist das Schema versioniert: Hibernate validiert nur noch (`ddl-auto=validate`) und
-- ändert nichts mehr selbst. Jede weitere Schemaänderung ist ein neues Skript V2, V3, ... —
-- diese Datei wird nicht mehr angefasst, auch nicht in Kleinigkeiten (Flyway prüft ihre
-- Prüfsumme gegen die Historientabelle bereits migrierter Datenbanken).
--
-- Eine Baseline-Behandlung für Bestandsdatenbanken gibt es bewusst nicht: zum Zeitpunkt der
-- Einführung existiert keine produktive Instanz, jede Datenbank startet leer.

create table faecher (
    id         uuid         not null,
    name       varchar(255) not null unique,
    short_name varchar(255) not null unique,
    primary key (id)
);

create table klassen (
    id   uuid         not null,
    name varchar(255) not null unique,
    primary key (id)
);

create table lehrer (
    id       uuid         not null,
    vorname  varchar(255) not null,
    nachname varchar(255) not null,
    kuerzel  varchar(255) not null,
    primary key (id)
);

create table lehrauftrag (
    id        uuid not null,
    lehrer_id uuid,
    klasse_id uuid,
    fach_id   uuid,
    primary key (id)
);

create table sprechtage (
    id              uuid         not null,
    titel           varchar(255) not null,
    start_date      date         not null,
    start_time      time(0)      not null,
    end_time        time(0)      not null,
    slot_in_minutes integer      not null,
    -- Zugangs-Token für den anonymen Elternlink; es gibt bewusst keine Eltern-Accounts.
    access_token    varchar(255) not null,
    location        varchar(255),
    description     varchar(255),
    status          varchar(255) not null
        check (status in ('ENTWURF', 'VEROEFFENTLICHT', 'ABGESAGT', 'ABGESCHLOSSEN')),
    primary key (id)
);

-- Verknüpfungstabelle der @ManyToMany zwischen Sprechtag und Klasse; bewusst ohne
-- Primärschlüssel, genau wie im bisher generierten Schema.
create table sprechtage_klassen (
    sprechtag_id uuid not null,
    klasse_id    uuid not null
);

create table termin (
    id           uuid         not null,
    startzeit    timestamp(6) not null,
    endzeit      timestamp(6) not null,
    status       varchar(255) not null check (status in ('FREI', 'BELEGT')),
    -- @Version: optimistisches Sperren gegen zwei gleichzeitige Buchungen desselben Termins.
    version      bigint       not null,
    lehrer_id    uuid,
    sprechtag_id uuid         not null,
    primary key (id)
);

create table buchungen (
    id             uuid         not null,
    erstellt_am    timestamp(6) not null,
    status         varchar(255) not null check (status in ('ZUGESAGT', 'ABGESAGT')),
    schueler_name  varchar(255) not null,
    eltern_name    varchar(255) not null,
    -- Kontakt-E-Mail der Eltern, denormalisiert an der Buchung (ADR 0001, ADR 0002).
    eltern_email   varchar(255) not null,
    notiz          varchar(255),
    lehrauftrag_id uuid         not null,
    -- Kein Unique-Constraint auf termin_id: ein stornierter Termin darf erneut gebucht werden,
    -- über die Zeit gibt es also mehrere Buchungen je Termin (höchstens eine davon aktiv).
    termin_id      uuid         not null,
    primary key (id)
);

alter table lehrauftrag
    add constraint fk_lehrauftrag_lehrer foreign key (lehrer_id) references lehrer;
alter table lehrauftrag
    add constraint fk_lehrauftrag_klasse foreign key (klasse_id) references klassen;
alter table lehrauftrag
    add constraint fk_lehrauftrag_fach foreign key (fach_id) references faecher;

alter table sprechtage_klassen
    add constraint fk_sprechtage_klassen_sprechtag foreign key (sprechtag_id) references sprechtage;
alter table sprechtage_klassen
    add constraint fk_sprechtage_klassen_klasse foreign key (klasse_id) references klassen;

alter table termin
    add constraint fk_termin_lehrer foreign key (lehrer_id) references lehrer;
alter table termin
    add constraint fk_termin_sprechtag foreign key (sprechtag_id) references sprechtage;

alter table buchungen
    add constraint fk_buchung_lehrauftrag foreign key (lehrauftrag_id) references lehrauftrag;
alter table buchungen
    add constraint fk_buchung_termin foreign key (termin_id) references termin;
