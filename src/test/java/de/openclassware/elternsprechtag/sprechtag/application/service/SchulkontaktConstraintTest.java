package de.openclassware.elternsprechtag.sprechtag.application.service;

import de.openclassware.elternsprechtag.sprechtag.AbstractServiceTest;
import de.openclassware.elternsprechtag.sprechtag.ServiceTest;
import de.openclassware.elternsprechtag.SprechtagKontextTestConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Der Check-Constraint aus Migration V3 — geprüft dort, wo er wirkt: am nackten SQL, an Aggregat
 * und Use Case vorbei.
 *
 * <p>Der Wert {@code Schulkontakt} der Domäne ist die getestete Wahrheit für die Anwendung;
 * dieser Test beantwortet die andere Frage: Kann ein Sprechtag ohne Schulkontakt überhaupt in der
 * Datenbank stehen? Er darf es nicht — in keinem Status und auch nicht durch ein Skript, einen
 * Datenbank-Client oder einen künftigen Schreibweg, der die Domäne umgeht.
 */
@ServiceTest
@Import(SprechtagKontextTestConfig.class)
class SchulkontaktConstraintTest extends AbstractServiceTest {

  @Autowired private DataSource dataSource;

  private static final LocalDate DATE = LocalDate.of(2026, 7, 20);

  @Test
  void insert_draftWithoutSchulkontakt_isRejected() {
    assertThatThrownBy(() -> insertSprechtag("ENTWURF", null)).isInstanceOf(SQLException.class);
  }

  @Test
  void insert_draftWithBlankSchulkontakt_isRejected() {
    assertThatThrownBy(() -> insertSprechtag("ENTWURF", "   ")).isInstanceOf(SQLException.class);
  }

  @Test
  void insert_publishedWithoutSchulkontakt_isRejected() {
    assertThatThrownBy(() -> insertSprechtag("VEROEFFENTLICHT", null))
        .isInstanceOf(SQLException.class);
  }

  @Test
  void insert_withSchulkontakt_isAllowed() {
    assertThatCode(() -> insertSprechtag("ENTWURF", SCHULKONTAKT)).doesNotThrowAnyException();
    assertThat(jdbc.queryForObject("select count(*) from sprechtage", Long.class)).isEqualTo(1);
  }

  @Test
  void update_toBlankSchulkontakt_isRejected() throws SQLException {
    UUID id = insertSprechtag("ENTWURF", SCHULKONTAKT);

    assertThatThrownBy(() -> execute("update sprechtage set schulkontakt = ' ' where id = ?", id))
        .isInstanceOf(SQLException.class);
  }

  /** Legt eine Zeile per reinem SQL an — bewusst am Aggregat vorbei, damit nur die Datenbank urteilt. */
  private UUID insertSprechtag(String status, String schulkontakt) throws SQLException {
    UUID id = UUID.randomUUID();
    execute(
        "insert into sprechtage"
            + " (id, titel, start_date, start_time, end_time, slot_in_minutes, access_token,"
            + " status, schulkontakt) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
        id,
        "Frühling",
        Date.valueOf(DATE),
        Time.valueOf(LocalTime.of(14, 0)),
        Time.valueOf(LocalTime.of(15, 0)),
        15,
        UUID.randomUUID().toString(),
        status,
        schulkontakt);
    return id;
  }

  private void execute(String sql, Object... parameter) throws SQLException {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      for (int i = 0; i < parameter.length; i++) {
        statement.setObject(i + 1, parameter[i]);
      }
      statement.execute();
    }
  }
}
