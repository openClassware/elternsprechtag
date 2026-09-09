package de.openclassware.elternsprechtag.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Der Check-Constraint aus Migration V3 — geprüft dort, wo er wirkt: am nackten SQL, an Service und
 * Hibernate vorbei.
 *
 * <p>Die Service-Prüfung in {@link SprechtagService} ist die getestete Wahrheit für die Anwendung;
 * dieser Test beantwortet die andere Frage: Kann ein veröffentlichter Sprechtag ohne Schulkontakt
 * überhaupt in der Datenbank stehen? Er darf es nicht — auch nicht durch ein Skript, einen
 * Datenbank-Client oder einen künftigen Schreibweg, der die Service-Prüfung umgeht.
 */
@ServiceTest
@Import({SprechtagService.class, BuchungService.class, KlassenService.class})
class SchulkontaktConstraintTest extends AbstractServiceTest {

  @Autowired private DataSource dataSource;

  private static final LocalDate DATE = LocalDate.of(2026, 7, 20);

  @Test
  void insert_publishedWithoutSchulkontakt_isRejected() {
    assertThatThrownBy(() -> insertSprechtag("VEROEFFENTLICHT", null))
        .isInstanceOf(SQLException.class);
  }

  @Test
  void insert_publishedWithBlankSchulkontakt_isRejected() {
    assertThatThrownBy(() -> insertSprechtag("VEROEFFENTLICHT", "   "))
        .isInstanceOf(SQLException.class);
  }

  @Test
  void insert_draftWithoutSchulkontakt_isAllowed() {
    assertThatCode(() -> insertSprechtag("ENTWURF", null)).doesNotThrowAnyException();
    assertThat(sprechtagRepository.count()).isEqualTo(1);
  }

  @Test
  void update_draftToPublishedWithoutSchulkontakt_isRejected() throws SQLException {
    UUID id = insertSprechtag("ENTWURF", null);

    assertThatThrownBy(
            () -> execute("update sprechtage set status = 'VEROEFFENTLICHT' where id = '" + id + "'"))
        .isInstanceOf(SQLException.class);
  }

  /** Legt eine Zeile per reinem SQL an — bewusst ohne JPA, damit nur die Datenbank urteilt. */
  private UUID insertSprechtag(String status, String schulkontakt) throws SQLException {
    UUID id = UUID.randomUUID();
    execute(
        "insert into sprechtage"
            + " (id, titel, start_date, start_time, end_time, slot_in_minutes, access_token,"
            + " status, schulkontakt) values ('"
            + id
            + "', 'Frühling', '"
            + DATE
            + "', '14:00', '15:00', 15, '"
            + UUID.randomUUID()
            + "', '"
            + status
            + "', "
            + (schulkontakt == null ? "null" : "'" + schulkontakt + "'")
            + ")");
    return id;
  }

  private void execute(String sql) throws SQLException {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }
}
