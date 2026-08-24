package de.openclassware.elternsprechtag.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

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
 *
 * <p>Die {@code @Import}-Liste ist für die Aussage des Tests ohne Bedeutung und nur deshalb da,
 * damit der Spring-Kontext derselbe ist wie in den anderen Service-Tests. Ohne sie ergäbe sich ein
 * eigener Cache-Schlüssel und die Suite würde einen zweiten Kontext bloß für diese eine Zusicherung
 * hochfahren.
 */
@ServiceTest
@Import({SprechtagService.class, BuchungService.class, KlassenService.class})
class ServiceTestDatenbankTest {

  @Autowired private DataSource dataSource;

  @Test
  void serviceTestsLaufenGegenPostgres() throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
      assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
    }
  }
}
