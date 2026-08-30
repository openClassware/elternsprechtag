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
 * Eine Datenbank, auf der bereits eine neuere Schemaversion lief, muss auch mit einer älteren
 * Auslieferung wieder starten.
 *
 * <p>Das ist der Rollback-Fall: Das zurückgerollte Image bringt die zuletzt angewandte Migration
 * nicht mehr mit, Flyway findet in der Historientabelle also einen Eintrag aus der Zukunft.
 * Flyway lässt das von Haus aus durch — sein Standardwert für {@code
 * spring.flyway.ignore-migration-patterns} ist genau {@code *:future}. Die Eigenschaft ERSETZT
 * diesen Standardwert aber, sobald sie gesetzt wird, und gesetzt ist sie hier: für den Demo-Seed
 * (siehe {@link DatenbankNachDemoBetriebTest}). Fällt {@code *:future} aus der Aufzählung in
 * application.properties heraus, verweigert jede zurückgerollte Instanz den Start — und die
 * lokale Entwicklung ebenso, sobald jemand von einem Branch mit neuer Migration zurückwechselt.
 *
 * <p>Wie {@link DatenbankNachDemoBetriebTest} stellt der Test den Zustand selbst her, statt sich
 * auf eine vorherige Testklasse zu verlassen: Erst migriert er die Test-Datenbank mit einem
 * zusätzlichen Verzeichnis, das eine spätere Schemaversion mitbringt, dann lässt er den regulären
 * Flyway dieses Kontextes darüber laufen — der kennt dieses Verzeichnis nicht.
 */
@SpringBootTest
@ActiveProfiles("test")
class DatenbankNachRollbackTest {

  @Autowired private DataSource dataSource;

  /** Der reguläre Flyway dieses Kontextes: Suchpfade und Validierungsregeln der Anwendung. */
  @Autowired private Flyway flyway;

  @Test
  void startetNachRollbackAufAeltereSchemaversionWeiter() {
    spaetereSchemaversionNachstellen();

    assertThatCode(() -> flyway.migrate()).doesNotThrowAnyException();
  }

  /** Hinterlässt eine Datenbank ohne die Zukunftsmigration — sie überlebt den Testlauf sonst. */
  @AfterEach
  void zukunftsmigrationEntfernen() {
    ohneZukunft().clean();
    flyway.migrate();
  }

  private void spaetereSchemaversionNachstellen() {
    Flyway mitZukunft =
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration", "classpath:db/zukunft")
            .cleanDisabled(false)
            .load();
    mitZukunft.clean();
    mitZukunft.migrate();
  }

  private Flyway ohneZukunft() {
    return Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .cleanDisabled(false)
        .load();
  }
}
