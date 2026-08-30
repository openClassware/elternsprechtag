package de.openclassware.elternsprechtag;

import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Lässt Flyway beim Kontextstart erst das Schema verwerfen und dann neu migrieren — der Ablauf
 * eines Demo-Deploys (siehe {@code .github/workflows/deploy-demo.yml}) in einem Testlauf.
 *
 * <p>Ohne das wären die Migrationstests von der Reihenfolge der Suite abhängig: Die Test-Datenbank
 * überlebt den Testlauf, Flyway spielt eine bereits vermerkte Migration nicht erneut ein, und die
 * Service-Tests räumen zwischendurch alle Tabellen ab. Der Seed wäre dann je nach Laufhistorie da
 * oder nicht. Nur ein tatsächlicher Kaltstart beantwortet die Frage, was eine frisch aufgesetzte
 * Instanz vorfindet.
 *
 * <p>Setzt {@code spring.flyway.clean-disabled=false} voraus; die Testklassen bringen es mit.
 */
@TestConfiguration
class FlywayFrischAufsetzen {

  @Bean
  FlywayMigrationStrategy schemaVerwerfenUndNeuMigrieren() {
    return flyway -> {
      flyway.clean();
      flyway.migrate();
    };
  }
}
