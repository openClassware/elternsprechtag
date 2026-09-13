-- V5: Der Sprechtag wird ein Aggregat (Issue #139, ADR 0003/0004).
--
-- Die Tabellen bleiben, wie sie sind — geändert hat sich, wer sie schreibt: statt einer
-- JPA-Entity mit Settern jetzt ein Aggregat über Spring Data JDBC. Schemaseitig fehlt dafür genau
-- eine Spalte.
--
-- `version` ist das optimistische Sperren am Root. Am Termin trägt es die Buchungs-Kollision
-- (ADR 0005); am Sprechtag trägt es die harmlosere, aber ebenso reale Kollision zweier
-- Organizer-Fenster — etwa „absagen" und „Titel speichern" gleichzeitig. Ohne die Spalte gewönne
-- stillschweigend, wer später speichert, und die Absage wäre wieder weg.
--
-- **`default 1`, nicht 0** — und das ist der Punkt, an dem diese Spalte kippen würde: Spring Data
-- JDBC leitet bei einer primitiven Versions-Eigenschaft „ist die Zeile neu?" aus `version == 0` ab.
-- Eine Bestandszeile mit 0 gälte beim ersten Speichern als neu, bekäme ein INSERT und liefe in eine
-- Verletzung des Primärschlüssels statt in ein UPDATE. Das Gegenstück gilt beim Schreiben: Ein
-- Aggregat mit `version == 0` hat es noch nie gegeben, und der erste INSERT hebt die Version auf 1.
-- Eine bestehende Zeile ist also genau dann richtig beschrieben, wenn sie mindestens 1 trägt.
alter table sprechtage
    add column version bigint not null default 1;

-- Dieselbe Rechnung für die Termine. Die Spalte gibt es seit V1, gefüllt hat sie damals Hibernate —
-- und dessen `@Version` beginnt bei 0. Zeilen aus dieser Zeit tragen deshalb eine 0 und liefen in
-- denselben INSERT; Zeilen, die das Aggregat seit V4 geschrieben hat, tragen bereits mindestens 1
-- und bleiben von der Bedingung unberührt.
update termin
   set version = 1
 where version = 0;

-- Die Verknüpfungstabelle hatte als @ManyToMany-Tabelle nie einen Primärschlüssel. Als Kindtabelle
-- eines Aggregats ist die Dublette jetzt ausgeschlossen: Eine Klasse nimmt an einem Sprechtag
-- entweder teil oder nicht — zweimal teilnehmen kann sie nicht.
alter table sprechtage_klassen
    add constraint pk_sprechtage_klassen primary key (sprechtag_id, klasse_id);
