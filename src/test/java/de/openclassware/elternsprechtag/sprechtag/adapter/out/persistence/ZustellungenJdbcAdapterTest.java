package de.openclassware.elternsprechtag.sprechtag.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import de.openclassware.elternsprechtag.sprechtag.application.port.out.BuchungsAnsichten.NichtErreichtZeile;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungId;
import de.openclassware.elternsprechtag.sprechtag.domain.Buchungsziel;
import de.openclassware.elternsprechtag.sprechtag.domain.Familie;
import de.openclassware.elternsprechtag.sprechtag.domain.LehrauftragId;
import de.openclassware.elternsprechtag.sprechtag.domain.LehrkraftId;
import de.openclassware.elternsprechtag.sprechtag.domain.Mailart;
import de.openclassware.elternsprechtag.sprechtag.domain.Pseudonymisierung;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
import de.openclassware.elternsprechtag.sprechtag.domain.Zeitraum;
import de.openclassware.elternsprechtag.sprechtag.domain.Zustellergebnis;
import de.openclassware.elternsprechtag.sprechtag.domain.Zustellung;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
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
 * Schreib- und Leseseite der Zustellungen (Issue #110) gegen das echte Schema: das Upsert je Buchung
 * und Art, die Regel „nicht erreicht" und — der Grund, warum die Tabelle keinen Fremdschlüssel hat —
 * dass eine Zustellung das Neuschreiben der Buchungen beim Speichern eines Termins übersteht.
 */
@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@ActiveProfiles("test")
@Import({
  ZustellungenJdbcAdapter.class,
  BuchungsAnsichtenJdbcAdapter.class,
  TerminePersistenceAdapter.class
})
class ZustellungenJdbcAdapterTest {

  private static final LocalDateTime BEGINN = LocalDateTime.of(2026, 7, 20, 14, 0);
  private static final LocalDateTime VERSAND = LocalDateTime.of(2026, 7, 1, 9, 30);

  @Autowired private ZustellungenJdbcAdapter zustellungen;
  @Autowired private BuchungsAnsichtenJdbcAdapter ansichten;
  @Autowired private TerminePersistenceAdapter termine;
  @Autowired private JdbcTemplate jdbc;

  private SprechtagId sprechtag;
  private LehrkraftId lehrkraft;
  private LehrauftragId lehrauftrag;
  private int slot;

  /** Fremdschlüssel-Kulisse per SQL, wie in {@code TerminePersistenceAdapterTest}. */
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
        LocalTime.of(16, 0),
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

  /** Ein neuer Termin im nächsten freien Slot, gebucht von dieser Familie. */
  private Termin gebucht(String name) {
    return gebucht("Eltern " + name, "Kind " + name, name + "@example.com");
  }

  /** Wie {@link #gebucht(String)}, mit freier Adresse — für Geschwister an einer Adresse. */
  private Termin gebucht(String eltern, String kind, String email) {
    LocalDateTime start = BEGINN.plusMinutes(15L * slot++);
    Termin termin = Termin.neu(sprechtag, lehrkraft, new Zeitraum(start, start.plusMinutes(15)));
    termin.buche(
        new Familie(eltern, kind, email),
        new Buchungsziel(lehrauftrag, lehrkraft, "Anna Berg", "BER", "5a", "Deutsch"),
        null,
        start);
    termine.speichere(termin);
    return termin;
  }

  private static BuchungId buchungVon(Termin termin) {
    return termin.aktiveBuchung().orElseThrow().id();
  }

  private void vermerke(BuchungId buchung, Mailart art, Zustellergebnis ergebnis) {
    zustellungen.speichere(List.of(Zustellung.vermerke(buchung, art, ergebnis, VERSAND)));
  }

  @Test
  void fehlgeschlageneZustellung_anAktiverBuchung_istNichtErreicht() {
    BuchungId buchung = buchungVon(gebucht("mueller"));

    vermerke(buchung, Mailart.BESTAETIGUNG, Zustellergebnis.FEHLGESCHLAGEN);

    assertThat(ansichten.nichtErreicht(sprechtag))
        .containsExactly(
            new NichtErreichtZeile(
                Mailart.BESTAETIGUNG,
                VERSAND,
                "mueller@example.com",
                "Kind mueller",
                "5a",
                List.of("Eltern mueller")));
  }

  @Test
  void abgeschickteZustellung_istNichtNichtErreicht() {
    BuchungId buchung = buchungVon(gebucht("mueller"));

    vermerke(buchung, Mailart.BESTAETIGUNG, Zustellergebnis.ABGESCHICKT);

    assertThat(ansichten.nichtErreicht(sprechtag)).isEmpty();
    assertThat(ansichten.nichtErreichtJeSprechtag()).doesNotContainKey(sprechtag.wert());
  }

  @Test
  void neueNachrichtDerselbenArt_ersetztDieVorige() {
    BuchungId buchung = buchungVon(gebucht("mueller"));
    vermerke(buchung, Mailart.ERINNERUNG, Zustellergebnis.FEHLGESCHLAGEN);

    vermerke(buchung, Mailart.ERINNERUNG, Zustellergebnis.ABGESCHICKT);

    assertThat(ansichten.nichtErreicht(sprechtag)).isEmpty();
    assertThat(jdbc.queryForObject("select count(*) from zustellungen", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void verschiedeneArten_stehenNebeneinander() {
    BuchungId buchung = buchungVon(gebucht("mueller"));

    vermerke(buchung, Mailart.BESTAETIGUNG, Zustellergebnis.FEHLGESCHLAGEN);
    vermerke(buchung, Mailart.ERINNERUNG, Zustellergebnis.ABGESCHICKT);

    assertThat(ansichten.nichtErreicht(sprechtag))
        .extracting(NichtErreichtZeile::art)
        .containsExactly(Mailart.BESTAETIGUNG);
  }

  /** Der Grund für „kein Fremdschlüssel" in V12: Spring Data JDBC schreibt Buchungen neu. */
  @Test
  void zustellung_uebersteht_dasNeuschreibenDerBuchungen() {
    Termin termin = gebucht("mueller");
    BuchungId buchung = buchungVon(termin);
    vermerke(buchung, Mailart.BESTAETIGUNG, Zustellergebnis.FEHLGESCHLAGEN);

    Termin geladen = termine.lade(termin.id()).orElseThrow();
    geladen.erinnereBuchung(buchung, VERSAND.plusDays(1));
    termine.speichere(geladen);

    assertThat(ansichten.nichtErreicht(sprechtag)).hasSize(1);
  }

  @Test
  void storno_nimmtDieFehlgeschlageneBestaetigung_ausDerListe() {
    Termin termin = gebucht("mueller");
    BuchungId buchung = buchungVon(termin);
    vermerke(buchung, Mailart.BESTAETIGUNG, Zustellergebnis.FEHLGESCHLAGEN);

    Termin geladen = termine.lade(termin.id()).orElseThrow();
    geladen.storniere(buchung);
    termine.speichere(geladen);

    assertThat(ansichten.nichtErreicht(sprechtag)).isEmpty();
  }

  @Test
  void ausfallNachricht_bleibt_anDerEntfallenenBuchungStehen() {
    Termin termin = gebucht("mueller");
    BuchungId buchung = buchungVon(termin);
    Termin geladen = termine.lade(termin.id()).orElseThrow();
    geladen.lassEntfallen();
    termine.speichere(geladen);

    vermerke(buchung, Mailart.AUSFALL, Zustellergebnis.FEHLGESCHLAGEN);

    assertThat(ansichten.nichtErreicht(sprechtag))
        .extracting(NichtErreichtZeile::art, NichtErreichtZeile::elternEmail)
        .containsExactly(tuple(Mailart.AUSFALL, "mueller@example.com"));
  }

  @Test
  void anonymisierteBuchung_istNichtMehrNichtErreicht() {
    Termin termin = gebucht("mueller");
    BuchungId buchung = buchungVon(termin);
    vermerke(buchung, Mailart.ABSAGE, Zustellergebnis.FEHLGESCHLAGEN);

    Termin geladen = termine.lade(termin.id()).orElseThrow();
    geladen.anonymisiere(Pseudonymisierung.mitSeed("ab12", "anonym@schule.example"), VERSAND);
    termine.speichere(geladen);

    assertThat(ansichten.nichtErreicht(sprechtag)).isEmpty();
  }

  private void vermerkeAbsage(Termin... termine) {
    List<Zustellung> vermerke = new ArrayList<>();
    for (Termin termin : termine) {
      vermerke.add(
          Zustellung.vermerke(
              buchungVon(termin), Mailart.ABSAGE, Zustellergebnis.FEHLGESCHLAGEN, VERSAND));
    }
    zustellungen.speichere(vermerke);
  }

  @Test
  void zweiBuchungenEinesKindes_sindEineNachricht() {
    // Dasselbe Kind bei zwei Lehrkräften: eine Nachricht, die beide Buchungen trug.
    Termin erste = gebucht("mueller");
    Termin zweite = gebucht("mueller");
    Termin dritte = gebucht("schmidt");
    vermerkeAbsage(erste, zweite, dritte);

    assertThat(ansichten.nichtErreichtJeSprechtag()).containsEntry(sprechtag.wert(), 2);
    assertThat(ansichten.nichtErreicht(sprechtag))
        .extracting(NichtErreichtZeile::schuelerName)
        .containsExactly("Kind mueller", "Kind schmidt");
  }

  /** ADR 0007: Geschwister und Stellvertreterfamilien an einer Adresse sind je eine Nachricht. */
  @Test
  void zweiKinderAnEinerAdresse_sindZweiNachrichten() {
    Termin lena = gebucht("Eltern Müller", "Lena Müller", "sekretariat@schule.example");
    Termin ben = gebucht("Eltern Yilmaz", "Ben Yilmaz", "sekretariat@schule.example");
    vermerkeAbsage(lena, ben);

    assertThat(ansichten.nichtErreichtJeSprechtag()).containsEntry(sprechtag.wert(), 2);
    assertThat(ansichten.nichtErreicht(sprechtag))
        .extracting(NichtErreichtZeile::schuelerName, NichtErreichtZeile::elternNamen)
        .containsExactly(
            tuple("Ben Yilmaz", List.of("Eltern Yilmaz")),
            tuple("Lena Müller", List.of("Eltern Müller")));
  }

  @Test
  void abweichendeElternnamenDesselbenKindes_bleibenBeideSichtbar() {
    Termin erste = gebucht("Petra Müller", "Lena Müller", "mueller@example.com");
    Termin zweite = gebucht("P. Müller", "Lena Müller", "mueller@example.com");
    vermerkeAbsage(erste, zweite);

    assertThat(ansichten.nichtErreicht(sprechtag))
        .singleElement()
        .satisfies(
            zeile ->
                assertThat(zeile.elternNamen())
                    .containsExactlyInAnyOrder("P. Müller", "Petra Müller"));
  }
}
