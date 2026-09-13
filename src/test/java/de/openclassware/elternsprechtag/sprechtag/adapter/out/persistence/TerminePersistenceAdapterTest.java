package de.openclassware.elternsprechtag.sprechtag.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import de.openclassware.elternsprechtag.sprechtag.domain.Buchung;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungId;
import de.openclassware.elternsprechtag.sprechtag.domain.Buchungsstatus;
import de.openclassware.elternsprechtag.sprechtag.domain.Buchungsziel;
import de.openclassware.elternsprechtag.sprechtag.domain.Familie;
import de.openclassware.elternsprechtag.sprechtag.domain.LehrauftragId;
import de.openclassware.elternsprechtag.sprechtag.domain.LehrkraftId;
import de.openclassware.elternsprechtag.sprechtag.domain.Notiz;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
import de.openclassware.elternsprechtag.sprechtag.domain.TerminId;
import de.openclassware.elternsprechtag.sprechtag.domain.Verfuegbarkeit;
import de.openclassware.elternsprechtag.sprechtag.domain.Zeitraum;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Der Beweis, den ADR 0004 ausdrücklich verlangt hat: trägt Spring Data JDBC ein Aggregat mit
 * Kind-Zeilen?
 *
 * <p>Spring Data JDBC schreibt die Kind-Zeilen beim Speichern neu (Delete-and-Insert). Weil die
 * Domäne ihre Ids selbst vergibt, <em>sollten</em> die {@code Buchung}-Ids dabei stabil bleiben —
 * daran hängt, dass {@code BuchungenBestaetigt} sie nach dem Commit in den {@code @Async}-Listener
 * trägt und die Bestätigungsmail sie noch findet. Das war anzunehmen und ist hier bewiesen.
 *
 * <p>Ebenso geprüft: das optimistische Sperren am Root, auf dem der Buchungsvorgang aufsetzt
 * (ADR 0005).
 */
@DataJdbcTest
// Gegen die konfigurierte Postgres testen statt gegen eine untergeschobene Datenbank — die
// Aggregat-Zuordnung und das Locking sollen gegen dasselbe Schema laufen wie im Betrieb.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@ActiveProfiles("test")
@Import(TerminePersistenceAdapter.class)
class TerminePersistenceAdapterTest {

  private static final LocalDateTime BEGINN = LocalDateTime.of(2026, 7, 20, 14, 0);

  @Autowired private TerminePersistenceAdapter termine;
  @Autowired private JdbcTemplate jdbc;

  private SprechtagId sprechtag;
  private LehrkraftId lehrkraft;
  private LehrauftragId lehrauftrag;

  /**
   * Fremdschlüssel-Kulisse per SQL statt über die JPA-Fixtures: Dieser Test kennt bewusst nur den
   * Persistenz-Adapter, nicht die halbe Anwendung.
   */
  @BeforeEach
  void stammdaten() {
    sprechtag = SprechtagId.neu();
    lehrkraft = LehrkraftId.neu();
    lehrauftrag = LehrauftragId.neu();
    UUID klasse = UUID.randomUUID();
    UUID fach = UUID.randomUUID();

    jdbc.update(
        "insert into sprechtage (id, titel, start_date, start_time, end_time, slot_in_minutes,"
            + " access_token, status, schulkontakt) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
        sprechtag.wert(),
        "Frühling",
        LocalDate.of(2026, 7, 20),
        LocalTime.of(14, 0),
        LocalTime.of(15, 0),
        15,
        UUID.randomUUID().toString(),
        "VEROEFFENTLICHT",
        "Sekretariat");
    jdbc.update(
        "insert into lehrer (id, vorname, nachname, kuerzel) values (?, ?, ?, ?)",
        lehrkraft.wert(),
        "Anna",
        "Berg",
        "BER");
    jdbc.update("insert into klassen (id, name) values (?, ?)", klasse, "5a");
    jdbc.update(
        "insert into faecher (id, name, short_name) values (?, ?, ?)", fach, "Deutsch", "D");
    jdbc.update(
        "insert into lehrauftrag (id, lehrer_id, klasse_id, fach_id) values (?, ?, ?, ?)",
        lehrauftrag.wert(),
        lehrkraft.wert(),
        klasse,
        fach);
  }

  private Termin neuerTermin() {
    return Termin.neu(sprechtag, lehrkraft, new Zeitraum(BEGINN, BEGINN.plusMinutes(15)));
  }

  private Buchungsziel ziel() {
    return new Buchungsziel(lehrauftrag, lehrkraft, "Anna Berg", "BER", "5a", "Deutsch");
  }

  private Familie familie(String name) {
    return new Familie("Eltern " + name, "Kind " + name, name + "@example.com");
  }

  @Test
  void speichereUndLade_gibtDasAggregatUnveraendertZurueck() {
    Termin termin = neuerTermin();
    BuchungId buchung = termin.buche(familie("mueller"), ziel(), new Notiz("Anliegen"), BEGINN);
    termine.speichere(termin);

    Termin geladen = termine.lade(termin.id()).orElseThrow();

    assertThat(geladen.id()).isEqualTo(termin.id());
    assertThat(geladen.sprechtag()).isEqualTo(sprechtag);
    assertThat(geladen.lehrkraft()).isEqualTo(lehrkraft);
    assertThat(geladen.zeitraum()).isEqualTo(new Zeitraum(BEGINN, BEGINN.plusMinutes(15)));
    assertThat(geladen.verfuegbarkeit()).isEqualTo(Verfuegbarkeit.VERFUEGBAR);
    assertThat(geladen.istBuchbar()).isFalse();
    Buchung geladeneBuchung = geladen.aktiveBuchung().orElseThrow();
    assertThat(geladeneBuchung.id()).isEqualTo(buchung);
    assertThat(geladeneBuchung.familie()).isEqualTo(familie("mueller"));
    assertThat(geladeneBuchung.ziel()).isEqualTo(ziel());
    assertThat(geladeneBuchung.notiz()).contains(new Notiz("Anliegen"));
  }

  @Test
  void buchungIds_bleibenUeberWiederholtesSpeichernStabil() {
    Termin termin = neuerTermin();
    BuchungId ersteId = termin.buche(familie("mueller"), ziel(), null, BEGINN);
    termine.speichere(termin);

    // Dreimal erneut speichern — Spring Data JDBC schreibt die Kind-Zeilen dabei jedes Mal neu.
    for (int runde = 0; runde < 3; runde++) {
      termine.speichere(termine.lade(termin.id()).orElseThrow());
    }

    assertThat(buchungIdsInDerDatenbank(termin)).containsExactly(ersteId.wert());
    assertThat(termine.lade(termin.id()).orElseThrow().aktiveBuchung().orElseThrow().id())
        .as("die Bestätigungsmail findet die Buchung nach dem Commit nur unter dieser Id")
        .isEqualTo(ersteId);
  }

  @Test
  void stornoUndNeubuchung_lassenDieAlteBuchungsIdUnveraendert() {
    Termin termin = neuerTermin();
    BuchungId ersteId = termin.buche(familie("mueller"), ziel(), null, BEGINN);
    termine.speichere(termin);

    Termin geladen = termine.lade(termin.id()).orElseThrow();
    geladen.storniere(ersteId);
    BuchungId zweiteId = geladen.buche(familie("schmidt"), ziel(), null, BEGINN.plusMinutes(1));
    termine.speichere(geladen);

    Termin danach = termine.lade(termin.id()).orElseThrow();
    assertThat(danach.buchungen())
        .extracting(Buchung::id, Buchung::status)
        .containsExactly(
            tuple(ersteId, Buchungsstatus.STORNIERT),
            tuple(zweiteId, Buchungsstatus.ZUGESAGT));
    assertThat(danach.aktiveBuchung().orElseThrow().id()).isEqualTo(zweiteId);
  }

  @Test
  void speichere_mitVeraltetemStand_meldetVersionskonflikt() {
    Termin termin = neuerTermin();
    termine.speichere(termin);

    Termin einer = termine.lade(termin.id()).orElseThrow();
    Termin anderer = termine.lade(termin.id()).orElseThrow();
    einer.buche(familie("mueller"), ziel(), null, BEGINN);
    termine.speichere(einer);

    anderer.buche(familie("schmidt"), ziel(), null, BEGINN);
    assertThatThrownBy(() -> termine.speichere(anderer))
        .as("genau dieser Konflikt wird im Use Case zu TerminBelegtException")
        .isInstanceOf(OptimisticLockingFailureException.class);
  }

  @Test
  void verfuegbarkeit_wirdGespeichert() {
    Termin termin = neuerTermin();
    termin.lassEntfallen();
    termine.speichere(termin);

    assertThat(termine.lade(termin.id()).orElseThrow().verfuegbarkeit())
        .isEqualTo(Verfuegbarkeit.ENTFAELLT);
  }

  @Test
  void existierenFuer_kenntDenSprechtag() {
    assertThat(termine.existierenFuer(sprechtag)).isFalse();

    termine.speichereAlle(List.of(neuerTermin(), neuerTermin()));

    assertThat(termine.existierenFuer(sprechtag)).isTrue();
    assertThat(termine.existierenFuer(SprechtagId.neu())).isFalse();
  }

  @Test
  void lade_unbekannterTermin_istLeer() {
    assertThat(termine.lade(TerminId.neu()))
        .isEmpty();
  }

  private List<UUID> buchungIdsInDerDatenbank(Termin termin) {
    return jdbc.queryForList(
        "select id from buchungen where termin_id = ? order by erstellt_am",
        UUID.class,
        termin.id().wert());
  }
}
