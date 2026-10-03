package de.openclassware.elternsprechtag.sprechtag.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import de.openclassware.elternsprechtag.SprechtagKontextTestConfig;
import de.openclassware.elternsprechtag.sprechtag.AbstractServiceTest;
import de.openclassware.elternsprechtag.sprechtag.ServiceTest;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen.BuchungsAnfrage;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen.BuchungsWunsch;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.EntfallenLassen.Ergebnis;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.EntfallenLassen.SlotZeile;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.EntfallenLassen.SlotZustand;
import de.openclassware.elternsprechtag.sprechtag.domain.Anmeldefrist;
import de.openclassware.elternsprechtag.sprechtag.domain.AusfallErfasst;
import de.openclassware.elternsprechtag.sprechtag.domain.Buchung;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungId;
import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagNichtVeroeffentlichtException;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
import de.openclassware.elternsprechtag.sprechtag.domain.Verfuegbarkeit;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * Die Sammelaktion „Lehrkraft fällt aus" (Issue #156) gegen eine echte Datenbank: Das Angebot kommt
 * aus dem Lesewege-Query-Port, verbindlich geprüft und geändert wird an jedem {@code Termin}-
 * Aggregat. Die Kaskade selbst — Buchung mit-stornieren, beide Ereignisse melden — steht ohne
 * Spring in {@code TerminTest}.
 */
@ServiceTest
@Import(SprechtagKontextTestConfig.class)
@RecordApplicationEvents
class EntfallenLassenTest extends AbstractServiceTest {

  @Autowired private ApplicationEvents events;

  private record Fixture(UUID sprechtag, UUID lehrkraft, UUID lehrauftrag) {}

  private Fixture veroeffentlichterSprechtag(SprechtagStatus status) {
    UUID klasse = persistKlasse("5a");
    UUID lehrkraft = persistLehrkraft("Anna", "Berg", "BER");
    UUID lehrauftrag = persistLehrauftrag(lehrkraft, klasse, persistFach("Deutsch", "D"));
    var sprechtag =
        persistSprechtag(
            "Frühling",
            LocalDate.now().plusDays(7),
            LocalTime.of(14, 0),
            LocalTime.of(15, 0),
            15,
            SprechtagStatus.ENTWURF,
            klasse);
    veroeffentlichen.veroeffentliche(sprechtag.id().wert());
    if (status == SprechtagStatus.ABGESAGT) {
      absagen.sageAb(sprechtag.id().wert());
    }
    return new Fixture(sprechtag.id().wert(), lehrkraft, lehrauftrag);
  }

  private UUID buche(UUID lehrauftrag, Termin termin) {
    buchen.buchen(
        new BuchungsAnfrage(
            "Eltern Müller",
            "Lukas Müller",
            "mueller@example.com",
            List.of(new BuchungsWunsch(lehrauftrag, termin.id().wert(), null))));
    return termine.lade(termin.id()).orElseThrow().aktiveBuchung().orElseThrow().id().wert();
  }

  @Test
  void slots_zeigtFreieGebuchteUndEntfalleneSlots() {
    Fixture f = veroeffentlichterSprechtag(SprechtagStatus.VEROEFFENTLICHT);
    List<Termin> slots = alleTermine();
    buche(f.lehrauftrag(), slots.get(0));
    entfallenLassen.entfallenLassen(List.of(slots.get(1).id().wert()));

    List<SlotZeile> zeilen = entfallenLassen.angebot(f.sprechtag(), f.lehrkraft()).slots();

    assertThat(zeilen).hasSize(slots.size());
    assertThat(zeilen.get(0).zustand()).isEqualTo(SlotZustand.GEBUCHT);
    assertThat(zeilen.get(0).schuelerName()).isEqualTo("Lukas Müller");
    assertThat(zeilen.get(1).zustand()).isEqualTo(SlotZustand.ENTFALLEN);
    assertThat(zeilen.get(2).zustand()).isEqualTo(SlotZustand.FREI);
  }

  /** Lässt die Anmeldung des Sprechtags enden — eine Woche vorher mit der längsten Frist. */
  private void schliesseAnmeldung(UUID sprechtagId) {
    Sprechtag geladen = ladeSprechtag(sprechtagId);
    geladen.aendereAnmeldefrist(Anmeldefrist.vonTagen(Anmeldefrist.HOECHSTENS_TAGE));
    sprechtage.speichere(geladen);
  }

  // --- Nachbuchbar (Issue #109) -----------------------------------------------------------

  @Test
  void angebot_vorDemAnmeldeschluss_istNachbuchbar() {
    Fixture f = veroeffentlichterSprechtag(SprechtagStatus.VEROEFFENTLICHT);

    assertThat(entfallenLassen.angebot(f.sprechtag(), f.lehrkraft()).nachbuchbar()).isTrue();
  }

  @Test
  void angebot_nachDemAnmeldeschluss_istNichtNachbuchbar() {
    Fixture f = veroeffentlichterSprechtag(SprechtagStatus.VEROEFFENTLICHT);
    schliesseAnmeldung(f.sprechtag());

    assertThat(entfallenLassen.angebot(f.sprechtag(), f.lehrkraft()).nachbuchbar()).isFalse();
  }

  @Test
  void entfallenLassen_vorDemAnmeldeschluss_meldetDenAusfallAlsNachbuchbar() {
    Fixture f = veroeffentlichterSprechtag(SprechtagStatus.VEROEFFENTLICHT);
    Termin termin = alleTermine().get(0);
    UUID buchung = buche(f.lehrauftrag(), termin);

    entfallenLassen.entfallenLassen(List.of(termin.id().wert()));

    assertThat(events.stream(AusfallErfasst.class))
        .singleElement()
        .isEqualTo(new AusfallErfasst(List.of(BuchungId.von(buchung)), true));
  }

  @Test
  void entfallenLassen_nachDemAnmeldeschluss_meldetDenAusfallAlsNichtNachbuchbar() {
    Fixture f = veroeffentlichterSprechtag(SprechtagStatus.VEROEFFENTLICHT);
    Termin termin = alleTermine().get(0);
    UUID buchung = buche(f.lehrauftrag(), termin);
    schliesseAnmeldung(f.sprechtag());

    entfallenLassen.entfallenLassen(List.of(termin.id().wert()));

    assertThat(events.stream(AusfallErfasst.class))
        .singleElement()
        .isEqualTo(new AusfallErfasst(List.of(BuchungId.von(buchung)), false));
  }

  // --- Entfallen lassen -------------------------------------------------------------------

  @Test
  void entfallenLassen_ohneBuchung_setztVerfuegbarkeitUndZaehltKeineAdresse() {
    Fixture f = veroeffentlichterSprechtag(SprechtagStatus.VEROEFFENTLICHT);
    Termin termin = alleTermine().get(0);

    Ergebnis ergebnis = entfallenLassen.entfallenLassen(List.of(termin.id().wert()));

    assertThat(ergebnis).isEqualTo(new Ergebnis(1, 0));
    assertThat(termine.lade(termin.id()).orElseThrow().verfuegbarkeit())
        .isEqualTo(Verfuegbarkeit.ENTFAELLT);
  }

  @Test
  void entfallenLassen_mitAktiverBuchung_storniertUndZaehltEineAdresse() {
    Fixture f = veroeffentlichterSprechtag(SprechtagStatus.VEROEFFENTLICHT);
    Termin termin = alleTermine().get(0);
    buche(f.lehrauftrag(), termin);

    Ergebnis ergebnis = entfallenLassen.entfallenLassen(List.of(termin.id().wert()));

    assertThat(ergebnis).isEqualTo(new Ergebnis(1, 1));
    Termin geladen = termine.lade(termin.id()).orElseThrow();
    assertThat(geladen.verfuegbarkeit()).isEqualTo(Verfuegbarkeit.ENTFAELLT);
    assertThat(geladen.aktiveBuchung()).isEmpty();
    assertThat(geladen.buchungen()).singleElement().extracting(Buchung::istAktiv).isEqualTo(false);
  }

  @Test
  void entfallenLassen_zweiTermineDerselbenFamilie_zaehltEineAdresse() {
    Fixture f = veroeffentlichterSprechtag(SprechtagStatus.VEROEFFENTLICHT);
    List<Termin> slots = alleTermine();
    buche(f.lehrauftrag(), slots.get(0));
    buche(f.lehrauftrag(), slots.get(1));

    Ergebnis ergebnis =
        entfallenLassen.entfallenLassen(
            List.of(slots.get(0).id().wert(), slots.get(1).id().wert()));

    assertThat(ergebnis).isEqualTo(new Ergebnis(2, 1));
  }

  @Test
  void entfallenLassen_bereitsEntfallenerTermin_wirdUebersprungen() {
    Fixture f = veroeffentlichterSprechtag(SprechtagStatus.VEROEFFENTLICHT);
    Termin termin = alleTermine().get(0);
    entfallenLassen.entfallenLassen(List.of(termin.id().wert()));

    Ergebnis zweitesMal = entfallenLassen.entfallenLassen(List.of(termin.id().wert()));

    assertThat(zweitesMal).isEqualTo(new Ergebnis(0, 0));
  }

  @Test
  void entfallenLassen_gemischteAuswahl_ueberspringtNurDenBereitsEntfallenen() {
    Fixture f = veroeffentlichterSprechtag(SprechtagStatus.VEROEFFENTLICHT);
    List<Termin> slots = alleTermine();
    entfallenLassen.entfallenLassen(List.of(slots.get(0).id().wert()));

    Ergebnis ergebnis =
        entfallenLassen.entfallenLassen(
            List.of(slots.get(0).id().wert(), slots.get(1).id().wert()));

    assertThat(ergebnis).isEqualTo(new Ergebnis(1, 0));
  }

  @Test
  void entfallenLassen_nichtVeroeffentlichterSprechtag_wirdAbgelehnt() {
    Fixture f = veroeffentlichterSprechtag(SprechtagStatus.ABGESAGT);
    Termin termin = alleTermine().get(0);

    assertThatThrownBy(() -> entfallenLassen.entfallenLassen(List.of(termin.id().wert())))
        .isInstanceOf(SprechtagNichtVeroeffentlichtException.class);
    assertThat(termine.lade(termin.id()).orElseThrow().verfuegbarkeit())
        .isEqualTo(Verfuegbarkeit.VERFUEGBAR);
  }

  @Test
  void entfallenLassen_leereAuswahl_bleibtOhneEffekt() {
    assertThat(entfallenLassen.entfallenLassen(List.of())).isEqualTo(new Ergebnis(0, 0));
  }

  @Test
  void entfallenLassen_zwischenAuswahlUndBestaetigungFrischGebucht_entfaelltMitBuchung() {
    Fixture f = veroeffentlichterSprechtag(SprechtagStatus.VEROEFFENTLICHT);
    Termin termin = alleTermine().get(0);
    // Simuliert: Dialog wurde mit freiem Slot geöffnet, danach hat jemand anders gebucht.
    buche(f.lehrauftrag(), termin);

    Ergebnis ergebnis = entfallenLassen.entfallenLassen(List.of(termin.id().wert()));

    assertThat(ergebnis).isEqualTo(new Ergebnis(1, 1));
  }

  /**
   * Issue #129: Die mit-stornierte Buchung bleibt unter „Stornierte anzeigen" erreichbar — als
   * entfallen erkennbar, nicht als gewöhnliches Storno.
   */
  @Test
  void auswertung_fuehrtDieMitStornierteBuchungAlsEntfallen() {
    Fixture f = veroeffentlichterSprechtag(SprechtagStatus.VEROEFFENTLICHT);
    List<Termin> slots = alleTermine();
    buche(f.lehrauftrag(), slots.get(0));
    UUID storniert = buche(f.lehrauftrag(), slots.get(1));
    stornieren.storniere(storniert, false);
    entfallenLassen.entfallenLassen(List.of(slots.get(0).id().wert()));

    var plan = auswerten.werteAus(f.sprechtag()).orElseThrow().plaene().get(0);

    assertThat(plan.stornierte())
        .extracting(zeile -> zeile.storniert(), zeile -> zeile.entfallen())
        .containsExactly(tuple(true, true), tuple(true, false));
  }
}
