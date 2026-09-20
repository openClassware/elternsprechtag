package de.openclassware.elternsprechtag.sprechtag.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.openclassware.elternsprechtag.SprechtagKontextTestConfig;
import de.openclassware.elternsprechtag.sprechtag.AbstractServiceTest;
import de.openclassware.elternsprechtag.sprechtag.ServiceTest;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen.BuchungsAnfrage;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen.BuchungsWunsch;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Umbuchen.SlotOption;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Umbuchen.UmbuchAnfrage;
import de.openclassware.elternsprechtag.sprechtag.domain.Buchung;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungBereitsStorniertException;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungNichtGefundenException;
import de.openclassware.elternsprechtag.sprechtag.domain.Buchungsstatus;
import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagNichtVeroeffentlichtException;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
import de.openclassware.elternsprechtag.sprechtag.domain.TerminBelegtException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

/**
 * Das Organizer-Umbuchen gegen eine echte Datenbank: Storno und Neubuchung in einem Zug, gegen das
 * Buchungsziel der alten Buchung, nicht neu aus der Schulorganisation geholt (ADR 0005).
 */
@ServiceTest
@Import(SprechtagKontextTestConfig.class)
class UmbuchenTest extends AbstractServiceTest {

  private static final LocalDate DATUM = LocalDate.of(2026, 7, 20);

  private record Fixture(Sprechtag sprechtag, UUID klasse, UUID lehrauftrag, UUID lehrkraft) {}

  /** Ein veröffentlichter Sprechtag mit einer Lehrkraft und vier materialisierten Slots. */
  private Fixture veroeffentlichterSprechtag() {
    UUID klasse = persistKlasse("5a");
    UUID lehrkraft = persistLehrkraft("Anna", "Berg", "BER");
    UUID lehrauftrag = persistLehrauftrag(lehrkraft, klasse, persistFach("Deutsch", "D"));
    Sprechtag sprechtag =
        persistSprechtag(
            "Frühling",
            DATUM,
            LocalTime.of(14, 0),
            LocalTime.of(15, 0),
            15,
            SprechtagStatus.ENTWURF,
            klasse);
    veroeffentlichen.veroeffentliche(sprechtag.id().wert());
    return new Fixture(sprechtag, klasse, lehrauftrag, lehrkraft);
  }

  private UUID buche(UUID lehrauftrag, Termin termin, String eltern, String schueler, String notiz) {
    buchen.buchen(
        new BuchungsAnfrage(
            eltern,
            schueler,
            "eltern@example.com",
            List.of(new BuchungsWunsch(lehrauftrag, termin.id().wert(), notiz))));
    return termine.lade(termin.id()).orElseThrow().aktiveBuchung().orElseThrow().id().wert();
  }

  @Test
  void umbuche_verschiebtFamilieZielUndNotizAufDenNeuenSlot() {
    Fixture f = veroeffentlichterSprechtag();
    List<Termin> slots = alleTermine();
    UUID alte =
        buche(f.lehrauftrag(), slots.get(0), "Eltern Müller", "Lukas Müller", "Bitte pünktlich");

    UUID neue =
        umbuchen.umbuche(new UmbuchAnfrage(alte, slots.get(2).id().wert()));

    assertThat(neue).isNotEqualTo(alte);
    Buchung neuBuchung =
        termine.lade(slots.get(2).id()).orElseThrow().aktiveBuchung().orElseThrow();
    assertThat(neuBuchung.id().wert()).isEqualTo(neue);
    assertThat(neuBuchung.familie().elternName()).isEqualTo("Eltern Müller");
    assertThat(neuBuchung.familie().schuelerName()).isEqualTo("Lukas Müller");
    assertThat(neuBuchung.ziel().herkunft().wert()).isEqualTo(f.lehrauftrag());
    assertThat(neuBuchung.notiz())
        .hasValueSatisfying(n -> assertThat(n.text()).isEqualTo("Bitte pünktlich"));
  }

  @Test
  void umbuche_stelltDenAltenSlotWiederFreiUndStorniertDieAlteBuchung() {
    Fixture f = veroeffentlichterSprechtag();
    List<Termin> slots = alleTermine();
    UUID alte = buche(f.lehrauftrag(), slots.get(0), "Eltern Müller", "Lukas Müller", null);

    umbuchen.umbuche(new UmbuchAnfrage(alte, slots.get(1).id().wert()));

    Termin alterTermin = termine.lade(slots.get(0).id()).orElseThrow();
    assertThat(alterTermin.istBuchbar()).isTrue();
    assertThat(alterTermin.aktiveBuchung()).isEmpty();
    assertThat(alterTermin.buchungen())
        .filteredOn(b -> b.id().wert().equals(alte))
        .singleElement()
        .extracting(Buchung::status)
        .isEqualTo(Buchungsstatus.STORNIERT);
  }

  @Test
  void umbuche_lehrauftragInzwischenStillgelegt_gelingtWeiterhin() {
    Fixture f = veroeffentlichterSprechtag();
    List<Termin> slots = alleTermine();
    UUID alte = buche(f.lehrauftrag(), slots.get(0), "Eltern Müller", "Lukas Müller", null);
    stammdatenpflege.legeLehrauftragStill(f.lehrauftrag());

    UUID neue = umbuchen.umbuche(new UmbuchAnfrage(alte, slots.get(1).id().wert()));

    assertThat(neue).isNotEqualTo(alte);
  }

  @Test
  void umbuche_wunschslotZwischenzeitlichVergeben_scheitertUndLaesstAlteBuchungBestehen() {
    Fixture f = veroeffentlichterSprechtag();
    List<Termin> slots = alleTermine();
    UUID alte = buche(f.lehrauftrag(), slots.get(0), "Eltern Müller", "Lukas Müller", null);
    buche(f.lehrauftrag(), slots.get(1), "Eltern Schmidt", "Mia Schmidt", null);

    assertThatThrownBy(() -> umbuchen.umbuche(new UmbuchAnfrage(alte, slots.get(1).id().wert())))
        .isInstanceOf(TerminBelegtException.class);

    Termin alterTermin = termine.lade(slots.get(0).id()).orElseThrow();
    assertThat(alterTermin.aktiveBuchung()).isPresent();
    assertThat(alterTermin.aktiveBuchung().orElseThrow().id().wert()).isEqualTo(alte);
  }

  @Test
  void umbuche_amAbgeschlossenenSprechtag_wirdAbgewiesen() {
    Fixture f = veroeffentlichterSprechtag();
    List<Termin> slots = alleTermine();
    UUID alte = buche(f.lehrauftrag(), slots.get(0), "Eltern Müller", "Lukas Müller", null);
    abschliessen.schliesseAb(f.sprechtag().id().wert());

    assertThatThrownBy(() -> umbuchen.umbuche(new UmbuchAnfrage(alte, slots.get(1).id().wert())))
        .isInstanceOf(SprechtagNichtVeroeffentlichtException.class);
  }

  @Test
  void umbuche_unbekannteBuchung_wirdAbgewiesen() {
    veroeffentlichterSprechtag();

    assertThatThrownBy(
            () ->
                umbuchen.umbuche(
                    new UmbuchAnfrage(UUID.randomUUID(), alleTermine().get(0).id().wert())))
        .isInstanceOf(BuchungNichtGefundenException.class);
  }

  @Test
  void umbuche_bereitsUmgebuchteBuchungEinZweitesMal_scheitertMitBegruendung() {
    Fixture f = veroeffentlichterSprechtag();
    List<Termin> slots = alleTermine();
    UUID alte = buche(f.lehrauftrag(), slots.get(0), "Eltern Müller", "Lukas Müller", null);
    umbuchen.umbuche(new UmbuchAnfrage(alte, slots.get(1).id().wert()));

    // Der alte Browser-Tab des Organizers klickt dieselbe Zeile noch einmal.
    assertThatThrownBy(() -> umbuchen.umbuche(new UmbuchAnfrage(alte, slots.get(2).id().wert())))
        .isInstanceOf(BuchungBereitsStorniertException.class);
  }

  @Test
  void freieSlots_liefertNurBuchbareSlotsDerselbenLehrkraft() {
    Fixture f = veroeffentlichterSprechtag();
    List<Termin> slots = alleTermine();
    UUID alte = buche(f.lehrauftrag(), slots.get(0), "Eltern Müller", "Lukas Müller", null);
    buche(f.lehrauftrag(), slots.get(1), "Eltern Schmidt", "Mia Schmidt", null);

    List<SlotOption> optionen = umbuchen.freieSlots(alte);

    assertThat(optionen)
        .extracting(SlotOption::terminId)
        .containsExactly(slots.get(2).id().wert(), slots.get(3).id().wert());
  }
}
