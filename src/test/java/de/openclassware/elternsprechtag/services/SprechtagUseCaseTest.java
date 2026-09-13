package de.openclassware.elternsprechtag.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.openclassware.elternsprechtag.SprechtagKontextTestConfig;
import de.openclassware.elternsprechtag.domain.Fach;
import de.openclassware.elternsprechtag.domain.Klasse;
import de.openclassware.elternsprechtag.domain.Lehrer;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen.BuchungsAnfrage;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen.BuchungsWunsch;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.SprechtagFormular;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Sprechtagsuebersicht.SprechtagZeile;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Sprechtagszugang.OeffentlicherSprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagAbgesagt;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagHatBuchungenException;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagVeroeffentlicht;
import de.openclassware.elternsprechtag.sprechtag.domain.StatusuebergangException;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
import de.openclassware.elternsprechtag.sprechtag.domain.ZeitstrukturEingefrorenException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * Die Sprechtag-Use-Cases gegen eine echte Datenbank — Nachfolger des alten
 * {@code SprechtagServiceTest}.
 *
 * <p>Was hier steht, braucht die Datenbank oder die Naht zwischen zwei Aggregaten: Materialisierung,
 * Read-Modelle, Ereignisse, das Verwerfen der Termine beim Zurücknehmen. Die Invarianten des
 * Sprechtags selbst stehen ohne Spring in {@code SprechtagTest} — dort, wo sie gelten.
 */
@ServiceTest
@Import(SprechtagKontextTestConfig.class)
@RecordApplicationEvents
class SprechtagUseCaseTest extends AbstractServiceTest {

  @Autowired private ApplicationEvents events;

  private static final LocalDate DATUM = LocalDate.of(2026, 7, 20);

  private SprechtagFormular formular(
      String titel, LocalTime beginn, LocalTime ende, int slot, Klasse... klassen) {
    SprechtagFormular formular = new SprechtagFormular();
    formular.setTitel(titel);
    formular.setDatum(DATUM);
    formular.setBeginn(beginn);
    formular.setEnde(ende);
    formular.setSlotInMinuten(slot);
    formular.setSchulkontakt(SCHULKONTAKT);
    formular.setKlasseIds(
        Arrays.stream(klassen)
            .map(Klasse::getId)
            .collect(Collectors.toCollection(LinkedHashSet::new)));
    return formular;
  }

  /** Eine Klasse mit einer Lehrkraft — so entstehen beim Veröffentlichen überhaupt Termine. */
  private Klasse klasseMitLehrkraft(String name) {
    Klasse klasse = persistKlasse(name);
    Lehrer lehrer = persistLehrer("Anna", "Berg", "BER");
    Fach fach = persistFach("Deutsch", "D");
    persistLehrauftrag(lehrer, klasse, fach);
    return klasse;
  }

  // --- Anlegen und Bearbeiten ---------------------------------------------------------------

  @Test
  void anlegen_erzeugtEinenEntwurfOhneTermine() {
    Klasse klasse = persistKlasse("5a");

    UUID id = anlegen.lege(formular("Frühling", LocalTime.of(14, 0), LocalTime.of(15, 0), 15, klasse));

    Sprechtag gespeichert = ladeSprechtag(id);
    assertThat(gespeichert.titel()).isEqualTo("Frühling");
    assertThat(gespeichert.status()).isEqualTo(SprechtagStatus.ENTWURF);
    assertThat(gespeichert.klassen()).hasSize(1);
    assertThat(alleTermine()).as("Entwurf materialisiert keine Termine").isEmpty();
  }

  @Test
  void bearbeiten_behaeltIdUndStatus() {
    Klasse klasse = persistKlasse("5a");
    UUID id = anlegen.lege(formular("Alt", LocalTime.of(14, 0), LocalTime.of(15, 0), 15, klasse));

    bearbeiten.bearbeite(id, formular("Neu", LocalTime.of(14, 0), LocalTime.of(15, 0), 15, klasse));

    assertThat(ladeSprechtag(id).titel()).isEqualTo("Neu");
    assertThat(ladeSprechtag(id).status()).isEqualTo(SprechtagStatus.ENTWURF);
    assertThat(jdbc.queryForObject("select count(*) from sprechtage", Long.class)).isEqualTo(1);
  }

  @Test
  void formularTraegtDenSchulkontaktUndDieKlassen() {
    Klasse a = persistKlasse("5a");
    Klasse b = persistKlasse("7a");
    Sprechtag sprechtag =
        persistSprechtag(
            "Frühling", DATUM, LocalTime.of(14, 0), LocalTime.of(15, 0), 15,
            SprechtagStatus.ENTWURF, a, b);

    SprechtagFormular geladen = bearbeiten.ladeFormular(sprechtag.id().wert()).orElseThrow();

    assertThat(geladen.getTitel()).isEqualTo("Frühling");
    assertThat(geladen.getSchulkontakt()).isEqualTo(SCHULKONTAKT);
    assertThat(geladen.getKlasseIds()).containsExactlyInAnyOrder(a.getId(), b.getId());
    assertThat(geladen.isZeitstrukturEingefroren()).as("ein Entwurf ist noch offen").isFalse();
  }

  /**
   * `ABDECKUNG.md` Z. 87 verlangt „gesperrt", nicht „abgewiesen": Das Formular sagt der Oberfläche,
   * dass sie diese Felder gar nicht erst zur Eingabe anbieten soll.
   */
  @Test
  void formularEinesVeroeffentlichten_meldetDieEingefroreneZeitstruktur() {
    Sprechtag sprechtag =
        persistSprechtag(
            "Frühling", DATUM, LocalTime.of(14, 0), LocalTime.of(15, 0), 15,
            SprechtagStatus.VEROEFFENTLICHT, persistKlasse("5a"));

    assertThat(bearbeiten.ladeFormular(sprechtag.id().wert()).orElseThrow().isZeitstrukturEingefroren())
        .isTrue();
  }

  @Test
  void anlegen_ohneSchulkontakt_wirdNichtGespeichert() {
    Klasse klasse = persistKlasse("5a");
    SprechtagFormular formular =
        formular("Entwurf", LocalTime.of(14, 0), LocalTime.of(15, 0), 15, klasse);
    formular.setSchulkontakt("   \n  ");

    assertThatThrownBy(() -> anlegen.lege(formular)).isInstanceOf(IllegalArgumentException.class);

    assertThat(jdbc.queryForObject("select count(*) from sprechtage", Long.class))
        .as("nichts angelegt")
        .isZero();
  }

  @Test
  void anlegen_ohneSlotdauer_wirdNichtGespeichert() {
    Klasse klasse = persistKlasse("5a");
    SprechtagFormular formular =
        formular("Entwurf", LocalTime.of(14, 0), LocalTime.of(15, 0), 15, klasse);
    formular.setSlotInMinuten(null);

    assertThatThrownBy(() -> anlegen.lege(formular)).isInstanceOf(IllegalArgumentException.class);
  }

  /** `ABDECKUNG.md` Z. 87 — die Sperre gilt auch über den Formularweg, nicht nur am Aggregat. */
  @Test
  void bearbeiten_zeitfensterEinesVeroeffentlichten_wirdAbgelehnt() {
    Klasse klasse = klasseMitLehrkraft("5a");
    UUID id = anlegen.lege(formular("Frühling", LocalTime.of(14, 0), LocalTime.of(15, 0), 15, klasse));
    veroeffentlichen.veroeffentliche(id);

    assertThatThrownBy(
            () ->
                bearbeiten.bearbeite(
                    id, formular("Frühling", LocalTime.of(16, 0), LocalTime.of(18, 0), 15, klasse)))
        .isInstanceOf(ZeitstrukturEingefrorenException.class);

    assertThat(ladeSprechtag(id).zeitfenster().beginn()).isEqualTo(LocalTime.of(14, 0));
  }

  /** `ABDECKUNG.md` Z. 90 — Titel und Ort bleiben auch danach änderbar. */
  @Test
  void bearbeiten_titelEinesVeroeffentlichten_bleibtErlaubt() {
    Klasse klasse = klasseMitLehrkraft("5a");
    UUID id = anlegen.lege(formular("Frühling", LocalTime.of(14, 0), LocalTime.of(15, 0), 15, klasse));
    veroeffentlichen.veroeffentliche(id);

    bearbeiten.bearbeite(
        id, formular("Frühling (Raumänderung)", LocalTime.of(14, 0), LocalTime.of(15, 0), 15, klasse));

    assertThat(ladeSprechtag(id).titel()).isEqualTo("Frühling (Raumänderung)");
  }

  /** `ABDECKUNG.md` Z. 93 — ein abgesagter Sprechtag lässt sich nicht über „Speichern" wiederbeleben. */
  @Test
  void bearbeiten_einesAbgesagten_wirdAbgelehnt() {
    Klasse klasse = persistKlasse("5a");
    Sprechtag sprechtag =
        persistSprechtag(
            "Frühling", DATUM, LocalTime.of(14, 0), LocalTime.of(15, 0), 15,
            SprechtagStatus.ABGESAGT, klasse);

    assertThatThrownBy(
            () ->
                bearbeiten.bearbeite(
                    sprechtag.id().wert(),
                    formular("Doch wieder da", LocalTime.of(14, 0), LocalTime.of(15, 0), 15, klasse)))
        .isInstanceOf(StatusuebergangException.class);

    assertThat(ladeSprechtag(sprechtag.id().wert()).status()).isEqualTo(SprechtagStatus.ABGESAGT);
  }

  // --- Veröffentlichen und Materialisieren ---------------------------------------------------

  @Test
  void veroeffentlichen_erzeugtJeLehrkraftUndSlotEinenTermin() {
    Klasse klasse = klasseMitLehrkraft("5a");
    Sprechtag sprechtag =
        persistSprechtag(
            "Frühling", DATUM, LocalTime.of(14, 0), LocalTime.of(15, 0), 15,
            SprechtagStatus.ENTWURF, klasse);

    // 14:00–15:00 in 15-Minuten-Slots => 4 Slots, eine Lehrkraft => 4 Termine.
    assertThat(veroeffentlichen.veroeffentliche(sprechtag.id().wert()).erzeugteTermine()).isEqualTo(4);
    assertThat(alleTermine()).hasSize(4).allSatisfy(t -> assertThat(t.istBuchbar()).isTrue());
    assertThat(events.stream(SprechtagVeroeffentlicht.class))
        .extracting(SprechtagVeroeffentlicht::sprechtag)
        .containsExactly(sprechtag.id());
  }

  @Test
  void veroeffentlichen_verwirftDenAngebrochenenRestSlot() {
    Klasse klasse = klasseMitLehrkraft("5a");
    Sprechtag sprechtag =
        persistSprechtag(
            "Kurz", DATUM, LocalTime.of(14, 0), LocalTime.of(14, 50), 20,
            SprechtagStatus.ENTWURF, klasse);

    // 14:00 und 14:20 passen; 14:40 + 20 = 15:00 liegt hinter 14:50.
    assertThat(veroeffentlichen.veroeffentliche(sprechtag.id().wert()).erzeugteTermine()).isEqualTo(2);
  }

  /** `ABDECKUNG.md` Z. 94 — keine der gewählten Klassen hat einen Lehrauftrag. */
  @Test
  void veroeffentlichen_ohneLehrauftrag_meldetKeineTermine() {
    Klasse klasse = persistKlasse("5a");
    Sprechtag sprechtag =
        persistSprechtag(
            "Frühling", DATUM, LocalTime.of(14, 0), LocalTime.of(15, 0), 15,
            SprechtagStatus.ENTWURF, klasse);

    assertThat(veroeffentlichen.veroeffentliche(sprechtag.id().wert()).ohneTermine()).isTrue();
    assertThat(ladeSprechtag(sprechtag.id().wert()).status())
        .isEqualTo(SprechtagStatus.VEROEFFENTLICHT);
  }

  @Test
  void materialisieren_istIdempotent() {
    Klasse klasse = klasseMitLehrkraft("5a");
    Sprechtag sprechtag =
        persistSprechtag(
            "Frühling", DATUM, LocalTime.of(14, 0), LocalTime.of(15, 0), 15,
            SprechtagStatus.ENTWURF, klasse);
    veroeffentlichen.veroeffentliche(sprechtag.id().wert());

    // Erneutes Speichern eines bereits veröffentlichten Sprechtags erzeugt keine neuen Termine.
    bearbeiten.bearbeite(
        sprechtag.id().wert(),
        formular("Frühling", LocalTime.of(14, 0), LocalTime.of(15, 0), 15, klasse));

    assertThat(alleTermine()).hasSize(4);
  }

  // --- Absagen und Abschließen ---------------------------------------------------------------

  @Test
  void absagen_meldetDasEreignis() {
    Sprechtag sprechtag =
        persistSprechtag(
            "Frühling", DATUM, LocalTime.of(14, 0), LocalTime.of(15, 0), 15,
            SprechtagStatus.VEROEFFENTLICHT, persistKlasse("5a"));

    absagen.sageAb(sprechtag.id().wert());

    assertThat(events.stream(SprechtagAbgesagt.class))
        .extracting(SprechtagAbgesagt::sprechtag)
        .containsExactly(sprechtag.id());
  }

  @Test
  void abschliessen_meldetKeineAbsage() {
    Sprechtag sprechtag =
        persistSprechtag(
            "Frühling", DATUM, LocalTime.of(14, 0), LocalTime.of(15, 0), 15,
            SprechtagStatus.VEROEFFENTLICHT, persistKlasse("5a"));

    abschliessen.schliesseAb(sprechtag.id().wert());

    assertThat(events.stream(SprechtagAbgesagt.class)).isEmpty();
    assertThat(ladeSprechtag(sprechtag.id().wert()).status())
        .isEqualTo(SprechtagStatus.ABGESCHLOSSEN);
  }

  @Test
  void abschliessen_einesEntwurfs_wirdAbgelehnt() {
    Sprechtag sprechtag =
        persistSprechtag(
            "Frühling", DATUM, LocalTime.of(14, 0), LocalTime.of(15, 0), 15,
            SprechtagStatus.ENTWURF, persistKlasse("5a"));

    assertThatThrownBy(() -> abschliessen.schliesseAb(sprechtag.id().wert()))
        .isInstanceOf(StatusuebergangException.class);
  }

  // --- Zurück auf Entwurf ---------------------------------------------------------------------

  /** `ABDECKUNG.md` Z. 91 — zu früh veröffentlicht, noch keine Buchung: erlaubt. */
  @Test
  void zurueckAufEntwurf_ohneBuchung_verwirftDieTermine() {
    Klasse klasse = klasseMitLehrkraft("5a");
    Sprechtag sprechtag =
        persistSprechtag(
            "Frühling", DATUM, LocalTime.of(14, 0), LocalTime.of(15, 0), 15,
            SprechtagStatus.ENTWURF, klasse);
    veroeffentlichen.veroeffentliche(sprechtag.id().wert());

    zurueckAufEntwurf.nimmZurueck(sprechtag.id().wert());

    assertThat(ladeSprechtag(sprechtag.id().wert()).status()).isEqualTo(SprechtagStatus.ENTWURF);
    assertThat(alleTermine()).as("die Termine haben ihre Grundlage verloren").isEmpty();
  }

  @Test
  void zurueckAufEntwurf_machtDieZeitstrukturWiederAenderbar() {
    Klasse klasse = klasseMitLehrkraft("5a");
    Sprechtag sprechtag =
        persistSprechtag(
            "Frühling", DATUM, LocalTime.of(14, 0), LocalTime.of(15, 0), 15,
            SprechtagStatus.ENTWURF, klasse);
    UUID id = sprechtag.id().wert();
    veroeffentlichen.veroeffentliche(id);
    zurueckAufEntwurf.nimmZurueck(id);

    bearbeiten.bearbeite(id, formular("Frühling", LocalTime.of(16, 0), LocalTime.of(17, 0), 30, klasse));
    veroeffentlichen.veroeffentliche(id);

    // Neue Zeitstruktur, neu gerechnet: 16:00–17:00 in 30-Minuten-Slots => 2 Termine.
    assertThat(alleTermine()).hasSize(2);
  }

  /** `ABDECKUNG.md` Z. 92 — zurück auf Entwurf, obwohl gebucht wurde: verhindert. */
  @Test
  void zurueckAufEntwurf_nachEinerBuchung_verweistAufDieAbsage() {
    Klasse klasse = klasseMitLehrkraft("5a");
    Sprechtag sprechtag =
        persistSprechtag(
            "Frühling", DATUM, LocalTime.of(14, 0), LocalTime.of(15, 0), 15,
            SprechtagStatus.ENTWURF, klasse);
    UUID id = sprechtag.id().wert();
    veroeffentlichen.veroeffentliche(id);
    bucheErstenTermin();

    assertThatThrownBy(() -> zurueckAufEntwurf.nimmZurueck(id))
        .isInstanceOf(SprechtagHatBuchungenException.class);

    assertThat(ladeSprechtag(id).status()).isEqualTo(SprechtagStatus.VEROEFFENTLICHT);
    assertThat(alleTermine()).as("nichts verworfen").hasSize(4);
  }

  /** Auch eine stornierte Buchung zählt: Benachrichtigt wurde trotzdem. */
  @Test
  void zurueckAufEntwurf_nachEinerStornierung_bleibtVerhindert() {
    Klasse klasse = klasseMitLehrkraft("5a");
    Sprechtag sprechtag =
        persistSprechtag(
            "Frühling", DATUM, LocalTime.of(14, 0), LocalTime.of(15, 0), 15,
            SprechtagStatus.ENTWURF, klasse);
    UUID id = sprechtag.id().wert();
    veroeffentlichen.veroeffentliche(id);
    bucheErstenTermin();
    storniere(buchung -> true);

    assertThatThrownBy(() -> zurueckAufEntwurf.nimmZurueck(id))
        .isInstanceOf(SprechtagHatBuchungenException.class);
  }

  // --- Duplizieren und Read-Modelle -----------------------------------------------------------

  @Test
  void duplizieren_erzeugtEinenEntwurfMitEigenemToken() {
    Klasse klasse = persistKlasse("5a");
    Sprechtag original =
        persistSprechtag(
            "Frühling", DATUM, LocalTime.of(14, 0), LocalTime.of(15, 0), 15,
            SprechtagStatus.VEROEFFENTLICHT, klasse);

    Sprechtag kopie = ladeSprechtag(duplizieren.dupliziere(original.id().wert()));

    assertThat(kopie.status()).isEqualTo(SprechtagStatus.ENTWURF);
    assertThat(kopie.accessToken()).isNotEqualTo(original.accessToken());
    assertThat(kopie.schulkontakt()).isEqualTo(original.schulkontakt());
    assertThat(kopie.klassen()).isEqualTo(original.klassen());
  }

  @Test
  void uebersicht_istNachDatumSortiertUndTraegtDieKlassennamen() {
    Klasse klasse = persistKlasse("5a");
    persistSprechtag(
        "Später", LocalDate.of(2026, 9, 1), LocalTime.of(14, 0), LocalTime.of(15, 0), 15,
        SprechtagStatus.ENTWURF, klasse);
    persistSprechtag(
        "Früher", LocalDate.of(2026, 3, 1), LocalTime.of(14, 0), LocalTime.of(15, 0), 15,
        SprechtagStatus.ENTWURF, klasse);

    List<SprechtagZeile> zeilen = sprechtagsuebersicht.alle();

    assertThat(zeilen).extracting(SprechtagZeile::titel).containsExactly("Früher", "Später");
    assertThat(zeilen.get(0).klassen()).containsExactly("5a");
  }

  @Test
  void zugang_ueberDasTokenLiefertDieWaehlbarenKlassen() {
    Klasse a = persistKlasse("5a");
    Klasse b = persistKlasse("7a");
    Sprechtag sprechtag =
        persistSprechtag(
            "Frühling", DATUM, LocalTime.of(14, 0), LocalTime.of(15, 0), 15,
            SprechtagStatus.VEROEFFENTLICHT, a, b);

    OeffentlicherSprechtag geoeffnet =
        sprechtagszugang.oeffne(sprechtag.accessToken().wert()).orElseThrow();

    assertThat(geoeffnet.buchbar()).isTrue();
    assertThat(geoeffnet.abgesagt()).isFalse();
    assertThat(geoeffnet.klassen()).extracting("name").containsExactly("5a", "7a");
  }

  @Test
  void zugang_einesAbgesagtenIstNichtBuchbar() {
    Sprechtag sprechtag =
        persistSprechtag(
            "Frühling", DATUM, LocalTime.of(14, 0), LocalTime.of(15, 0), 15,
            SprechtagStatus.ABGESAGT, persistKlasse("5a"));

    OeffentlicherSprechtag geoeffnet =
        sprechtagszugang.oeffne(sprechtag.accessToken().wert()).orElseThrow();

    assertThat(geoeffnet.buchbar()).isFalse();
    assertThat(geoeffnet.abgesagt()).isTrue();
  }

  @Test
  void zugang_mitUnbekanntemToken_liefertNichts() {
    assertThat(sprechtagszugang.oeffne(UUID.randomUUID().toString())).isEmpty();
    assertThat(sprechtagszugang.oeffne("  ")).isEmpty();
  }

  // --- Hilfen ---------------------------------------------------------------------------------

  private void bucheErstenTermin() {
    Termin termin = alleTermine().get(0);
    UUID lehrauftragId = jdbc.queryForObject("select id from lehrauftrag limit 1", UUID.class);
    buchen.buchen(
        new BuchungsAnfrage(
            "Elke Elternteil",
            "Karl Kind",
            "eltern@example.com",
            List.of(new BuchungsWunsch(lehrauftragId, termin.id().wert(), null))));
  }
}
