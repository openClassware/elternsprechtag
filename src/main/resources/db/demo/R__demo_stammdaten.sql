-- Demo-Daten: Stammdaten (Fächer, Klassen, Lehrer, Lehraufträge) und dahinter fünf Sprechtage mit
-- Terminen und Buchungen, je einer in jedem Zustand (Issue #123).
--
-- Diese Migration liegt bewusst NICHT unter `db/migration`, sondern in einem eigenen Verzeichnis,
-- das nur das 'demo'-Profil in `spring.flyway.locations` aufnimmt (application-demo.properties).
-- Eine Schulinstanz sieht dieses Skript damit gar nicht erst und bekommt keine Demo-Daten.
--
-- Sie ist eine WIEDERHOLBARE Migration (`R__`) und keine versionierte: Wiederholbare laufen immer
-- nach allen versionierten Skripten, also nach der jeweils neuesten Schemaversion. Eine feste
-- Versionsnummer müsste dagegen entweder in die Hauptkette eingereiht werden (wo sie mit dem
-- nächsten V-Skript kollidiert) oder hinter ihr liegen (dann liefen spätere Hauptmigrationen
-- "out of order" und Flyway bräche ab).
--
-- Feste UUIDs + ON CONFLICT DO NOTHING => idempotent. Wiederholbare Migrationen laufen erneut,
-- sobald sich ihre Prüfsumme ändert, und der Demo-Deploy setzt die Datenbank ohnehin vor jedem
-- Start zurück — beides darf hier nichts kaputtmachen.
--
-- Das Konfliktziel ist bewusst `(id)` und nicht das weitere `ON CONFLICT DO NOTHING`. Neben der id
-- sind auch `faecher.name`, `faecher.short_name` und `klassen.name` eindeutig; eine fremde Zeile,
-- die nur auf so einem Namen kollidiert, lässt den Seed hier laut scheitern. Das ist gewollt:
-- Ohne Ziel würde die Zeile still übersprungen, die Lehraufträge zeigten dann auf eine nie
-- eingefügte Klasse und der Start bräche stattdessen an einer Fremdschlüsselverletzung ab — bei
-- gleicher Wirkung, aber mit einer Meldung, die von der Ursache wegführt.
--
-- Der Seed setzt also eine Datenbank voraus, die diese Stammdaten entweder gar nicht oder exakt
-- so enthält. Genau das stellt der Demo-Deploy her: Er leert die Datenbank vor jedem Start.

-- ---------------------------------------------------------------------------
-- Fächer
-- ---------------------------------------------------------------------------
INSERT INTO faecher (id, name, short_name) VALUES
  ('00000000-0000-0000-0001-000000000001', 'Deutsch',     'D'),
  ('00000000-0000-0000-0001-000000000002', 'Mathematik',  'M'),
  ('00000000-0000-0000-0001-000000000003', 'Englisch',    'E'),
  ('00000000-0000-0000-0001-000000000004', 'Physik',      'Ph'),
  ('00000000-0000-0000-0001-000000000005', 'Biologie',    'Bio'),
  ('00000000-0000-0000-0001-000000000006', 'Chemie',      'Ch'),
  ('00000000-0000-0000-0001-000000000007', 'Geschichte',  'Ge'),
  ('00000000-0000-0000-0001-000000000008', 'Sport',       'Sp')
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Klassen
-- ---------------------------------------------------------------------------
INSERT INTO klassen (id, name) VALUES
  ('00000000-0000-0000-0002-000000000001', '5a'),
  ('00000000-0000-0000-0002-000000000002', '5b'),
  ('00000000-0000-0000-0002-000000000003', '6a'),
  ('00000000-0000-0000-0002-000000000004', '7a'),
  ('00000000-0000-0000-0002-000000000005', '7b'),
  ('00000000-0000-0000-0002-000000000006', '8a')
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Lehrer
-- ---------------------------------------------------------------------------
INSERT INTO lehrer (id, vorname, nachname, kuerzel) VALUES
  ('00000000-0000-0000-0003-000000000001', 'Anna',    'Krause',   'KRA'),
  ('00000000-0000-0000-0003-000000000002', 'Michael', 'Kern',     'KRN'),
  ('00000000-0000-0000-0003-000000000003', 'Sabine',  'Bauer',    'BAU'),
  ('00000000-0000-0000-0003-000000000004', 'Thomas',  'Wagner',   'WAG'),
  ('00000000-0000-0000-0003-000000000005', 'Julia',   'Schmidt',  'SMT'),
  ('00000000-0000-0000-0003-000000000006', 'Peter',   'Hoffmann', 'HOF'),
  ('00000000-0000-0000-0003-000000000007', 'Laura',   'Fischer',  'FIS'),
  ('00000000-0000-0000-0003-000000000008', 'Markus',  'Weber',    'WEB'),
  ('00000000-0000-0000-0003-000000000009', 'Nicole',  'Richter',  'RIC'),
  ('00000000-0000-0000-0003-000000000010', 'Stefan',  'Koch',     'KOC')
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Lehraufträge (Lehrer x Klasse x Fach)
-- Jede Klasse hat 5 Fächer mit zugeordneter Lehrkraft.
-- ---------------------------------------------------------------------------
INSERT INTO lehrauftrag (id, lehrer_id, klasse_id, fach_id) VALUES
  -- Klasse 5a
  ('00000000-0000-0000-0004-000000000001', '00000000-0000-0000-0003-000000000001', '00000000-0000-0000-0002-000000000001', '00000000-0000-0000-0001-000000000001'), -- D  Krause
  ('00000000-0000-0000-0004-000000000002', '00000000-0000-0000-0003-000000000002', '00000000-0000-0000-0002-000000000001', '00000000-0000-0000-0001-000000000002'), -- M  Kern
  ('00000000-0000-0000-0004-000000000003', '00000000-0000-0000-0003-000000000003', '00000000-0000-0000-0002-000000000001', '00000000-0000-0000-0001-000000000003'), -- E  Bauer
  ('00000000-0000-0000-0004-000000000004', '00000000-0000-0000-0003-000000000006', '00000000-0000-0000-0002-000000000001', '00000000-0000-0000-0001-000000000005'), -- Bio Hoffmann
  ('00000000-0000-0000-0004-000000000005', '00000000-0000-0000-0003-000000000008', '00000000-0000-0000-0002-000000000001', '00000000-0000-0000-0001-000000000008'), -- Sp  Weber
  -- Klasse 5b
  ('00000000-0000-0000-0004-000000000006', '00000000-0000-0000-0003-000000000005', '00000000-0000-0000-0002-000000000002', '00000000-0000-0000-0001-000000000001'), -- D  Schmidt
  ('00000000-0000-0000-0004-000000000007', '00000000-0000-0000-0003-000000000008', '00000000-0000-0000-0002-000000000002', '00000000-0000-0000-0001-000000000002'), -- M  Weber
  ('00000000-0000-0000-0004-000000000008', '00000000-0000-0000-0003-000000000007', '00000000-0000-0000-0002-000000000002', '00000000-0000-0000-0001-000000000003'), -- E  Fischer
  ('00000000-0000-0000-0004-000000000009', '00000000-0000-0000-0003-000000000006', '00000000-0000-0000-0002-000000000002', '00000000-0000-0000-0001-000000000005'), -- Bio Hoffmann
  ('00000000-0000-0000-0004-000000000010', '00000000-0000-0000-0003-000000000002', '00000000-0000-0000-0002-000000000002', '00000000-0000-0000-0001-000000000008'), -- Sp  Kern
  -- Klasse 6a
  ('00000000-0000-0000-0004-000000000011', '00000000-0000-0000-0003-000000000001', '00000000-0000-0000-0002-000000000003', '00000000-0000-0000-0001-000000000001'), -- D  Krause
  ('00000000-0000-0000-0004-000000000012', '00000000-0000-0000-0003-000000000002', '00000000-0000-0000-0002-000000000003', '00000000-0000-0000-0001-000000000002'), -- M  Kern
  ('00000000-0000-0000-0004-000000000013', '00000000-0000-0000-0003-000000000003', '00000000-0000-0000-0002-000000000003', '00000000-0000-0000-0001-000000000003'), -- E  Bauer
  ('00000000-0000-0000-0004-000000000014', '00000000-0000-0000-0003-000000000009', '00000000-0000-0000-0002-000000000003', '00000000-0000-0000-0001-000000000007'), -- Ge  Richter
  ('00000000-0000-0000-0004-000000000015', '00000000-0000-0000-0003-000000000008', '00000000-0000-0000-0002-000000000003', '00000000-0000-0000-0001-000000000008'), -- Sp  Weber
  -- Klasse 7a
  ('00000000-0000-0000-0004-000000000016', '00000000-0000-0000-0003-000000000005', '00000000-0000-0000-0002-000000000004', '00000000-0000-0000-0001-000000000001'), -- D  Schmidt
  ('00000000-0000-0000-0004-000000000017', '00000000-0000-0000-0003-000000000008', '00000000-0000-0000-0002-000000000004', '00000000-0000-0000-0001-000000000002'), -- M  Weber
  ('00000000-0000-0000-0004-000000000018', '00000000-0000-0000-0003-000000000007', '00000000-0000-0000-0002-000000000004', '00000000-0000-0000-0001-000000000003'), -- E  Fischer
  ('00000000-0000-0000-0004-000000000019', '00000000-0000-0000-0003-000000000004', '00000000-0000-0000-0002-000000000004', '00000000-0000-0000-0001-000000000004'), -- Ph  Wagner
  ('00000000-0000-0000-0004-000000000020', '00000000-0000-0000-0003-000000000010', '00000000-0000-0000-0002-000000000004', '00000000-0000-0000-0001-000000000006'), -- Ch  Koch
  -- Klasse 7b
  ('00000000-0000-0000-0004-000000000021', '00000000-0000-0000-0003-000000000001', '00000000-0000-0000-0002-000000000005', '00000000-0000-0000-0001-000000000001'), -- D  Krause
  ('00000000-0000-0000-0004-000000000022', '00000000-0000-0000-0003-000000000002', '00000000-0000-0000-0002-000000000005', '00000000-0000-0000-0001-000000000002'), -- M  Kern
  ('00000000-0000-0000-0004-000000000023', '00000000-0000-0000-0003-000000000003', '00000000-0000-0000-0002-000000000005', '00000000-0000-0000-0001-000000000003'), -- E  Bauer
  ('00000000-0000-0000-0004-000000000024', '00000000-0000-0000-0003-000000000004', '00000000-0000-0000-0002-000000000005', '00000000-0000-0000-0001-000000000004'), -- Ph  Wagner
  ('00000000-0000-0000-0004-000000000025', '00000000-0000-0000-0003-000000000009', '00000000-0000-0000-0002-000000000005', '00000000-0000-0000-0001-000000000007'), -- Ge  Richter
  -- Klasse 8a
  ('00000000-0000-0000-0004-000000000026', '00000000-0000-0000-0003-000000000005', '00000000-0000-0000-0002-000000000006', '00000000-0000-0000-0001-000000000001'), -- D  Schmidt
  ('00000000-0000-0000-0004-000000000027', '00000000-0000-0000-0003-000000000008', '00000000-0000-0000-0002-000000000006', '00000000-0000-0000-0001-000000000002'), -- M  Weber
  ('00000000-0000-0000-0004-000000000028', '00000000-0000-0000-0003-000000000007', '00000000-0000-0000-0002-000000000006', '00000000-0000-0000-0001-000000000003'), -- E  Fischer
  ('00000000-0000-0000-0004-000000000029', '00000000-0000-0000-0003-000000000010', '00000000-0000-0000-0002-000000000006', '00000000-0000-0000-0001-000000000006'), -- Ch  Koch
  ('00000000-0000-0000-0004-000000000030', '00000000-0000-0000-0003-000000000006', '00000000-0000-0000-0002-000000000006', '00000000-0000-0000-0001-000000000005')  -- Bio Hoffmann
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Sprechtage (Issue #123): je einer in jedem Zustand, den ein Besucher sehen will — aktiv,
-- Anmeldung beendet, abgeschlossen, Entwurf, abgesagt.
--
-- Alle Daten stehen relativ zu CURRENT_DATE: Der Demo-Deploy setzt die Datenbank täglich zurück
-- und seedet neu, so stimmen die Zustände an jedem Tag. Die Zugangs-Tokens sind fest und
-- sprechend, damit die Elternlinks gezielt aufrufbar sind (/elternsprechtag/demo-aktiv usw.).
-- ---------------------------------------------------------------------------
INSERT INTO sprechtage (id, titel, start_date, start_time, end_time, slot_in_minutes, access_token,
                        location, description, status, schulkontakt, erinnerung_vorlauf,
                        anmeldefrist_tage) VALUES
  -- Aktiv: in drei Wochen, Anmeldeschluss in zwei Wochen.
  ('00000000-0000-0000-0005-000000000001', 'Elternsprechtag Klassen 5 und 6',
   CURRENT_DATE + 21, '14:00', '18:00', 10, 'demo-aktiv',
   'Hauptgebäude, Erdgeschoss', 'Bitte bringen Sie das Hausaufgabenheft Ihres Kindes mit.',
   'VEROEFFENTLICHT', E'Sekretariat, Frau Albers\nTel. 0123 456789\nMo–Fr 7:30–13:00 Uhr',
   'EIN_TAG', 7),
  -- Anmeldung beendet: in drei Tagen, bei sieben Tagen Frist also seit vier Tagen geschlossen.
  ('00000000-0000-0000-0005-000000000002', 'Elternsprechtag Klassen 7 und 8',
   CURRENT_DATE + 3, '15:00', '18:00', 15, 'demo-anmeldung-beendet',
   'Neubau, 1. Obergeschoss', NULL,
   'VEROEFFENTLICHT', E'Sekretariat, Frau Albers\nTel. 0123 456789\nMo–Fr 7:30–13:00 Uhr',
   'KEINE', 7),
  -- Abgeschlossen: vor vier Wochen.
  ('00000000-0000-0000-0005-000000000003', 'Elternsprechtag Herbst',
   CURRENT_DATE - 28, '14:00', '17:00', 15, 'demo-abgeschlossen',
   'Hauptgebäude, Erdgeschoss', NULL,
   'ABGESCHLOSSEN', E'Sekretariat, Frau Albers\nTel. 0123 456789\nMo–Fr 7:30–13:00 Uhr',
   'KEINE', 1),
  -- Entwurf: in acht Wochen, noch ohne Termine.
  ('00000000-0000-0000-0005-000000000004', 'Elternsprechtag Sommer',
   CURRENT_DATE + 56, '14:00', '18:00', 10, 'demo-entwurf',
   'Hauptgebäude, Erdgeschoss', NULL,
   'ENTWURF', E'Sekretariat, Frau Albers\nTel. 0123 456789\nMo–Fr 7:30–13:00 Uhr',
   'KEINE', 7),
  -- Abgesagt: in zehn Tagen, schon vor dem Anmeldeschluss abgesagt. Ohne ihn fehlte die
  -- Hinweisseite „abgesagt“, die die Anwender-Doku unter dem Zugangs-Link zeigt (Issue #210).
  ('00000000-0000-0000-0005-000000000005', 'Elternsprechtag Winter',
   CURRENT_DATE + 10, '15:00', '18:00', 15, 'demo-abgesagt',
   'Neubau, 1. Obergeschoss', NULL,
   'ABGESAGT', E'Sekretariat, Frau Albers\nTel. 0123 456789\nMo–Fr 7:30–13:00 Uhr',
   'KEINE', 7)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sprechtage_klassen (sprechtag_id, klasse_id) VALUES
  ('00000000-0000-0000-0005-000000000001', '00000000-0000-0000-0002-000000000001'), -- 5a
  ('00000000-0000-0000-0005-000000000001', '00000000-0000-0000-0002-000000000002'), -- 5b
  ('00000000-0000-0000-0005-000000000001', '00000000-0000-0000-0002-000000000003'), -- 6a
  ('00000000-0000-0000-0005-000000000002', '00000000-0000-0000-0002-000000000004'), -- 7a
  ('00000000-0000-0000-0005-000000000002', '00000000-0000-0000-0002-000000000005'), -- 7b
  ('00000000-0000-0000-0005-000000000002', '00000000-0000-0000-0002-000000000006'), -- 8a
  ('00000000-0000-0000-0005-000000000003', '00000000-0000-0000-0002-000000000001'), -- 5a
  ('00000000-0000-0000-0005-000000000003', '00000000-0000-0000-0002-000000000004'), -- 7a
  ('00000000-0000-0000-0005-000000000004', '00000000-0000-0000-0002-000000000001'), -- 5a
  ('00000000-0000-0000-0005-000000000004', '00000000-0000-0000-0002-000000000002'), -- 5b
  ('00000000-0000-0000-0005-000000000004', '00000000-0000-0000-0002-000000000003'), -- 6a
  ('00000000-0000-0000-0005-000000000005', '00000000-0000-0000-0002-000000000006')  -- 8a
ON CONFLICT (sprechtag_id, klasse_id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Termine: so, wie das Veröffentlichen sie materialisiert (MaterialisierenService) — je Lehrkraft
-- mit einem Lehrauftrag in einer teilnehmenden Klasse ein Termin pro Slot; ein Rest-Slot, der
-- nicht mehr voll ins Zeitfenster passt, entfällt. Der Entwurf bekommt keine. Die ids leiten sich
-- deterministisch aus Sprechtag, Lehrkraft und Slot ab — die Buchungen unten rechnen genauso.
--
-- Am Sprechtag mit beendeter Anmeldung fällt Thomas Wagner ab 16 Uhr aus (Slot 4 und später): So
-- zeigt die Auswertung entfallene Termine, wie sie die Sammelaktion „Lehrkraft fällt aus“
-- hinterlässt (Issue #207).
-- ---------------------------------------------------------------------------
INSERT INTO termin (id, startzeit, endzeit, verfuegbarkeit, version, lehrer_id, sprechtag_id)
SELECT md5('demo-termin:' || s.id || ':' || l.lehrer_id || ':' || slot)::uuid,
       s.start_date + s.start_time + make_interval(mins => slot * s.slot_in_minutes),
       s.start_date + s.start_time + make_interval(mins => (slot + 1) * s.slot_in_minutes),
       CASE WHEN s.id = '00000000-0000-0000-0005-000000000002'
                 AND l.lehrer_id = '00000000-0000-0000-0003-000000000004'
                 AND slot >= 4
            THEN 'ENTFAELLT' ELSE 'VERFUEGBAR' END,
       1, l.lehrer_id, s.id
  FROM sprechtage s
  CROSS JOIN LATERAL (
    SELECT DISTINCT la.lehrer_id
      FROM lehrauftrag la
      JOIN sprechtage_klassen sk ON sk.klasse_id = la.klasse_id
     WHERE sk.sprechtag_id = s.id AND NOT la.stillgelegt) l
  CROSS JOIN LATERAL generate_series(
    0, (extract(epoch FROM s.end_time - s.start_time) / 60)::int / s.slot_in_minutes - 1) AS slot
 WHERE s.id IN ('00000000-0000-0000-0005-000000000001',
                '00000000-0000-0000-0005-000000000002',
                '00000000-0000-0000-0005-000000000003',
                '00000000-0000-0000-0005-000000000005')
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Buchungen: ein paar Familien, damit Auswertung und Belegung etwas zeigen. Der Termin ergibt sich
-- aus Sprechtag, der Lehrkraft des Lehrauftrags und dem Slot (0 = erster Slot des Tages); Lehrkraft,
-- Klasse und Fach stehen denormalisiert an der Buchung wie beim echten Buchen. Eine Buchung am
-- aktiven Sprechtag ist storniert und ihr Termin wieder frei: Ohne sie fehlte der Auswertung der
-- Schalter „Stornierte anzeigen“, den die Anwender-Doku zeigt (Issue #206). Am Sprechtag mit
-- beendeter Anmeldung hat der Ausfall von Thomas Wagner eine Buchung mitstorniert (Issue #207). Anna
-- Krause hat dort drei Termine, einer mit Notiz, und freie dazwischen: Ihr Blatt ist das
-- Beispielblatt der Anwender-Doku (Issue #208). Am abgeschlossenen Sprechtag hat Familie Wolf
-- neben drei Terminen einen stornierten: Die Auskunft, die die Anwender-Doku zeigt, findet ihn erst
-- über „Stornierte anzeigen“ (Issue #209).
-- ---------------------------------------------------------------------------
INSERT INTO buchungen (id, erstellt_am, status, schueler_name, eltern_name, eltern_email, notiz,
                       lehrauftrag_id, termin_id, lehrkraft_id, lehrkraft_name, lehrkraft_kuerzel,
                       klasse_name, fach_name)
SELECT md5('demo-buchung:' || s.id || ':' || la.id || ':' || b.slot)::uuid,
       b.erstellt_am, b.status, b.kind, b.eltern, b.email, b.notiz,
       la.id,
       md5('demo-termin:' || s.id || ':' || la.lehrer_id || ':' || b.slot)::uuid,
       l.id, l.vorname || ' ' || l.nachname, l.kuerzel, k.name, f.name
  FROM (VALUES
    -- Aktiv
    ('00000000-0000-0000-0005-000000000001', '00000000-0000-0000-0004-000000000001', 0, 'Emre Yilmaz',   'Ayşe Yilmaz',    'yilmaz@example.org',   NULL,                             CURRENT_DATE - 2 + time '19:12', 'ZUGESAGT'),
    ('00000000-0000-0000-0005-000000000001', '00000000-0000-0000-0004-000000000002', 2, 'Emre Yilmaz',   'Ayşe Yilmaz',    'yilmaz@example.org',   NULL,                             CURRENT_DATE - 2 + time '19:12', 'ZUGESAGT'),
    ('00000000-0000-0000-0005-000000000001', '00000000-0000-0000-0004-000000000006', 3, 'Lena Neumann',  'Sandra Neumann', 'neumann@example.org',  NULL,                             CURRENT_DATE - 1 + time '20:41', 'ZUGESAGT'),
    ('00000000-0000-0000-0005-000000000001', '00000000-0000-0000-0004-000000000008', 5, 'Lena Neumann',  'Sandra Neumann', 'neumann@example.org',  'Leseförderung besprechen',       CURRENT_DATE - 1 + time '20:41', 'ZUGESAGT'),
    ('00000000-0000-0000-0005-000000000001', '00000000-0000-0000-0004-000000000012', 4, 'Paul Becker',   'Thomas Becker',  'becker@example.org',   NULL,                             CURRENT_DATE - 1 + time '07:55', 'ZUGESAGT'),
    ('00000000-0000-0000-0005-000000000001', '00000000-0000-0000-0004-000000000014', 1, 'Paul Becker',   'Thomas Becker',  'becker@example.org',   NULL,                             CURRENT_DATE - 1 + time '07:55', 'ZUGESAGT'),
    ('00000000-0000-0000-0005-000000000001', '00000000-0000-0000-0004-000000000001', 1, 'Mia Schulz',    'Katrin Schulz',  'schulz@example.org',   NULL,                             CURRENT_DATE - 1 + time '06:30', 'ZUGESAGT'),
    ('00000000-0000-0000-0005-000000000001', '00000000-0000-0000-0004-000000000005', 6, 'Mia Schulz',    'Katrin Schulz',  'schulz@example.org',   'Knieverletzung, Sportbefreiung', CURRENT_DATE - 1 + time '06:30', 'ZUGESAGT'),
    ('00000000-0000-0000-0005-000000000001', '00000000-0000-0000-0004-000000000001', 2, 'Ben Krüger',    'Petra Krüger',   'krueger@example.org',  NULL,                             CURRENT_DATE - 3 + time '17:20', 'STORNIERT'),
    -- Anmeldung beendet
    ('00000000-0000-0000-0005-000000000002', '00000000-0000-0000-0004-000000000021', 0, 'Noah Hartmann', 'Jens Hartmann',  'hartmann@example.org', NULL,                             CURRENT_DATE - 9 + time '18:03', 'ZUGESAGT'),
    ('00000000-0000-0000-0005-000000000002', '00000000-0000-0000-0004-000000000021', 2, 'Sophie Lange',  'Claudia Lange',  'lange@example.org',    'Versetzung gefährdet?',          CURRENT_DATE - 10 + time '20:15', 'ZUGESAGT'),
    ('00000000-0000-0000-0005-000000000002', '00000000-0000-0000-0004-000000000021', 5, 'Elias Brandt',  'Murat Brandt',   'brandt@example.org',   NULL,                             CURRENT_DATE - 8 + time '16:58', 'ZUGESAGT'),
    ('00000000-0000-0000-0005-000000000002', '00000000-0000-0000-0004-000000000024', 2, 'Noah Hartmann', 'Jens Hartmann',  'hartmann@example.org', NULL,                             CURRENT_DATE - 9 + time '18:03', 'ZUGESAGT'),
    ('00000000-0000-0000-0005-000000000002', '00000000-0000-0000-0004-000000000019', 6, 'Lina Vogel',    'Sven Vogel',     'vogel@exmaple.org',    NULL,                             CURRENT_DATE - 8 + time '21:47', 'STORNIERT'),
    -- Abgeschlossen
    ('00000000-0000-0000-0005-000000000003', '00000000-0000-0000-0004-000000000001', 0, 'Emre Yilmaz',   'Ayşe Yilmaz',    'yilmaz@example.org',   NULL,                             CURRENT_DATE - 35 + time '19:40', 'ZUGESAGT'),
    ('00000000-0000-0000-0005-000000000003', '00000000-0000-0000-0004-000000000003', 2, 'Emre Yilmaz',   'Ayşe Yilmaz',    'yilmaz@example.org',   NULL,                             CURRENT_DATE - 35 + time '19:40', 'ZUGESAGT'),
    ('00000000-0000-0000-0005-000000000003', '00000000-0000-0000-0004-000000000016', 1, 'Jonas Wolf',    'Martina Wolf',   'wolf@example.org',     NULL,                             CURRENT_DATE - 33 + time '12:15', 'ZUGESAGT'),
    ('00000000-0000-0000-0005-000000000003', '00000000-0000-0000-0004-000000000019', 3, 'Jonas Wolf',    'Martina Wolf',   'wolf@example.org',     'Nachprüfung im Frühjahr?',       CURRENT_DATE - 33 + time '12:15', 'ZUGESAGT'),
    ('00000000-0000-0000-0005-000000000003', '00000000-0000-0000-0004-000000000017', 5, 'Jonas Wolf',    'Martina Wolf',   'wolf@example.org',     NULL,                             CURRENT_DATE - 33 + time '12:15', 'ZUGESAGT'),
    ('00000000-0000-0000-0005-000000000003', '00000000-0000-0000-0004-000000000002', 4, 'Mia Schulz',    'Katrin Schulz',  'schulz@example.org',   NULL,                             CURRENT_DATE - 30 + time '21:02', 'ZUGESAGT'),
    ('00000000-0000-0000-0005-000000000003', '00000000-0000-0000-0004-000000000018', 0, 'Jonas Wolf',    'Martina Wolf',   'wolf@example.org',     NULL,                             CURRENT_DATE - 33 + time '12:15', 'STORNIERT')
  ) AS b(sprechtag_id, lehrauftrag_id, slot, kind, eltern, email, notiz, erstellt_am, status)
  JOIN sprechtage s   ON s.id  = b.sprechtag_id::uuid
  JOIN lehrauftrag la ON la.id = b.lehrauftrag_id::uuid
  JOIN lehrer l       ON l.id  = la.lehrer_id
  JOIN klassen k      ON k.id  = la.klasse_id
  JOIN faecher f      ON f.id  = la.fach_id
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Eine Buchung, deren Angaben schon entfernt sind (Issue #209): So sieht die Zeile nach einem
-- Löschverlangen aus — Platzhalter wie aus einem Lauf, die Ersatzadresse als E-Mail, keine Notiz
-- und der Vermerk „Angaben entfernt am …“, den `anonymisiert_am` trägt. Die Zusage bleibt stehen
-- und zählt weiter. Der Sprechtag selbst ist noch nicht anonymisiert, seine Frist läuft noch.
-- ---------------------------------------------------------------------------
INSERT INTO buchungen (id, erstellt_am, status, schueler_name, eltern_name, eltern_email, notiz,
                       lehrauftrag_id, termin_id, lehrkraft_id, lehrkraft_name, lehrkraft_kuerzel,
                       klasse_name, fach_name, anonymisiert_am)
SELECT md5('demo-buchung:' || s.id || ':' || la.id || ':' || 2)::uuid,
       CURRENT_DATE - 34 + time '18:26', 'ZUGESAGT',
       'Schueler-7c1e4a90-001', 'Eltern-7c1e4a90-001', 'noreply@openclassware.de', NULL,
       la.id,
       md5('demo-termin:' || s.id || ':' || la.lehrer_id || ':' || 2)::uuid,
       l.id, l.vorname || ' ' || l.nachname, l.kuerzel, k.name, f.name,
       CURRENT_DATE - 3 + time '10:12'
  FROM sprechtage s
  JOIN lehrauftrag la ON la.id = '00000000-0000-0000-0004-000000000001'
  JOIN lehrer l       ON l.id  = la.lehrer_id
  JOIN klassen k      ON k.id  = la.klasse_id
  JOIN faecher f      ON f.id  = la.fach_id
 WHERE s.id = '00000000-0000-0000-0005-000000000003'
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Zustellungen (Issue #207): Die Ausfall-Mail an Familie Vogel hat der Mailserver abgelehnt — die
-- Adresse hat einen Tippfehler, der häufigste Grund. Ohne sie fehlten der Hinweis „1 Nachricht nicht
-- zugestellt“ in der Liste und der Block „Nicht erreicht“ in der Auswertung, die die Anwender-Doku
-- zeigt. Für alle übrigen Buchungen steht keine Zeile: Wo nichts gescheitert ist, zeigt die
-- Anwendung nichts.
-- ---------------------------------------------------------------------------
INSERT INTO zustellungen (buchung_id, art, ergebnis, zeitpunkt)
SELECT md5('demo-buchung:' || '00000000-0000-0000-0005-000000000002' || ':'
           || '00000000-0000-0000-0004-000000000019' || ':' || 6)::uuid,
       'AUSFALL', 'FEHLGESCHLAGEN', CURRENT_DATE - 1 + time '07:42'
ON CONFLICT (buchung_id, art) DO NOTHING;
