package de.openclassware.elternsprechtag;

import static org.assertj.core.api.Assertions.assertThatCode;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Eine Datenbank, die einmal unter dem Profil {@code demo} lief, muss auch ohne dieses Profil
 * wieder starten.
 *
 * <p>Der Demo-Seed ist eine wiederholbare Migration in einem Verzeichnis, das nur das
 * {@code demo}-Profil in seinen Suchpfaden hat. Ohne das Profil findet Flyway also einen
 * angewandten Eintrag in der Historientabelle, zu dem es kein Skript mehr gibt — und verweigert
 * ohne Weiteres die Arbeit ("Detected applied migration not resolved locally"). Getroffen hätte
 * das vor allem die lokale Entwicklung, wo dieselbe Datenbank mal mit und mal ohne {@code demo}
 * startet. Aufgefangen wird es durch {@code spring.flyway.ignore-migration-patterns} in
 * application.properties.
 *
 * <p>Der Test stellt den Zustand selbst her, statt sich auf eine vorherige Testklasse zu
 * verlassen: Erst migriert er die Test-Datenbank wie eine Demo-Instanz, dann lässt er den
 * regulären Flyway dieses Kontextes darüber laufen — der kennt {@code db/demo} nicht.
 */
@SpringBootTest
@ActiveProfiles("test")
class DatenbankNachDemoBetriebTest {

  @Autowired private DataSource dataSource;

  /** Der reguläre Flyway dieses Kontextes: Suchpfade und Validierungsregeln der Anwendung. */
  @Autowired private Flyway flyway;

  @Test
  void startetAuchOhneDemoProfilWeiter() {
    demoInstanzNachstellen();

    assertThatCode(() -> flyway.migrate()).doesNotThrowAnyException();
  }

  /** Hinterlässt eine Datenbank ohne Demo-Spuren — die Historientabelle überlebt den Testlauf. */
  @AfterEach
  void demoSpurenEntfernen() {
    ohneDemo().clean();
    flyway.migrate();
  }

  private void demoInstanzNachstellen() {
    Flyway mitDemo =
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration", "classpath:db/demo")
            .cleanDisabled(false)
            .load();
    mitDemo.clean();
    mitDemo.migrate();
  }

  private Flyway ohneDemo() {
    return Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .cleanDisabled(false)
        .load();
  }
}
