package de.openclassware.elternsprechtag.schulorganisation.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.openclassware.elternsprechtag.schulorganisation.application.port.out.Stammdatenansichten.LehrauftragDaten;
import de.openclassware.elternsprechtag.schulorganisation.domain.Fach;
import de.openclassware.elternsprechtag.schulorganisation.domain.Klasse;
import de.openclassware.elternsprechtag.schulorganisation.domain.Lehrauftrag;
import de.openclassware.elternsprechtag.schulorganisation.domain.Lehrkraft;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Die Persistenz des Schulorganisations-Kontexts: die vier Aggregate über ihre Ports und den
 * Query-Port daneben.
 *
 * <p>Die Fragen sind dieselben wie bei den Aggregaten der ersten beiden Scheiben — trägt das
 * Mapping hin und zurück, greift das optimistische Sperren, hält die Datenbank die Regel, die das
 * Aggregat bewusst nicht hält. Die letzte ist hier die eigentliche: Die Eindeutigkeit je (Lehrkraft,
 * Klasse, Fach) steht <b>nur</b> als Constraint in V6, und was nur in der Datenbank steht, ist genau
 * das, was ein Test beweisen muss.
 */
@DataJdbcTest
// Gegen die konfigurierte Postgres testen statt gegen eine untergeschobene Datenbank — Mapping und
// Constraints sollen gegen dasselbe Schema laufen wie im Betrieb.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@ActiveProfiles("test")
@Import({StammdatenPersistenceAdapter.class, StammdatenJdbcAdapter.class})
class StammdatenPersistenzTest {

  @Autowired private StammdatenPersistenceAdapter stammdaten;
  @Autowired private StammdatenJdbcAdapter ansichten;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void leereStammdaten() {
    jdbc.update("delete from buchungen");
    jdbc.update("delete from termin");
    jdbc.update("delete from sprechtage_klassen");
    jdbc.update("delete from lehrauftrag");
    jdbc.update("delete from klassen");
    jdbc.update("delete from faecher");
    jdbc.update("delete from lehrer");
  }

  @Test
  void eineLehrkraftKommtUnveraendertZurueck() {
    Lehrkraft berg = Lehrkraft.stelleEin("Anna", "Berg", "BER");
    stammdaten.speichere(berg);

    Lehrkraft geladen = stammdaten.lade(berg.id()).orElseThrow();

    assertThat(geladen.id()).isEqualTo(berg.id());
    assertThat(geladen.vorname()).isEqualTo("Anna");
    assertThat(geladen.nachname()).isEqualTo("Berg");
    assertThat(geladen.kuerzel()).isEqualTo("BER");
    assertThat(geladen.istStillgelegt()).isFalse();
  }

  /**
   * Der erste INSERT hebt die Version von 0 auf 1. Daran hängt mehr als eine Zahl: Spring Data JDBC
   * leitet „ist die Zeile neu?" aus {@code version == 0} ab — bliebe sie stehen, liefe das zweite
   * Speichern in einen INSERT und damit in den Primärschlüssel (siehe V6).
   */
  @Test
  void dasSpeichernHebtDieVersion() {
    Lehrkraft berg = Lehrkraft.stelleEin("Anna", "Berg", "BER");
    assertThat(berg.version()).isZero();

    stammdaten.speichere(berg);

    assertThat(stammdaten.lade(berg.id()).orElseThrow().version()).isEqualTo(1L);
  }

  @Test
  void einZweitesFensterMitAelteremStandSchreibtNicht() {
    Lehrkraft berg = Lehrkraft.stelleEin("Anna", "Berg", "BER");
    stammdaten.speichere(berg);

    Lehrkraft einFenster = stammdaten.lade(berg.id()).orElseThrow();
    Lehrkraft anderesFenster = stammdaten.lade(berg.id()).orElseThrow();
    einFenster.benenne("Anna", "Berg-Adler", "BEA");
    stammdaten.speichere(einFenster);

    anderesFenster.benenne("Anna", "Adler", "ADL");
    assertThatThrownBy(() -> stammdaten.speichere(anderesFenster))
        .isInstanceOf(OptimisticLockingFailureException.class);

    assertThat(stammdaten.lade(berg.id()).orElseThrow().nachname()).isEqualTo("Berg-Adler");
  }

  @Test
  void einStillgelegtesStammdatumBleibtStillgelegt() {
    Klasse klasse = Klasse.richteEin("5a");
    stammdaten.speichere(klasse);

    Klasse geladen = stammdaten.lade(klasse.id()).orElseThrow();
    geladen.legeStill();
    stammdaten.speichere(geladen);

    assertThat(stammdaten.lade(klasse.id()).orElseThrow().istStillgelegt()).isTrue();
  }

  @Test
  void einFachTraegtSeinKuerzelInDerSpalteShortName() {
    Fach deutsch = Fach.fuehreEin("Deutsch", "D");
    stammdaten.speichere(deutsch);

    assertThat(stammdaten.lade(deutsch.id()).orElseThrow().kuerzel()).isEqualTo("D");
    assertThat(jdbc.queryForObject("select short_name from faecher where id = ?", String.class, deutsch.id().wert()))
        .isEqualTo("D");
  }

  @Test
  void einLehrauftragKommtMitSeinenDreiVerweisenZurueck() {
    Lehrauftrag auftrag = erteile("Anna", "Berg", "BER", "5a", "Deutsch", "D");

    Lehrauftrag geladen = stammdaten.lade(auftrag.id()).orElseThrow();

    assertThat(geladen.lehrkraft()).isEqualTo(auftrag.lehrkraft());
    assertThat(geladen.klasse()).isEqualTo(auftrag.klasse());
    assertThat(geladen.fach()).isEqualTo(auftrag.fach());
  }

  /** Die Regel, die das Aggregat bewusst nicht kennt — hier steht sie auf dem Prüfstand. */
  @Test
  void dasselbeTripelLaesstSichKeinZweitesMalErteilen() {
    Lehrauftrag auftrag = erteile("Anna", "Berg", "BER", "5a", "Deutsch", "D");

    Lehrauftrag dublette =
        Lehrauftrag.erteile(auftrag.lehrkraft(), auftrag.klasse(), auftrag.fach());

    assertThatThrownBy(() -> stammdaten.speichere(dublette))
        .isInstanceOf(DuplicateKeyException.class);
  }

  @Test
  void dieLehrauftraegeEinerKlasseKommenNachFachnameSortiert() {
    Klasse klasse = Klasse.richteEin("5a");
    stammdaten.speichere(klasse);
    Lehrkraft berg = Lehrkraft.stelleEin("Anna", "Berg", "BER");
    stammdaten.speichere(berg);
    Fach mathe = Fach.fuehreEin("Mathematik", "M");
    stammdaten.speichere(mathe);
    Fach deutsch = Fach.fuehreEin("Deutsch", "D");
    stammdaten.speichere(deutsch);
    stammdaten.speichere(Lehrauftrag.erteile(berg.id(), klasse.id(), mathe.id()));
    stammdaten.speichere(Lehrauftrag.erteile(berg.id(), klasse.id(), deutsch.id()));

    assertThat(ansichten.lehrauftraegeEinerKlasse(klasse.id().wert()))
        .extracting(LehrauftragDaten::fach)
        .containsExactly("Deutsch", "Mathematik");
  }

  /**
   * Stillgelegt heißt „nimmt an nichts Neuem mehr teil" — und zwar unabhängig davon, welcher der
   * vier beteiligten Datensätze stillgelegt wurde.
   */
  @Test
  void eineStillgelegteLehrkraftStehtNichtMehrZurWahl() {
    Lehrauftrag auftrag = erteile("Anna", "Berg", "BER", "5a", "Deutsch", "D");
    Lehrkraft berg = stammdaten.lade(auftrag.lehrkraft()).orElseThrow();
    berg.legeStill();
    stammdaten.speichere(berg);

    assertThat(ansichten.lehrauftraegeEinerKlasse(auftrag.klasse().wert())).isEmpty();
  }

  /**
   * Der Gegenfall, und der wichtigere: Eine Buchung beruft sich auf ihren Lehrauftrag. Verschwände
   * er mit dem Stilllegen aus der Einzelabfrage, wäre die Auswertung des letzten Schuljahrs nicht
   * mehr lesbar.
   */
  @Test
  void einStillgelegterLehrauftragBleibtEinzelnAbrufbar() {
    Lehrauftrag auftrag = erteile("Anna", "Berg", "BER", "5a", "Deutsch", "D");
    Lehrauftrag geladen = stammdaten.lade(auftrag.id()).orElseThrow();
    geladen.legeStill();
    stammdaten.speichere(geladen);

    assertThat(ansichten.lehrauftrag(auftrag.id().wert()))
        .hasValueSatisfying(
            daten -> {
              assertThat(daten.nachname()).isEqualTo("Berg");
              assertThat(daten.klasse()).isEqualTo("5a");
              assertThat(daten.fach()).isEqualTo("Deutsch");
            });
  }

  @Test
  void stillgelegteKlassenFehlenDerWahlUndBleibenInDerVollenListe() {
    Klasse fuenfA = Klasse.richteEin("5a");
    stammdaten.speichere(fuenfA);
    Klasse siebenB = Klasse.richteEin("7b");
    stammdaten.speichere(siebenB);
    Klasse stillzulegen = stammdaten.lade(siebenB.id()).orElseThrow();
    stillzulegen.legeStill();
    stammdaten.speichere(stillzulegen);

    assertThat(ansichten.aktiveKlassen()).extracting("name").containsExactly("5a");
    assertThat(ansichten.alleKlassen()).extracting("name").containsExactly("5a", "7b");
  }

  @Test
  void einUnbekannterLehrauftragIstLeer() {
    assertThat(ansichten.lehrauftrag(java.util.UUID.randomUUID())).isEmpty();
  }

  private Lehrauftrag erteile(
      String vorname,
      String nachname,
      String kuerzel,
      String klassenname,
      String fachname,
      String fachkuerzel) {
    Lehrkraft lehrkraft = Lehrkraft.stelleEin(vorname, nachname, kuerzel);
    stammdaten.speichere(lehrkraft);
    Klasse klasse = Klasse.richteEin(klassenname);
    stammdaten.speichere(klasse);
    Fach fach = Fach.fuehreEin(fachname, fachkuerzel);
    stammdaten.speichere(fach);
    Lehrauftrag auftrag = Lehrauftrag.erteile(lehrkraft.id(), klasse.id(), fach.id());
    stammdaten.speichere(auftrag);
    return auftrag;
  }
}
