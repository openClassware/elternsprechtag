package de.openclassware.elternsprechtag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten.BuchungsZeile;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten.LehrkraftPlan;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten.NichtErreicht;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten.SprechtagAuswertung;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Sprechtagszugang;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Sprechtagszugang.Zugangsstand;
import de.openclassware.elternsprechtag.sprechtag.domain.Mailart;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Hält fest, was eine frisch aufgesetzte Demo-Instanz vorfindet: dieselbe Migrationskette wie eine
 * Schulinstanz und dahinter die Demo-Daten aus {@code db/demo/R__demo_stammdaten.sql} —
 * Stammdaten und je ein Sprechtag in jedem Zustand.
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
  @Autowired private Sprechtagszugang sprechtagszugang;
  @Autowired private Auswerten auswerten;

  @Test
  void migrationskettePlusSeedErgibtDieDemoStammdaten() {
    // Kommt der Kontext überhaupt hoch, ist die Migrationskette durchgelaufen.
    // Bleibt die Frage, ob danach auch der Seed gelaufen ist.
    assertThat(count("faecher")).isEqualTo(8);
    assertThat(count("klassen")).isEqualTo(6);
    assertThat(count("lehrer")).isEqualTo(10);
    assertThat(count("lehrauftrag")).isEqualTo(30);
  }

  /**
   * Issue #123 — je ein Sprechtag in jedem Zustand, den ein Besucher sehen will, ohne erst Daten
   * anzulegen. Die Daten stehen relativ zu {@code CURRENT_DATE}; der tägliche Reset seedet neu.
   */
  @Test
  void seedBringtJeEinenSprechtagInJedemZustandMit() {
    assertThat(
            jdbcTemplate.queryForList(
                "select status from sprechtage order by status", String.class))
        .containsExactly(
            "ABGESAGT", "ABGESCHLOSSEN", "ENTWURF", "VEROEFFENTLICHT", "VEROEFFENTLICHT");
    assertThat(count("termin")).isPositive();
    assertThat(count("buchungen")).isPositive();
  }

  /** Jede Seite, die ein Elternlink zeigen kann — die Anwender-Doku fotografiert sie alle. */
  @Test
  void dieElternlinksDerDemoZeigenJedenZugangsstand() {
    assertThat(sprechtagszugang.oeffne("demo-aktiv").orElseThrow().stand())
        .isEqualTo(Zugangsstand.BUCHBAR);
    assertThat(sprechtagszugang.oeffne("demo-anmeldung-beendet").orElseThrow().stand())
        .isEqualTo(Zugangsstand.ANMELDUNG_BEENDET);
    assertThat(sprechtagszugang.oeffne("demo-abgeschlossen").orElseThrow().stand())
        .isEqualTo(Zugangsstand.VORBEI);
    assertThat(sprechtagszugang.oeffne("demo-abgesagt").orElseThrow().stand())
        .isEqualTo(Zugangsstand.ABGESAGT);
    assertThat(sprechtagszugang.oeffne("demo-entwurf").orElseThrow().stand())
        .isEqualTo(Zugangsstand.NICHT_VERFUEGBAR);
  }

  /** Ein Entwurf hat noch keine Termine — sie entstehen erst beim Veröffentlichen. */
  @Test
  void derEntwurfHatKeineTermine() {
    assertThat(
            jdbcTemplate.queryForObject(
                """
                select count(*) from termin t join sprechtage s on s.id = t.sprechtag_id
                 where s.status = 'ENTWURF'
                """,
                Integer.class))
        .isZero();
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

  /**
   * Issue #207 — die Anwender-Doku zeigt einen Teilausfall und eine Familie, die die Ausfall-Mail
   * nicht erreicht hat. Geprüft über die Auswertung, wie der Organisator sie sieht: Die Zustellung
   * hängt an einer Buchungs-Id, die der Seed nur errechnet, und ein Rechenfehler ließe den Block
   * „Nicht erreicht“ still leer.
   */
  @Test
  void derSprechtagMitBeendeterAnmeldungZeigtAusfallUndNichtErreichteFamilie() {
    SprechtagAuswertung auswertung =
        auswerten.werteAus(UUID.fromString("00000000-0000-0000-0005-000000000002")).orElseThrow();

    LehrkraftPlan wagner =
        auswertung.plaene().stream()
            .filter(plan -> plan.kuerzel().equals("WAG"))
            .findFirst()
            .orElseThrow();
    assertThat(wagner.entfalleneAnzahl()).isEqualTo(8);
    assertThat(wagner.stornierte()).singleElement().matches(zeile -> zeile.entfallen());

    assertThat(auswertung.nichtErreicht())
        .singleElement()
        .extracting(NichtErreicht::art, NichtErreicht::schuelerName, NichtErreicht::email)
        .containsExactly(Mailart.AUSFALL, "Lina Vogel", "vogel@exmaple.org");
  }

  /**
   * Issue #208 — die Anwender-Doku zeigt das Tagesplan-Blatt von Anna Krause für den Sprechtag in
   * drei Tagen: belegte Termine, einer davon mit Notiz, und freie dazwischen. Gesucht wird am
   * Telefon nach Hartmann, der bei zwei Lehrkräften einen Termin hat.
   */
  @Test
  void derSprechtagMitBeendeterAnmeldungFuelltEinTagesplanBlatt() {
    SprechtagAuswertung auswertung =
        auswerten.werteAus(UUID.fromString("00000000-0000-0000-0005-000000000002")).orElseThrow();

    LehrkraftPlan krause =
        auswertung.plaene().stream()
            .filter(plan -> plan.kuerzel().equals("KRA"))
            .findFirst()
            .orElseThrow();
    assertThat(krause.zeilen())
        .extracting(BuchungsZeile::startzeit, BuchungsZeile::schuelerName)
        .containsExactly(
            tuple(LocalTime.of(15, 0), "Noah Hartmann"),
            tuple(LocalTime.of(15, 30), "Sophie Lange"),
            tuple(LocalTime.of(16, 15), "Elias Brandt"));
    assertThat(krause.zeilen()).anyMatch(zeile -> zeile.notiz() != null);

    assertThat(auswertung.plaene())
        .filteredOn(
            plan ->
                plan.zeilen().stream()
                    .anyMatch(zeile -> zeile.elternName().equals("Jens Hartmann")))
        .hasSize(2);
  }

  /**
   * Issue #209 — die Anwender-Doku zeigt am abgeschlossenen Sprechtag eine Buchung, deren Angaben
   * schon entfernt sind, und beantwortet eine Auskunft über Familie Wolf, die neben drei geltenden
   * Terminen eine stornierte Buchung hat. Geprüft über die Auswertung: Der Vermerk „Angaben entfernt
   * am …“ hängt an einer Spalte, die der Seed eigens setzen muss.
   */
  @Test
  void derAbgeschlosseneSprechtagZeigtEntfernteAngabenUndEineStornierteBuchung() {
    SprechtagAuswertung auswertung =
        auswerten.werteAus(UUID.fromString("00000000-0000-0000-0005-000000000003")).orElseThrow();

    assertThat(auswertung.anonymisiertAm()).isNull();
    assertThat(auswertung.plaene())
        .flatExtracting(LehrkraftPlan::zeilen)
        .filteredOn(zeile -> zeile.anonymisiertAm() != null)
        .singleElement()
        .satisfies(
            zeile -> {
              assertThat(zeile.schuelerName()).startsWith("Schueler-");
              assertThat(zeile.notiz()).isNull();
            });

    assertThat(auswertung.plaene())
        .flatExtracting(LehrkraftPlan::stornierte)
        .singleElement()
        .satisfies(
            zeile -> {
              assertThat(zeile.schuelerName()).isEqualTo("Jonas Wolf");
              assertThat(zeile.entfallen()).isFalse();
            });
  }

  private Integer count(String tabelle) {
    return jdbcTemplate.queryForObject("select count(*) from " + tabelle, Integer.class);
  }
}
