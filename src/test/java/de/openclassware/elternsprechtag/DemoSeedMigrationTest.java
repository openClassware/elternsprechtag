package de.openclassware.elternsprechtag;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Hält fest, was eine frisch aufgesetzte Demo-Instanz vorfindet: dieselbe Migrationskette wie eine
 * Schulinstanz und dahinter die Demo-Stammdaten aus {@code db/demo/R__demo_stammdaten.sql}.
 *
 * <p>Der Seed hängt an einer einzigen Zeile — {@code spring.flyway.locations} in {@code
 * application-demo.properties}. Fällt {@code db/demo} dort heraus, startet die Anwendung weiterhin
 * anstandslos, nur eben ohne Stammdaten; fiele {@code db/migration} heraus, fehlte das Schema.
 * Beides zeigte sich sonst erst auf demo.openclassware.de.
 *
 * <p>Die erwarteten Zahlen stehen absichtlich als feste Werte da und nicht als „mehr als null“:
 * Die Demo soll nach jedem Reset exakt dieselben Stammdaten zeigen.
 *
 * <p>Der Test läuft zusätzlich unter dem Profil {@code test}, damit er die Test-Datenbank benutzt
 * und nicht die der Anwendung.
 */
@SpringBootTest(properties = "spring.flyway.clean-disabled=false")
@ActiveProfiles({"test", "demo"})
@Import(FlywayFrischAufsetzen.class)
class DemoSeedMigrationTest {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void migrationskettePlusSeedErgibtDieDemoStammdaten() {
    // Kommt der Kontext überhaupt hoch, haben Migration und Hibernate-Validierung schon
    // zusammengepasst. Bleibt die Frage, ob danach auch der Seed gelaufen ist.
    assertThat(count("faecher")).isEqualTo(8);
    assertThat(count("klassen")).isEqualTo(6);
    assertThat(count("lehrer")).isEqualTo(10);
    assertThat(count("lehrauftrag")).isEqualTo(30);
  }

  @Test
  void seedBringtKeineSprechtageOderBuchungenMit() {
    // Bewusst nur Stammdaten: Sprechtage legt der Besucher der Demo selbst an.
    assertThat(count("sprechtage")).isZero();
    assertThat(count("termin")).isZero();
    assertThat(count("buchungen")).isZero();
  }

  @Test
  void seedTraegtDieStammdatenInhaltlichEin() {
    // Stichprobe gegen die Zeilenzahlen oben: Die Zahlen allein überstünden auch vertauschte
    // Fremdschlüssel. Geprüft wird die Zuordnung, die ein Besucher der Demo als Erstes sieht —
    // welche Lehrkraft welches Fach in welcher Klasse unterrichtet.
    assertThat(
            jdbcTemplate.queryForObject(
                """
                select count(*) from lehrauftrag la
                  join lehrer l on l.id = la.lehrer_id
                  join klassen k on k.id = la.klasse_id
                  join faecher f on f.id = la.fach_id
                 where l.nachname = 'Krause' and k.name = '5a' and f.short_name = 'D'
                """,
                Integer.class))
        .isOne();
  }

  private Integer count(String tabelle) {
    return jdbcTemplate.queryForObject("select count(*) from " + tabelle, Integer.class);
  }
}
