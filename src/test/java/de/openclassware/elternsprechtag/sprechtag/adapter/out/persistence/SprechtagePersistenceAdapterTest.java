package de.openclassware.elternsprechtag.sprechtag.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.openclassware.elternsprechtag.sprechtag.domain.AccessToken;
import de.openclassware.elternsprechtag.sprechtag.domain.ErinnerungsVorlauf;
import de.openclassware.elternsprechtag.sprechtag.domain.KlasseId;
import de.openclassware.elternsprechtag.sprechtag.domain.Schulkontakt;
import de.openclassware.elternsprechtag.sprechtag.domain.Slotdauer;
import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import de.openclassware.elternsprechtag.sprechtag.domain.Zeitfenster;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
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
 * Dieselbe Frage wie in {@code TerminePersistenceAdapterTest}, für das zweite Aggregat: Trägt Spring
 * Data JDBC den Sprechtag samt seiner Kindzeilen?
 *
 * <p>Die Teilnehmerliste ist eine {@code Set}-Kindbeziehung <b>ohne eigene Id</b> — anders als die
 * Buchung am Termin. Beim Speichern schreibt Spring Data JDBC sie neu (Delete-and-Insert); dass
 * dabei genau die gewählten Klassen übrig bleiben und der Primärschlüssel aus V5 nicht dazwischenkommt,
 * ist hier bewiesen statt angenommen.
 *
 * <p>Ebenso geprüft: das optimistische Sperren am Root — und der Fall, an dem es kippen würde, wenn
 * die Version einer Bestandszeile auf 0 stünde. Spring Data JDBC hielte sie dann für neu und
 * schriebe ein INSERT statt eines UPDATE (siehe Migration V5).
 */
@DataJdbcTest
// Gegen die konfigurierte Postgres testen statt gegen eine untergeschobene Datenbank — Mapping,
// Kindzeilen und Locking sollen gegen dasselbe Schema laufen wie im Betrieb.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@ActiveProfiles("test")
@Import(SprechtagePersistenceAdapter.class)
class SprechtagePersistenceAdapterTest {

  private static final LocalDate DATUM = LocalDate.of(2026, 7, 20);
  private static final Zeitfenster NACHMITTAG =
      new Zeitfenster(LocalTime.of(14, 0), LocalTime.of(15, 0));

  @Autowired private SprechtagePersistenceAdapter sprechtage;
  @Autowired private JdbcTemplate jdbc;

  private KlasseId klasse5a;
  private KlasseId klasse7b;

  /**
   * Fremdschlüssel-Kulisse per SQL statt über die Fixtures der Use-Case-Naht: Dieser Test kennt
   * bewusst nur den Persistenz-Adapter, nicht die halbe Anwendung.
   */
  @BeforeEach
  void stammdaten() {
    jdbc.update("delete from sprechtage_klassen");
    jdbc.update("delete from termin");
    jdbc.update("delete from sprechtage");
    jdbc.update("delete from klassen");
    klasse5a = KlasseId.von(persistKlasse("5a"));
    klasse7b = KlasseId.von(persistKlasse("7b"));
  }

  private UUID persistKlasse(String name) {
    UUID id = UUID.randomUUID();
    jdbc.update("insert into klassen (id, name) values (?, ?)", id, name);
    return id;
  }

  private Sprechtag entwurf(KlasseId... klassen) {
    return Sprechtag.entwirf(
        "Frühling",
        "Aula",
        "Bitte pünktlich",
        Schulkontakt.von("Sekretariat, Tel. 0123 456789"),
        AccessToken.neu(),
        DATUM,
        NACHMITTAG,
        Slotdauer.vonMinuten(15),
        List.of(klassen),
        ErinnerungsVorlauf.KEINE);
  }

  @Test
  void speichertUndLaedtDasGanzeAggregat() {
    Sprechtag sprechtag = entwurf(klasse5a, klasse7b);

    sprechtage.speichere(sprechtag);

    Sprechtag geladen = sprechtage.lade(sprechtag.id()).orElseThrow();
    assertThat(geladen.titel()).isEqualTo("Frühling");
    assertThat(geladen.ort()).isEqualTo("Aula");
    assertThat(geladen.beschreibung()).isEqualTo("Bitte pünktlich");
    assertThat(geladen.schulkontakt()).isEqualTo(sprechtag.schulkontakt());
    assertThat(geladen.accessToken()).isEqualTo(sprechtag.accessToken());
    assertThat(geladen.datum()).isEqualTo(DATUM);
    assertThat(geladen.zeitfenster()).isEqualTo(NACHMITTAG);
    assertThat(geladen.slotdauer()).isEqualTo(Slotdauer.vonMinuten(15));
    assertThat(geladen.status()).isEqualTo(SprechtagStatus.ENTWURF);
    assertThat(geladen.klassen()).containsExactlyInAnyOrder(klasse5a, klasse7b);
  }

  /**
   * Eigener Test statt einer Assertion in {@link #speichertUndLaedtDasGanzeAggregat()}: Der
   * Standardwert {@code KEINE} dort würde einen falschen Spaltennamen, eine falsche
   * Enum-Konvertierung oder einen zu engen Check-Constraint nicht aufdecken.
   */
  @Test
  void erinnerungsVorlaufWirdMitgespeichert() {
    Sprechtag sprechtag = entwurf(klasse5a);
    sprechtag.aendereErinnerungsVorlauf(ErinnerungsVorlauf.ZWEI_TAGE);

    sprechtage.speichere(sprechtag);

    Sprechtag geladen = sprechtage.lade(sprechtag.id()).orElseThrow();
    assertThat(geladen.erinnerungsVorlauf()).isEqualTo(ErinnerungsVorlauf.ZWEI_TAGE);
  }

  @Test
  void ortUndBeschreibungDuerfenFehlen() {
    Sprechtag sprechtag =
        Sprechtag.entwirf(
            "Ohne Angaben",
            null,
            null,
            Schulkontakt.von("Sekretariat"),
            AccessToken.neu(),
            DATUM,
            NACHMITTAG,
            Slotdauer.vonMinuten(15),
            List.of(klasse5a),
            ErinnerungsVorlauf.KEINE);

    sprechtage.speichere(sprechtag);

    Sprechtag geladen = sprechtage.lade(sprechtag.id()).orElseThrow();
    assertThat(geladen.ort()).isNull();
    assertThat(geladen.beschreibung()).isNull();
  }

  /** Die Teilnehmerliste wird beim Speichern neu geschrieben — es bleibt genau die neue Auswahl. */
  @Test
  void geaenderteKlassenlisteErsetztDieAlte() {
    Sprechtag sprechtag = entwurf(klasse5a, klasse7b);
    sprechtage.speichere(sprechtag);

    Sprechtag neuGeladen = sprechtage.lade(sprechtag.id()).orElseThrow();
    neuGeladen.legeZeitstrukturFest(
        DATUM, NACHMITTAG, Slotdauer.vonMinuten(15), List.of(klasse7b));
    sprechtage.speichere(neuGeladen);

    assertThat(sprechtage.lade(sprechtag.id()).orElseThrow().klassen()).containsExactly(klasse7b);
    assertThat(jdbc.queryForObject("select count(*) from sprechtage_klassen", Long.class))
        .as("keine Karteileichen in der Verknüpfungstabelle")
        .isEqualTo(1);
  }

  @Test
  void findetDenSprechtagUeberSeinZugangsToken() {
    Sprechtag sprechtag = entwurf(klasse5a);
    sprechtage.speichere(sprechtag);

    Sprechtag geladen = sprechtage.ladeNachAccessToken(sprechtag.accessToken()).orElseThrow();

    assertThat(geladen.id()).isEqualTo(sprechtag.id());
    assertThat(geladen.klassen()).as("auch über diesen Weg ein ganzes Aggregat").hasSize(1);
  }

  @Test
  void unbekanntesTokenLiefertNichts() {
    assertThat(sprechtage.ladeNachAccessToken(AccessToken.neu())).isEmpty();
  }

  @Test
  void unbekannteIdLiefertNichts() {
    assertThat(sprechtage.lade(SprechtagId.neu())).isEmpty();
  }

  /**
   * Der erste INSERT hebt die Version auf 1. Das ist die Zusicherung, auf der die Migration V5
   * aufsetzt, wenn sie Bestandszeilen auf 1 statt auf 0 stellt.
   */
  @Test
  void dieVersionBeginntNachDemSpeichernBeiEins() {
    Sprechtag sprechtag = entwurf(klasse5a);
    assertThat(sprechtag.version()).isZero();

    sprechtage.speichere(sprechtag);

    assertThat(sprechtage.lade(sprechtag.id()).orElseThrow().version()).isEqualTo(1L);
  }

  /**
   * Ein Aggregat ist nach dem Speichern verbraucht: Es trägt noch die alte Version, die Datenbank
   * längst die neue. Ein zweites {@code speichere} derselben Instanz meldet deshalb einen Konflikt,
   * auch wenn niemand dazwischengekommen ist.
   *
   * <p>Bei einem frisch angelegten Sprechtag steht die Version der Instanz weiter auf 0 — Spring
   * Data JDBC hält sie darum für neu und setzt ein zweites INSERT auf dieselbe Id ab. Die Datenbank
   * meldet den Konflikt hier also als Schlüsseldublette und nicht als Versionskonflikt; Letzteren
   * zeigt {@link #zweiFensterAufDemselbenSprechtag_dasZweiteSchreibenScheitert()} am geladenen
   * Aggregat.
   */
  @Test
  void zweimalSpeichernDerselbenInstanzMeldetEinenKonflikt() {
    Sprechtag sprechtag = entwurf(klasse5a);
    sprechtage.speichere(sprechtag);

    assertThatThrownBy(() -> sprechtage.speichere(sprechtag))
        .as("verbrauchtes Aggregat, Version 0 — INSERT auf eine Id, die schon steht")
        .isInstanceOf(DuplicateKeyException.class);
  }

  @Test
  void zweiFensterAufDemselbenSprechtag_dasZweiteSchreibenScheitert() {
    Sprechtag sprechtag = entwurf(klasse5a);
    sprechtage.speichere(sprechtag);

    Sprechtag fensterA = sprechtage.lade(sprechtag.id()).orElseThrow();
    Sprechtag fensterB = sprechtage.lade(sprechtag.id()).orElseThrow();

    fensterA.veroeffentliche();
    sprechtage.speichere(fensterA);

    fensterB.beschreibeNeu(
        "Anderer Titel",
        null,
        null,
        Schulkontakt.von("Sekretariat"),
        fensterB.accessToken());
    assertThatThrownBy(() -> sprechtage.speichere(fensterB))
        .isInstanceOf(OptimisticLockingFailureException.class);

    assertThat(sprechtage.lade(sprechtag.id()).orElseThrow().status())
        .isEqualTo(SprechtagStatus.VEROEFFENTLICHT);
  }
}
