package de.openclassware.elternsprechtag;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Die Gegenprobe zu {@link DemoSeedMigrationTest}: Eine Schulinstanz — also jede Instanz ohne das
 * Profil {@code demo} — startet auf einer leeren Datenbank und bekommt dabei keine Demo-Daten.
 *
 * <p>Das ist kein Selbstläufer, sondern hängt daran, dass {@code db/demo} ausschließlich in
 * {@code application-demo.properties} zu den Flyway-Suchpfaden gehört. Rutschte der Seed in das
 * Standardverzeichnis {@code db/migration}, fänden die Eltern einer echten Schule erfundene
 * Lehrkräfte in ihren Terminlisten — und der Demo-Test bliebe davon unberührt grün.
 */
@SpringBootTest(properties = "spring.flyway.clean-disabled=false")
@ActiveProfiles("test")
@Import(FlywayFrischAufsetzen.class)
class SchulinstanzOhneDemoSeedTest {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void frischMigrierteSchulinstanzIstLeer() {
    assertThat(count("faecher")).isZero();
    assertThat(count("klassen")).isZero();
    assertThat(count("lehrer")).isZero();
    assertThat(count("lehrauftrag")).isZero();
  }

  private Integer count(String tabelle) {
    return jdbcTemplate.queryForObject("select count(*) from " + tabelle, Integer.class);
  }
}
