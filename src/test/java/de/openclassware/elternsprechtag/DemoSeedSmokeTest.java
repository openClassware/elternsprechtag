package de.openclassware.elternsprechtag;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Hält die Reihenfolge fest, in der das {@code demo}-Profil startet: erst die
 * Flyway-Migrationen, dann der Seed aus {@code data-demo.sql}.
 *
 * <p>Beides ist leicht zu zerstören, ohne dass es hier auffiele — die Demo ist die einzige Stelle,
 * an der das Seed-Skript überhaupt läuft, und ihr Bruch zeigt sich sonst erst auf
 * demo.openclassware.de. Konkret hing der Seed früher über {@code
 * spring.jpa.defer-datasource-initialization=true} hinter Hibernates Schema-Erstellung; mit Flyway
 * ist genau dieses Flag schädlich, weil die EntityManagerFactory damit nicht mehr auf die
 * Datenbank-Initialisierung wartet und Hibernate gegen ein noch leeres Schema validiert.
 *
 * <p>Der Test läuft zusätzlich unter dem Profil {@code test}, damit er die Test-Datenbank benutzt
 * und nicht die der Anwendung.
 */
@SpringBootTest
@ActiveProfiles({"test", "demo"})
class DemoSeedSmokeTest {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void seedLaeuftNachDenMigrationen() {
    // Kommt der Kontext überhaupt hoch, haben Migration und Hibernate-Validierung schon
    // zusammengepasst. Bleibt die Frage, ob danach auch der Seed gelaufen ist.
    assertThat(count("faecher")).isPositive();
    assertThat(count("klassen")).isPositive();
    assertThat(count("lehrer")).isPositive();
    assertThat(count("lehrauftrag")).isPositive();
  }

  private Integer count(String tabelle) {
    return jdbcTemplate.queryForObject("select count(*) from " + tabelle, Integer.class);
  }
}
