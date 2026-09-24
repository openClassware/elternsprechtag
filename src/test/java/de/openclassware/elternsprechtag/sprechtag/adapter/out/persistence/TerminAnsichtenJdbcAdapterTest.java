package de.openclassware.elternsprechtag.sprechtag.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import de.openclassware.elternsprechtag.sprechtag.application.port.out.TerminAnsichten.AusfallSlotZustand;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.TerminAnsichten.AusfallZeile;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.TerminAnsichten.EntfalleneZeile;
import de.openclassware.elternsprechtag.sprechtag.domain.Buchungsziel;
import de.openclassware.elternsprechtag.sprechtag.domain.Familie;
import de.openclassware.elternsprechtag.sprechtag.domain.LehrauftragId;
import de.openclassware.elternsprechtag.sprechtag.domain.LehrkraftId;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Die Leseseite des Ausfall-Dialogs (Sammelaktion „Lehrkraft fällt aus", Issue #156): drei
 * Zustände je Slot, korrekt abgeleitet aus {@code verfuegbarkeit} und einer optional daran
 * hängenden aktiven Buchung — auch bei mehreren Buchungen in der Historie desselben Slots.
 */
@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@ActiveProfiles("test")
@Import({TerminePersistenceAdapter.class, TerminAnsichtenJdbcAdapter.class})
class TerminAnsichtenJdbcAdapterTest {

  private static final LocalDateTime BEGINN = LocalDateTime.of(2026, 7, 20, 14, 0);

  @Autowired private TerminePersistenceAdapter termine;
  @Autowired private TerminAnsichtenJdbcAdapter ausfallSlots;
  @Autowired private JdbcTemplate jdbc;

  private SprechtagId sprechtag;
  private LehrkraftId lehrkraft;
  private LehrauftragId lehrauftrag;

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

  private Termin neuerTermin(LocalDateTime beginn) {
    return Termin.neu(sprechtag, lehrkraft, new Zeitraum(beginn, beginn.plusMinutes(15)));
  }

  private Buchungsziel ziel() {
    return new Buchungsziel(lehrauftrag, lehrkraft, "Anna Berg", "BER", "5a", "Deutsch");
  }

  private Familie familie(String name) {
    return new Familie("Eltern " + name, "Kind " + name, name + "@example.com");
  }

  @Test
  void ausfallSlots_unterscheidetFreiGebuchtUndEntfallen() {
    Termin frei = neuerTermin(BEGINN);
    Termin gebucht = neuerTermin(BEGINN.plusMinutes(15));
    gebucht.buche(familie("mueller"), ziel(), null, BEGINN);
    Termin entfallen = neuerTermin(BEGINN.plusMinutes(30));
    entfallen.lassEntfallen();
    termine.speichereAlle(List.of(frei, gebucht, entfallen));

    List<AusfallZeile> zeilen = ausfallSlots.ausfallSlots(sprechtag, lehrkraft);

    assertThat(zeilen)
        .extracting(AusfallZeile::zeit, AusfallZeile::zustand)
        .containsExactly(
            tuple(LocalTime.of(14, 0), AusfallSlotZustand.FREI),
            tuple(LocalTime.of(14, 15), AusfallSlotZustand.GEBUCHT),
            tuple(LocalTime.of(14, 30), AusfallSlotZustand.ENTFALLEN));
    AusfallZeile gebuchteZeile = zeilen.get(1);
    assertThat(gebuchteZeile.schuelerName()).isEqualTo("Kind mueller");
    assertThat(gebuchteZeile.elternName()).isEqualTo("Eltern mueller");
    assertThat(gebuchteZeile.familienSchluessel()).isEqualTo("mueller@example.com");
  }

  @Test
  void ausfallSlots_mehrereBuchungenInDerHistorie_zeigtNurDieAktive() {
    Termin termin = neuerTermin(BEGINN);
    var erste = termin.buche(familie("mueller"), ziel(), null, BEGINN);
    termine.speichere(termin);
    Termin geladen = termine.lade(termin.id()).orElseThrow();
    geladen.storniere(erste);
    geladen.buche(familie("schmidt"), ziel(), null, BEGINN.plusMinutes(1));
    termine.speichere(geladen);

    List<AusfallZeile> zeilen = ausfallSlots.ausfallSlots(sprechtag, lehrkraft);

    assertThat(zeilen).singleElement().satisfies(zeile -> {
      assertThat(zeile.zustand()).isEqualTo(AusfallSlotZustand.GEBUCHT);
      assertThat(zeile.schuelerName()).isEqualTo("Kind schmidt");
    });
  }

  @Test
  void ausfallSlots_andereLehrkraft_bleibtAusgeschlossen() {
    termine.speichere(neuerTermin(BEGINN));

    assertThat(ausfallSlots.ausfallSlots(sprechtag, LehrkraftId.neu())).isEmpty();
  }

  @Test
  void entfalleneJeLehrkraft_keinTerminEntfaellt_liefertLeereListe() {
    termine.speichereAlle(List.of(neuerTermin(BEGINN), neuerTermin(BEGINN.plusMinutes(15))));

    assertThat(ausfallSlots.entfalleneJeLehrkraft(sprechtag)).isEmpty();
  }

  @Test
  void entfalleneJeLehrkraft_zaehltNurEntfalleneJeLehrkraft() {
    LehrkraftId zweiteLehrkraft = LehrkraftId.neu();
    jdbc.update(
        "insert into lehrer (id, vorname, nachname, kuerzel) values (?, ?, ?, ?)",
        zweiteLehrkraft.wert(),
        "Carl",
        "Adler",
        "ADL");

    Termin frei = neuerTermin(BEGINN);
    Termin entfallenBerg1 = neuerTermin(BEGINN.plusMinutes(15));
    entfallenBerg1.lassEntfallen();
    Termin entfallenBerg2 = neuerTermin(BEGINN.plusMinutes(30));
    entfallenBerg2.lassEntfallen();
    Termin entfallenAdler =
        Termin.neu(sprechtag, zweiteLehrkraft, new Zeitraum(BEGINN, BEGINN.plusMinutes(15)));
    entfallenAdler.lassEntfallen();
    termine.speichereAlle(List.of(frei, entfallenBerg1, entfallenBerg2, entfallenAdler));

    List<EntfalleneZeile> zeilen = ausfallSlots.entfalleneJeLehrkraft(sprechtag);

    assertThat(zeilen)
        .extracting(EntfalleneZeile::lehrkraftId, EntfalleneZeile::anzahl)
        .containsExactlyInAnyOrder(tuple(lehrkraft.wert(), 2), tuple(zweiteLehrkraft.wert(), 1));
  }
}
