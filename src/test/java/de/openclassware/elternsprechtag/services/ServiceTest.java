package de.openclassware.elternsprechtag.services;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gemeinsame Spring-Test-Konfiguration der Service-Naht.
 *
 * <p>Bündelt an einer Stelle, was bisher auf jeder Service-Testklasse einzeln stand. Der Grund ist
 * nicht Schreibersparnis: Die Konfiguration hier entscheidet, gegen welche Datenbank getestet wird.
 * Verstreut auf fünf Klassen genügt eine vergessene Annotation, damit ein neuer Service-Test still
 * gegen eine andere Datenbank läuft als alle anderen.
 *
 * <p>Die fachlichen {@code @Import}-Listen bleiben bewusst bei den einzelnen Testklassen — die
 * unterscheiden sich je Test und gehören dorthin, wo man sie liest.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@DataJpaTest
// Gegen die konfigurierte Postgres testen, nicht gegen eine untergeschobene Datenbank.
// @DataJpaTest ersetzt die Datasource sonst per Default (Replace.NON_TEST) durch eine
// eingebettete — die Suite bliebe grün, liefe aber gegen eine andere Datenbank als die
// Produktion. Nur so läuft ab dem Flyway-Ticket dieselbe Migrationskette auch in Tests.
// Preis: die Service-Tests brauchen eine laufende Datenbank (siehe docs/ci.md).
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
// Flyway gehört nicht zum Umfang des @DataJpaTest-Schnitts (dessen Auto-Konfigurationsliste kennt
// nur JPA). Ohne diese Zeile stünde die Test-Datenbank ohne Schema da und Hibernates `validate`
// ließe den Kontext gar nicht erst hochkommen. Mit ihr läuft in Tests dieselbe Migrationskette
// wie im Betrieb — und der Kontextstart prüft nebenbei, dass Migrationen und Entitäten
// zusammenpassen.
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
// Eigene Test-Datenbank statt der Entwicklungsdatenbank. Zwingend, nicht kosmetisch: Die
// Fixture-Reinigung in AbstractServiceTest räumt vor jedem Test alle Tabellen ab — gegen
// `elternsprechtag` würde ein Testlauf die Daten leeren, mit denen gerade entwickelt wird.
// Siehe src/test/resources/application-test.properties.
@ActiveProfiles("test")
// Keine umschließende Test-Transaktion: die Fixture-Saves committen sofort, sodass die eigenen
// Transaktionsgrenzen der Services real beobachtbar bleiben. Übernommen aus den bisherigen
// Testklassen — siehe AbstractServiceTest.
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@interface ServiceTest {}
