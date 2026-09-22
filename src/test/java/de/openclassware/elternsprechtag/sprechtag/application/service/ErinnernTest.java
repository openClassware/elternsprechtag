package de.openclassware.elternsprechtag.sprechtag.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import de.openclassware.elternsprechtag.SprechtagKontextTestConfig;
import de.openclassware.elternsprechtag.sprechtag.AbstractServiceTest;
import de.openclassware.elternsprechtag.sprechtag.ServiceTest;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen.BuchungsAnfrage;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen.BuchungsWunsch;
import de.openclassware.elternsprechtag.sprechtag.domain.Buchung;
import de.openclassware.elternsprechtag.sprechtag.domain.ErinnerungsVorlauf;
import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

/**
 * Der tägliche Erinnerungs-Scheduler (Issue #107) gegen eine echte Datenbank: Kandidaten kommen
 * aus den Query-Ports, verbindlich geprüft und markiert wird an jedem {@code Termin}-Aggregat. Die
 * Fälligkeitsregel selbst — „verfallen statt nachholen" — steht ohne Spring in
 * {@code ErinnerungsVorlaufTest}.
 */
@ServiceTest
@Import(SprechtagKontextTestConfig.class)
class ErinnernTest extends AbstractServiceTest {

  private record Fixture(Sprechtag sprechtag, UUID lehrauftrag) {}

  /** Ein veröffentlichter Sprechtag am gewünschten Datum mit einem materialisierten Slot. */
  private Fixture veroeffentlichterSprechtag(LocalDate datum, ErinnerungsVorlauf vorlauf) {
    UUID klasse = persistKlasse("5a");
    UUID lehrkraft = persistLehrkraft("Anna", "Berg", "BER");
    UUID lehrauftrag = persistLehrauftrag(lehrkraft, klasse, persistFach("Deutsch", "D"));
    Sprechtag sprechtag =
        persistSprechtag(
            "Frühling",
            null,
            datum,
            LocalTime.of(14, 0),
            LocalTime.of(15, 0),
            15,
            SprechtagStatus.ENTWURF,
            vorlauf,
            klasse);
    veroeffentlichen.veroeffentliche(sprechtag.id().wert());
    return new Fixture(sprechtag, lehrauftrag);
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
  void erinnere_markiertDieFaelligeBuchungUndZaehltSie() {
    Fixture f = veroeffentlichterSprechtag(LocalDate.now().plusDays(1), ErinnerungsVorlauf.EIN_TAG);
    buche(f.lehrauftrag(), alleTermine().get(0));

    int anzahl = erinnern.erinnere();

    assertThat(anzahl).isEqualTo(1);
    assertThat(alleBuchungen())
        .singleElement()
        .extracting(Buchung::erinnerungVersendetAm)
        .satisfies(zeitpunkt -> assertThat(zeitpunkt).isPresent());
  }

  @Test
  void erinnere_zweitesMal_erinnertNichtNochEinmal() {
    Fixture f = veroeffentlichterSprechtag(LocalDate.now().plusDays(1), ErinnerungsVorlauf.EIN_TAG);
    buche(f.lehrauftrag(), alleTermine().get(0));
    erinnern.erinnere();

    int anzahl = erinnern.erinnere();

    assertThat(anzahl).isZero();
  }

  @Test
  void erinnere_vorlaufNochNichtFaellig_erinnertNicht() {
    // Drei Tage Vorlauf, aber der Sprechtag ist erst in zwei Tagen — noch nicht dran.
    Fixture f = veroeffentlichterSprechtag(LocalDate.now().plusDays(2), ErinnerungsVorlauf.DREI_TAGE);
    buche(f.lehrauftrag(), alleTermine().get(0));

    int anzahl = erinnern.erinnere();

    assertThat(anzahl).isZero();
    assertThat(alleBuchungen()).singleElement().satisfies(b -> assertThat(b.erinnerungVersendetAm()).isEmpty());
  }

  @Test
  void erinnere_vorlaufBereitsVerstrichen_holtNichtNach() {
    // "Verfallen statt nachholen": Der fällige Tag liegt in der Vergangenheit — ein ausgefallener
    // Lauf wird nicht nachgeholt.
    Fixture f = veroeffentlichterSprechtag(LocalDate.now(), ErinnerungsVorlauf.EIN_TAG);
    buche(f.lehrauftrag(), alleTermine().get(0));

    int anzahl = erinnern.erinnere();

    assertThat(anzahl).isZero();
  }

  @Test
  void erinnere_ohneErinnerungsVorlauf_erinnertNicht() {
    Fixture f = veroeffentlichterSprechtag(LocalDate.now().plusDays(1), ErinnerungsVorlauf.KEINE);
    buche(f.lehrauftrag(), alleTermine().get(0));

    int anzahl = erinnern.erinnere();

    assertThat(anzahl).isZero();
  }

  @Test
  void erinnere_stornierteBuchung_erinnertNicht() {
    Fixture f = veroeffentlichterSprechtag(LocalDate.now().plusDays(1), ErinnerungsVorlauf.EIN_TAG);
    UUID buchung = buche(f.lehrauftrag(), alleTermine().get(0));
    stornieren.storniere(buchung);

    int anzahl = erinnern.erinnere();

    assertThat(anzahl).isZero();
  }

  @Test
  void erinnere_abgesagterSprechtag_erinnertNicht() {
    Fixture f = veroeffentlichterSprechtag(LocalDate.now().plusDays(1), ErinnerungsVorlauf.EIN_TAG);
    buche(f.lehrauftrag(), alleTermine().get(0));
    absagen.sageAb(f.sprechtag().id().wert());

    int anzahl = erinnern.erinnere();

    assertThat(anzahl).isZero();
  }

  @Test
  void erinnere_ohneFaelligeBuchung_bleibtOhneEffekt() {
    veroeffentlichterSprechtag(LocalDate.now().plusDays(1), ErinnerungsVorlauf.EIN_TAG);
    // Kein Buchen — der freie Slot ist kein Kandidat.

    assertThat(erinnern.erinnere()).isZero();
  }
}
