package de.openclassware.elternsprechtag.sprechtag.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.openclassware.elternsprechtag.SprechtagKontextTestConfig;
import de.openclassware.elternsprechtag.sprechtag.AbstractServiceTest;
import de.openclassware.elternsprechtag.sprechtag.ServiceTest;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten.LehrkraftPlan;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen.BuchungsAnfrage;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen.BuchungsWunsch;
import de.openclassware.elternsprechtag.sprechtag.domain.Buchung;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungNichtGefundenException;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungNochAktivException;
import de.openclassware.elternsprechtag.sprechtag.domain.Buchungsstatus;
import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

/**
 * Die Einzelaktion „Angaben entfernen" gegen eine echte Datenbank (Issue #129). Die Regelmatrix
 * selbst — wann eine Zusage unberührt bleibt, dass ein zweiter Aufruf nichts tut — steht ohne
 * Spring in {@code TerminTest}; hier geht es darum, dass der Use Case den Sprechtag-Status richtig
 * hineinreicht und das Ergebnis festgeschrieben wird.
 */
@ServiceTest
@Import(SprechtagKontextTestConfig.class)
class AngabenEntfernenTest extends AbstractServiceTest {

  private static final LocalDate DATUM = LocalDate.of(2099, 7, 20);

  private record Fixture(Sprechtag sprechtag, UUID lehrauftrag) {}

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
    return new Fixture(sprechtag, lehrauftrag);
  }

  private UUID buche(UUID lehrauftrag, Termin termin) {
    buchen.buchen(
        new BuchungsAnfrage(
            "Eltern Müller",
            "Lukas Müller",
            "eltern@example.com",
            List.of(new BuchungsWunsch(lehrauftrag, termin.id().wert(), "Anliegen"))));
    return termine.lade(termin.id()).orElseThrow().aktiveBuchung().orElseThrow().id().wert();
  }

  private Buchung geladen(Termin termin) {
    return termine.lade(termin.id()).orElseThrow().buchungen().getFirst();
  }

  @Test
  void entferne_stornierteBuchung_ersetztDieFamilie() {
    Fixture f = veroeffentlichterSprechtag();
    Termin termin = alleTermine().get(0);
    UUID buchung = buche(f.lehrauftrag(), termin);
    stornieren.storniere(buchung, false);

    angabenEntfernen.entferne(buchung);

    Buchung entfernt = geladen(termin);
    assertThat(entfernt.familie().elternName()).isNotEqualTo("Eltern Müller");
    assertThat(entfernt.familie().email()).isNotEqualTo("eltern@example.com");
    assertThat(entfernt.notiz()).isEmpty();
    assertThat(entfernt.status()).isEqualTo(Buchungsstatus.STORNIERT);
    assertThat(entfernt.anonymisiertAm()).isPresent();
  }

  @Test
  void entferne_geltendeZusageAmVeroeffentlichtenSprechtag_wirdAbgewiesen() {
    Fixture f = veroeffentlichterSprechtag();
    Termin termin = alleTermine().get(0);
    UUID buchung = buche(f.lehrauftrag(), termin);

    // Am Presenter vorbei: Die Auswertungs-Route ist per URL in jedem Status erreichbar.
    assertThatThrownBy(() -> angabenEntfernen.entferne(buchung))
        .isInstanceOf(BuchungNochAktivException.class);

    assertThat(geladen(termin).familie().elternName()).isEqualTo("Eltern Müller");
  }

  @Test
  void entferne_geltendeZusageNachDemSprechtag_laesstDenTerminBelegt() {
    Fixture f = veroeffentlichterSprechtag();
    Termin termin = alleTermine().get(0);
    UUID buchung = buche(f.lehrauftrag(), termin);
    schliesseAb(f.sprechtag().id().wert());

    angabenEntfernen.entferne(buchung);

    Termin neu = termine.lade(termin.id()).orElseThrow();
    assertThat(neu.istBuchbar()).isFalse();
    Buchung entfernt = neu.aktiveBuchung().orElseThrow();
    assertThat(entfernt.familie().elternName()).isNotEqualTo("Eltern Müller");
    assertThat(entfernt.anonymisiertAm()).isPresent();
  }

  @Test
  void entferne_zweitesMal_istKeinFehlerUndBehaeltDenErstenZeitpunkt() {
    Fixture f = veroeffentlichterSprechtag();
    Termin termin = alleTermine().get(0);
    UUID buchung = buche(f.lehrauftrag(), termin);
    schliesseAb(f.sprechtag().id().wert());
    angabenEntfernen.entferne(buchung);
    LocalDateTime erster = geladen(termin).anonymisiertAm().orElseThrow();

    angabenEntfernen.entferne(buchung);

    assertThat(geladen(termin).anonymisiertAm()).contains(erster);
  }

  /**
   * Variante 2 aus #129: Stornierte Buchungen bleiben für die Auswertung erreichbar — gesondert, damit
   * Zähler und Terminplan weiter nur die geltenden Zusagen zeigen.
   */
  @Test
  void auswertung_fuehrtStornierteGesondertUndTraegtDenVermerk() {
    Fixture f = veroeffentlichterSprechtag();
    List<Termin> slots = alleTermine();
    UUID storniert = buche(f.lehrauftrag(), slots.get(0));
    UUID aktiv = buche(f.lehrauftrag(), slots.get(1));
    stornieren.storniere(storniert, true);

    LehrkraftPlan plan = auswerten.werteAus(f.sprechtag().id().wert()).orElseThrow().plaene().get(0);

    assertThat(plan.anzahl()).isEqualTo(1);
    assertThat(plan.zeilen()).singleElement().satisfies(zeile -> {
      assertThat(zeile.buchungId()).isEqualTo(aktiv);
      assertThat(zeile.storniert()).isFalse();
      assertThat(zeile.anonymisiertAm()).isNull();
    });
    assertThat(plan.stornierte()).singleElement().satisfies(zeile -> {
      assertThat(zeile.buchungId()).isEqualTo(storniert);
      assertThat(zeile.storniert()).isTrue();
      assertThat(zeile.anonymisiertAm()).isEqualTo(LocalDate.now());
    });
  }

  @Test
  void entferne_unbekannteBuchung_wirdAbgewiesen() {
    veroeffentlichterSprechtag();

    assertThatThrownBy(() -> angabenEntfernen.entferne(UUID.randomUUID()))
        .isInstanceOf(BuchungNichtGefundenException.class);
  }
}
