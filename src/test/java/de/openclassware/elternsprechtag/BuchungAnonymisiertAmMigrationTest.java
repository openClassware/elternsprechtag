package de.openclassware.elternsprechtag;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * V11 trägt den Anonymisierungs-Zeitpunkt an bestehende Buchungen nach (Issue #129): Was der Lauf
 * aus #126 schon anonymisiert hat, bekäme sonst in der Auswertung noch „Angaben entfernen"
 * angeboten.
 *
 * <p>Wie {@link DatenbankNachRollbackTest} stellt der Test den Zustand selbst her: Er migriert
 * eine leere Test-Datenbank nur bis V10, legt dort Altbestand an und lässt dann den regulären
 * Flyway dieses Kontextes den Rest einspielen.
 */
@SpringBootTest
@ActiveProfiles("test")
class BuchungAnonymisiertAmMigrationTest {

  private static final UUID ANONYMISIERTE_BUCHUNG = UUID.randomUUID();
  private static final UUID OFFENE_BUCHUNG = UUID.randomUUID();
  private static final LocalDateTime LAUF = LocalDateTime.of(2026, 9, 1, 1, 0);

  @Autowired private DataSource dataSource;

  /** Der reguläre Flyway dieses Kontextes: Suchpfade und Validierungsregeln der Anwendung. */
  @Autowired private Flyway flyway;

  @Test
  void v11_uebernimmtDenZeitpunktDesSprechtagsNurFuerDessenBuchungen() {
    bisV10MitAltbestand();

    flyway.migrate();

    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    assertThat(anonymisiertAm(jdbc, ANONYMISIERTE_BUCHUNG)).isEqualTo(LAUF);
    assertThat(anonymisiertAm(jdbc, OFFENE_BUCHUNG)).isNull();
  }

  /** Hinterlässt eine leere Datenbank auf dem aktuellen Stand — sie überlebt den Testlauf sonst. */
  @AfterEach
  void aufraeumen() {
    frischerFlyway().load().clean();
    flyway.migrate();
  }

  private void bisV10MitAltbestand() {
    Flyway bisV10 = frischerFlyway().target("10").load();
    bisV10.clean();
    bisV10.migrate();

    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    UUID fach = UUID.randomUUID();
    UUID klasse = UUID.randomUUID();
    UUID lehrer = UUID.randomUUID();
    UUID lehrauftrag = UUID.randomUUID();
    jdbc.update("insert into faecher (id, name, short_name) values (?, 'Deutsch', 'D')", fach);
    jdbc.update("insert into klassen (id, name) values (?, '5a')", klasse);
    jdbc.update(
        "insert into lehrer (id, vorname, nachname, kuerzel) values (?, 'Anna', 'Berg', 'BER')",
        lehrer);
    jdbc.update(
        "insert into lehrauftrag (id, lehrer_id, klasse_id, fach_id) values (?, ?, ?, ?)",
        lehrauftrag,
        lehrer,
        klasse,
        fach);

    buchungAn(jdbc, sprechtag(jdbc, "herbst", LAUF), lehrer, lehrauftrag, ANONYMISIERTE_BUCHUNG);
    buchungAn(jdbc, sprechtag(jdbc, "fruehling", null), lehrer, lehrauftrag, OFFENE_BUCHUNG);
  }

  private static UUID sprechtag(JdbcTemplate jdbc, String token, LocalDateTime anonymisiertAm) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        """
        insert into sprechtage (id, titel, start_date, start_time, end_time, slot_in_minutes,
                                access_token, status, schulkontakt, erinnerung_vorlauf,
                                anmeldefrist_tage, anonymisiert_am)
        values (?, ?, date '2026-07-20', time '14:00', time '15:00', 15, ?, 'ABGESCHLOSSEN',
                'Sekretariat', 'KEINE', 7, ?)
        """,
        id,
        "Sprechtag " + token,
        token,
        anonymisiertAm);
    return id;
  }

  private static void buchungAn(
      JdbcTemplate jdbc, UUID sprechtag, UUID lehrer, UUID lehrauftrag, UUID buchung) {
    UUID termin = UUID.randomUUID();
    jdbc.update(
        """
        insert into termin (id, startzeit, endzeit, verfuegbarkeit, version, lehrer_id, sprechtag_id)
        values (?, timestamp '2026-07-20 14:00', timestamp '2026-07-20 14:15', 'VERFUEGBAR', 1, ?, ?)
        """,
        termin,
        lehrer,
        sprechtag);
    jdbc.update(
        """
        insert into buchungen (id, erstellt_am, status, schueler_name, eltern_name, eltern_email,
                               lehrauftrag_id, termin_id, lehrkraft_id, lehrkraft_name,
                               lehrkraft_kuerzel, klasse_name, fach_name)
        values (?, timestamp '2026-07-01 10:00', 'ZUGESAGT', 'Kind', 'Eltern', 'e@example.org',
                ?, ?, ?, 'Anna Berg', 'BER', '5a', 'Deutsch')
        """,
        buchung,
        lehrauftrag,
        termin,
        lehrer);
  }

  private static LocalDateTime anonymisiertAm(JdbcTemplate jdbc, UUID buchung) {
    return jdbc.queryForObject(
        "select anonymisiert_am from buchungen where id = ?", LocalDateTime.class, buchung);
  }

  private FluentConfiguration frischerFlyway() {
    return Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .cleanDisabled(false);
  }
}
