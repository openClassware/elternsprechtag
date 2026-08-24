package de.openclassware.elternsprechtag.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Hält fest, gegen welche Datenbank die Service-Naht läuft.
 *
 * <p>Ohne diesen Test ist der Unterschied unsichtbar: {@code @DataJpaTest} schiebt standardmäßig
 * eine eingebettete Datenbank unter ({@code Replace.NON_TEST}), und die Suite bleibt dabei grün —
 * nur eben gegen eine andere Datenbank als die Produktion. Genau das soll nicht mehr passieren:
 * Datenbankspezifisches Verhalten muss in Tests sichtbar werden, und ab Flyway gibt es nur noch
 * eine Schema-Wahrheit.
 *
 * <p>Schlägt dieser Test fehl, läuft die Service-Naht wieder gegen eine untergeschobene Datenbank
 * — dann fehlt {@code Replace.NONE} an {@link ServiceTest}.
 */
@ServiceTest
class ServiceTestDatenbankTest {

  @Autowired private DataSource dataSource;

  @Test
  void serviceTestsLaufenGegenPostgres() throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
      assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
    }
  }
}
