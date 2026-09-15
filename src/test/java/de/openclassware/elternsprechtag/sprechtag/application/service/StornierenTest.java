package de.openclassware.elternsprechtag.sprechtag.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.openclassware.elternsprechtag.SprechtagKontextTestConfig;
import de.openclassware.elternsprechtag.sprechtag.AbstractServiceTest;
import de.openclassware.elternsprechtag.sprechtag.ServiceTest;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten.BuchungsZeile;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten.LehrkraftPlan;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten.SprechtagAuswertung;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen.BuchungsAnfrage;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen.BuchungsWunsch;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchungsoptionen.SlotOption;
import de.openclassware.elternsprechtag.sprechtag.domain.Buchung;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungBereitsStorniertException;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungNichtGefundenException;
import de.openclassware.elternsprechtag.sprechtag.domain.Buchungsstatus;
import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagNichtVeroeffentlichtException;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

/**
 * Das Organizer-Storno gegen eine echte Datenbank: Was hier geprüft wird, hängt am Commit und an
 * den Vorbedingungen des Use Case — die Idempotenz des Aggregats selbst steht ohne Spring in
 * {@code TerminTest}.
 */
@ServiceTest
@Import(SprechtagKontextTestConfig.class)
class StornierenTest extends AbstractServiceTest {

  private static final LocalDate DATUM = LocalDate.of(2026, 7, 20);

  /** Ein veröffentlichter Sprechtag mit einer Lehrkraft und vier materialisierten Slots. */
  private record Fixture(Sprechtag sprechtag, UUID klasse, UUID lehrauftrag, UUID lehrkraft) {}

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

  private UUID buche(UUID lehrauftrag, Termin termin, String eltern, String schueler) {
    buchen.buchen(
        new BuchungsAnfrage(
            eltern,
            schueler,
            "eltern@example.com",
            List.of(new BuchungsWunsch(lehrauftrag, termin.id().wert(), null))));
    return termine.lade(termin.id()).orElseThrow().aktiveBuchung().orElseThrow().id().wert();
  }

  private SprechtagAuswertung auswertung(Fixture f) {
    return auswerten.werteAus(f.sprechtag().id().wert()).orElseThrow();
  }

  @Test
  void storniere_setztBuchungAufStorniertUndGibtDenTerminFrei() {
    Fixture f = veroeffentlichterSprechtag();
    Termin termin = alleTermine().get(0);
    UUID buchung = buche(f.lehrauftrag(), termin, "Eltern Müller", "Lukas Müller");

    stornieren.storniere(buchung);

    Termin neu = termine.lade(termin.id()).orElseThrow();
    assertThat(neu.istBuchbar()).isTrue();
    assertThat(neu.aktiveBuchung()).isEmpty();
    // Der Datensatz bleibt erhalten — er verschwindet nur aus dem Plan.
    assertThat(neu.buchungen())
        .singleElement()
        .extracting(Buchung::status)
        .isEqualTo(Buchungsstatus.STORNIERT);
  }

  @Test
  void storniere_unbekannteBuchung_wirdAbgewiesen() {
    veroeffentlichterSprechtag();

    assertThatThrownBy(() -> stornieren.storniere(UUID.randomUUID()))
        .isInstanceOf(BuchungNichtGefundenException.class);
  }

  @Test
  void storniere_zweitesMal_scheitertUndLaesstDenNeuGebuchtenTerminBelegt() {
    Fixture f = veroeffentlichterSprechtag();
    Termin termin = alleTermine().get(0);
    UUID erste = buche(f.lehrauftrag(), termin, "Eltern Müller", "Lukas Müller");
    stornieren.storniere(erste);
    // Der frei gewordene Slot wird sofort von einer anderen Familie belegt.
    UUID zweite =
        buche(f.lehrauftrag(), termine.lade(termin.id()).orElseThrow(), "Eltern Schmidt", "Mia");

    // Der alte Browser-Tab des Organizers klickt dieselbe Zeile noch einmal.
    assertThatThrownBy(() -> stornieren.storniere(erste))
        .isInstanceOf(BuchungBereitsStorniertException.class);

    Termin neu = termine.lade(termin.id()).orElseThrow();
    assertThat(neu.istBuchbar()).as("die frische Buchung darf nicht verloren gehen").isFalse();
    assertThat(neu.aktiveBuchung().orElseThrow().id().wert()).isEqualTo(zweite);
  }

  @Test
  void storniere_amAbgeschlossenenSprechtag_wirdAbgewiesen() {
    Fixture f = veroeffentlichterSprechtag();
    UUID buchung =
        buche(f.lehrauftrag(), alleTermine().get(0), "Eltern Müller", "Lukas Müller");
    abschliessen.schliesseAb(f.sprechtag().id().wert());

    // Am Service vorbei an der Oberfläche: Die Auswertungs-Route ist per URL erreichbar.
    assertThatThrownBy(() -> stornieren.storniere(buchung))
        .isInstanceOf(SprechtagNichtVeroeffentlichtException.class);

    assertThat(alleBuchungen()).singleElement().satisfies(b -> assertThat(b.istAktiv()).isTrue());
  }

  @Test
  void storniere_entferntDieZeileAusDerAuswertungUndSenktDenZaehler() {
    Fixture f = veroeffentlichterSprechtag();
    List<Termin> slots = alleTermine();
    UUID buchung = buche(f.lehrauftrag(), slots.get(0), "Eltern Müller", "Lukas Müller");
    buche(f.lehrauftrag(), slots.get(1), "Eltern Schmidt", "Mia Schmidt");

    stornieren.storniere(buchung);

    LehrkraftPlan plan = auswertung(f).plaene().get(0);
    assertThat(plan.anzahl()).isEqualTo(1);
    assertThat(plan.zeilen())
        .extracting(BuchungsZeile::schuelerName)
        .containsExactly("Mia Schmidt");
  }

  @Test
  void auswertung_traegtBuchungsIdUndSprechtagStatus() {
    Fixture f = veroeffentlichterSprechtag();
    UUID buchung =
        buche(f.lehrauftrag(), alleTermine().get(0), "Eltern Müller", "Lukas Müller");

    SprechtagAuswertung auswertung = auswertung(f);

    assertThat(auswertung.status()).isEqualTo(SprechtagStatus.VEROEFFENTLICHT);
    assertThat(auswertung.plaene().get(0).zeilen())
        .extracting(BuchungsZeile::buchungId)
        .containsExactly(buchung);
  }

  @Test
  void buchenStornierenErneutBuchen_derSlotStehtElternWiederOffen() {
    Fixture f = veroeffentlichterSprechtag();
    Termin termin = alleTermine().get(0);
    UUID buchung = buche(f.lehrauftrag(), termin, "Eltern Müller", "Lukas Müller");

    stornieren.storniere(buchung);

    // Die Eltern-Strecke meldet den Slot wieder als frei …
    assertThat(slot(f, termin).buchbar()).isTrue();
    // … und eine Buchung darauf gelingt.
    UUID zweite =
        buche(f.lehrauftrag(), termine.lade(termin.id()).orElseThrow(), "Eltern Schmidt", "Mia");
    assertThat(zweite).isNotEqualTo(buchung);
    assertThat(slot(f, termin).buchbar()).isFalse();
  }

  private SlotOption slot(Fixture f, Termin termin) {
    return buchungsoptionen
        .ladeLehrkraftOptionen(f.sprechtag().id().wert(), f.klasse())
        .get(0)
        .slots()
        .stream()
        .filter(kandidat -> kandidat.terminId().equals(termin.id().wert()))
        .findFirst()
        .orElseThrow();
  }
}
