package de.openclassware.elternsprechtag.sprechtag.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import de.openclassware.elternsprechtag.sprechtag.application.port.out.TerminAnsichten.LehrkraftSlotZeile;
import de.openclassware.elternsprechtag.sprechtag.domain.LehrkraftId;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import de.openclassware.elternsprechtag.sprechtag.domain.TerminId;
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
 * Der Leseweg des Ausfall-Dialogs (#156): alle Slots einer Lehrkraft samt Zustand, aktiver Buchung,
 * Namen und Familien-Schlüssel.
 *
 * <p>Geprüft wird hier, was am SQL hängt und nirgends sonst auffiele — vor allem, dass ein Slot mit
 * mehreren Buchungen in seiner Historie <em>eine</em> Zeile ergibt und nicht eine je Storno.
 */
@DataJdbcTest
// Gegen die konfigurierte Postgres statt gegen eine untergeschobene Datenbank: Ein Left Join auf
// einen Teilzustand ist genau das, was zwischen zwei Dialekten auseinandergeht.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@ActiveProfiles("test")
@Import(TerminAnsichtenJdbcAdapter.class)
class TerminAnsichtenJdbcAdapterTest {

  private static final LocalDate DATUM = LocalDate.of(2026, 7, 20);
  private static final LocalDateTime BEGINN = LocalDateTime.of(2026, 7, 20, 14, 0);

  @Autowired private TerminAnsichtenJdbcAdapter ansichten;
  @Autowired private JdbcTemplate jdbc;

  private SprechtagId sprechtag;
  private LehrkraftId berg;
  private LehrkraftId conrad;
  private UUID lehrauftrag;

  /** Kulisse per SQL: Dieser Test kennt den Query-Adapter, nicht die halbe Anwendung. */
  @BeforeEach
  void stammdaten() {
    sprechtag = SprechtagId.neu();
    berg = LehrkraftId.neu();
    conrad = LehrkraftId.neu();
    lehrauftrag = UUID.randomUUID();
    UUID klasse = UUID.randomUUID();
    UUID fach = UUID.randomUUID();

    jdbc.update(
        "insert into sprechtage (id, titel, start_date, start_time, end_time, slot_in_minutes,"
            + " access_token, status, schulkontakt) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
        sprechtag.wert(),
        "Frühling",
        DATUM,
        LocalTime.of(14, 0),
        LocalTime.of(15, 0),
        15,
        UUID.randomUUID().toString(),
        "VEROEFFENTLICHT",
        "Sekretariat");
    insertLehrkraft(berg, "Anna", "Berg", "BER");
    insertLehrkraft(conrad, "Carl", "Conrad", "CON");
    jdbc.update("insert into klassen (id, name) values (?, ?)", klasse, "5a");
    jdbc.update("insert into faecher (id, name, short_name) values (?, ?, ?)", fach, "Deutsch", "D");
    jdbc.update(
        "insert into lehrauftrag (id, lehrer_id, klasse_id, fach_id) values (?, ?, ?, ?)",
        lehrauftrag,
        berg.wert(),
        klasse,
        fach);
  }

  @Test
  void slotsDerLehrkraft_liefertFreieGebuchteUndEntfalleneChronologisch() {
    TerminId frei = termin(berg, 0, false);
    TerminId gebucht = termin(berg, 15, false);
    TerminId entfallen = termin(berg, 30, true);
    buche(gebucht, "Lena Müller", "Familie Müller", "mueller@example.com", "ZUGESAGT");

    List<LehrkraftSlotZeile> zeilen = ansichten.slotsDerLehrkraft(sprechtag, berg);

    assertThat(zeilen)
        .extracting(
            LehrkraftSlotZeile::terminId,
            LehrkraftSlotZeile::zeit,
            LehrkraftSlotZeile::entfaellt,
            LehrkraftSlotZeile::schuelerName,
            LehrkraftSlotZeile::elternName,
            LehrkraftSlotZeile::elternAdresse)
        .containsExactly(
            tuple(frei.wert(), LocalTime.of(14, 0), false, null, null, null),
            tuple(
                gebucht.wert(),
                LocalTime.of(14, 15),
                false,
                "Lena Müller",
                "Familie Müller",
                "mueller@example.com"),
            tuple(entfallen.wert(), LocalTime.of(14, 30), true, null, null, null));
  }

  @Test
  void slotsDerLehrkraft_mehrereBuchungenInDerHistorie_ergebenEineZeile() {
    TerminId slot = termin(berg, 0, false);
    buche(slot, "Erst Storniert", "Familie Eins", "eins@example.com", "STORNIERT");
    buche(slot, "Dann Storniert", "Familie Zwei", "zwei@example.com", "STORNIERT");
    buche(slot, "Jetzt Gebucht", "Familie Drei", "drei@example.com", "ZUGESAGT");

    List<LehrkraftSlotZeile> zeilen = ansichten.slotsDerLehrkraft(sprechtag, berg);

    assertThat(zeilen)
        .as("angehängt wird nur die aktive Buchung, nicht die Historie")
        .singleElement()
        .extracting(LehrkraftSlotZeile::schuelerName, LehrkraftSlotZeile::elternAdresse)
        .containsExactly("Jetzt Gebucht", "drei@example.com");
  }

  @Test
  void slotsDerLehrkraft_nurStornierteBuchungen_istEinFreierSlot() {
    TerminId slot = termin(berg, 0, false);
    buche(slot, "Weg Gebucht", "Familie Weg", "weg@example.com", "STORNIERT");

    assertThat(ansichten.slotsDerLehrkraft(sprechtag, berg))
        .singleElement()
        .extracting(LehrkraftSlotZeile::schuelerName)
        .isNull();
  }

  @Test
  void slotsDerLehrkraft_entfallenerSlotMitBuchung_bleibtEineZeile() {
    // Wie der Vorgang ihn hinterlässt: Verfügbarkeit ENTFAELLT, Buchung storniert.
    TerminId slot = termin(berg, 0, true);
    buche(slot, "Lena Müller", "Familie Müller", "mueller@example.com", "STORNIERT");

    assertThat(ansichten.slotsDerLehrkraft(sprechtag, berg))
        .singleElement()
        .extracting(LehrkraftSlotZeile::entfaellt, LehrkraftSlotZeile::schuelerName)
        .containsExactly(true, null);
  }

  @Test
  void slotsDerLehrkraft_kenntNurDieGefragteLehrkraftUndDenGefragtenSprechtag() {
    termin(berg, 0, false);
    termin(conrad, 0, false);

    assertThat(ansichten.slotsDerLehrkraft(sprechtag, conrad)).hasSize(1);
    assertThat(ansichten.slotsDerLehrkraft(SprechtagId.neu(), berg)).isEmpty();
  }

  private void insertLehrkraft(LehrkraftId id, String vorname, String nachname, String kuerzel) {
    jdbc.update(
        "insert into lehrer (id, vorname, nachname, kuerzel) values (?, ?, ?, ?)",
        id.wert(),
        vorname,
        nachname,
        kuerzel);
  }

  private TerminId termin(LehrkraftId lehrkraft, int minuteVersatz, boolean entfaellt) {
    TerminId id = TerminId.neu();
    LocalDateTime start = BEGINN.plusMinutes(minuteVersatz);
    jdbc.update(
        "insert into termin (id, sprechtag_id, lehrer_id, startzeit, endzeit, verfuegbarkeit,"
            + " version) values (?, ?, ?, ?, ?, ?, 0)",
        id.wert(),
        sprechtag.wert(),
        lehrkraft.wert(),
        start,
        start.plusMinutes(15),
        entfaellt ? "ENTFAELLT" : "VERFUEGBAR");
    return id;
  }

  private void buche(
      TerminId termin, String schueler, String elternName, String elternEmail, String status) {
    jdbc.update(
        "insert into buchungen (id, termin_id, lehrauftrag_id, lehrkraft_id, lehrkraft_name,"
            + " lehrkraft_kuerzel, klasse_name, fach_name, eltern_name, schueler_name,"
            + " eltern_email, status, erstellt_am) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        UUID.randomUUID(),
        termin.wert(),
        lehrauftrag,
        berg.wert(),
        "Anna Berg",
        "BER",
        "5a",
        "Deutsch",
        elternName,
        schueler,
        elternEmail,
        status,
        LocalDateTime.now());
  }
}
