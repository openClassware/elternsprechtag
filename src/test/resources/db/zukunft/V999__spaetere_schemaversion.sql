-- Nur für DatenbankNachRollbackTest: eine Schemaversion, die neuer ist als alles unter
-- db/migration. Sie steht bewusst NICHT in db/migration, sondern in einem Verzeichnis, das
-- ausschließlich dieser Test in seine Flyway-Suchpfade aufnimmt.
--
-- Was sie anlegt, ist gleichgültig — gebraucht wird allein der Eintrag in der Historientabelle,
-- den ein anschließender Lauf ohne dieses Verzeichnis als "future" vorfindet.
CREATE TABLE spaetere_schemaversion (id uuid PRIMARY KEY);
