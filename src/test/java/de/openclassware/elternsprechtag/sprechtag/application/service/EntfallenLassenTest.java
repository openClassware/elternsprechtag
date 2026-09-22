package de.openclassware.elternsprechtag.sprechtag.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.openclassware.elternsprechtag.SprechtagKontextTestConfig;
import de.openclassware.elternsprechtag.sprechtag.AbstractServiceTest;
import de.openclassware.elternsprechtag.sprechtag.ServiceTest;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen.BuchungsAnfrage;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen.BuchungsWunsch;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.EntfallenLassen.Ausfallergebnis;
import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagNichtVeroeffentlichtException;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
import de.openclassware.elternsprechtag.sprechtag.domain.TerminId;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

/**
 * Die Sammelaktion „Lehrkraft fällt aus" (#156) an ihrem Port, gegen eine echte Datenbank: was der
 * Organizer auslöst, was danach buchbar ist, und welche Zahlen er zurückbekommt.
 *
 * <p>Die Kaskade selbst — Buchung storniert, Termin auf {@code ENTFAELLT}, zweiter Aufruf ohne
 * Wirkung — steht ohne Spring und ohne Datenbank in {@code TerminTest} und wird hier bewusst
 * <b>nicht</b> noch einmal geprüft. Hier geht es um den Vorgang darum herum: Vorbedingung,
 * Transaktionsschnitt und Rückgabewert.
 */
@ServiceTest
@Import(SprechtagKontextTestConfig.class)
class EntfallenLassenTest extends AbstractServiceTest {

  private record Fixture(Sprechtag sprechtag, UUID lehrauftrag) {}

  /** Ein veröffentlichter Sprechtag mit vier materialisierten Slots einer Lehrkraft. */
  private Fixture veroeffentlichterSprechtag() {
    UUID klasse = persistKlasse("5a");
    UUID lehrkraft = persistLehrkraft("Anna", "Berg", "BER");
    UUID lehrauftrag = persistLehrauftrag(lehrkraft, klasse, persistFach("Deutsch", "D"));
    Sprechtag sprechtag =
        persistSprechtag(
            "Frühling",
            LocalDate.now().plusDays(7),
            LocalTime.of(14, 0),
            LocalTime.of(15, 0),
            15,
            SprechtagStatus.ENTWURF,
            klasse);
    veroeffentlichen.veroeffentliche(sprechtag.id().wert());
    return new Fixture(sprechtag, lehrauftrag);
  }

  private void buche(UUID lehrauftrag, Termin termin, String elternEmail) {
    buchen.buchen(
        new BuchungsAnfrage(
            "Eltern " + elternEmail,
            "Kind " + elternEmail,
            elternEmail,
            List.of(new BuchungsWunsch(lehrauftrag, termin.id().wert(), null))));
  }

  private Termin lade(UUID terminId) {
    return termine.lade(TerminId.von(terminId)).orElseThrow();
  }

  @Test
  void lassEntfallen_freierSlot_istDanachNichtMehrBuchbar() {
    veroeffentlichterSprechtag();
    UUID slot = alleTermine().get(0).id().wert();

    Ausfallergebnis ergebnis = entfallenLassen.lassEntfallen(List.of(slot));

    assertThat(ergebnis).isEqualTo(new Ausfallergebnis(1, 0));
    assertThat(lade(slot).istBuchbar()).isFalse();
  }

  @Test
  void lassEntfallen_nichtVeroeffentlichterSprechtag_wirdAbgewiesen() {
    Fixture f = veroeffentlichterSprechtag();
    List<UUID> auswahl = alleTermine().stream().limit(2).map(t -> t.id().wert()).toList();
    absagen.sageAb(f.sprechtag().id().wert());

    // Die Prüfung gehört in den Use Case: Die Auswertungs-Route ist per URL für jeden
    // Sprechtag-Status erreichbar.
    assertThatThrownBy(() -> entfallenLassen.lassEntfallen(auswahl))
        .isInstanceOf(SprechtagNichtVeroeffentlichtException.class);
    // Und sie greift, bevor der erste Termin geschrieben ist: Jede Einzeltransaktion committet für
    // sich, eine Abweisung mitten im Lauf ließe die ersten Stornos stehen — ohne dass die Familien
    // je davon erführen.
    assertThat(auswahl).allSatisfy(slot -> assertThat(lade(slot).istBuchbar()).isTrue());
  }

  @Test
  void lassEntfallen_bereitsEntfallenerTermin_wirdStillUebersprungen() {
    veroeffentlichterSprechtag();
    UUID slot = alleTermine().get(0).id().wert();
    entfallenLassen.lassEntfallen(List.of(slot));

    // Der zweite Klick aus einem alten Browser-Tab: kein Fehler, kein zweites Ereignis.
    Ausfallergebnis ergebnis = entfallenLassen.lassEntfallen(List.of(slot));

    assertThat(ergebnis).isEqualTo(new Ausfallergebnis(0, 0));
  }

  @Test
  void lassEntfallen_zaehltNurDasTatsaechlichGeschehene() {
    veroeffentlichterSprechtag();
    List<Termin> slots = alleTermine();
    UUID schonEntfallen = slots.get(0).id().wert();
    UUID frisch = slots.get(1).id().wert();
    entfallenLassen.lassEntfallen(List.of(schonEntfallen));

    Ausfallergebnis ergebnis =
        entfallenLassen.lassEntfallen(List.of(schonEntfallen, frisch, UUID.randomUUID()));

    // Drei Ids in der Auswahl, ein tatsächlich entfallener Termin.
    assertThat(ergebnis).isEqualTo(new Ausfallergebnis(1, 0));
    assertThat(lade(frisch).istBuchbar()).isFalse();
  }

  @Test
  void lassEntfallen_zweiTermineDerselbenFamilie_zaehltEineAdresse() {
    Fixture f = veroeffentlichterSprechtag();
    List<Termin> slots = alleTermine();
    buche(f.lehrauftrag(), slots.get(0), "mueller@example.com");
    buche(f.lehrauftrag(), slots.get(1), "mueller@example.com");
    buche(f.lehrauftrag(), slots.get(2), "schmidt@example.com");

    Ausfallergebnis ergebnis =
        entfallenLassen.lassEntfallen(
            slots.stream().limit(3).map(t -> t.id().wert()).toList());

    // Bündelungsschlüssel ist die Adresse: Müller bekommt eine Nachricht, nicht zwei.
    assertThat(ergebnis).isEqualTo(new Ausfallergebnis(3, 2));
  }

  @Test
  void lassEntfallen_frischGebuchterSlot_entfaelltSamtSeinerBuchung() {
    Fixture f = veroeffentlichterSprechtag();
    // Der Dialog liest seine Auswahl, als der Slot noch frei ist …
    Termin slot = alleTermine().get(0);
    // … und erst danach bucht eine Familie hinein. Die Vorschauzahl im Dialog ist eine Schätzung
    // auf einem Read-Modell; verbindlich entschieden wird hier am Aggregat, ohne Rückfrage.
    buche(f.lehrauftrag(), slot, "spaet@example.com");

    Ausfallergebnis ergebnis = entfallenLassen.lassEntfallen(List.of(slot.id().wert()));

    // Eine betroffene Adresse, obwohl die Auswahl einen freien Slot meinte.
    assertThat(ergebnis).isEqualTo(new Ausfallergebnis(1, 1));
    assertThat(lade(slot.id().wert()).istBuchbar()).isFalse();
  }

  @Test
  void lassEntfallen_einScheiternderTermin_stopptDenLaufNicht() {
    veroeffentlichterSprechtag();
    List<Termin> slots = alleTermine();
    UUID kaputt = slots.get(0).id().wert();
    UUID gesund = slots.get(1).id().wert();
    // Ein Termin ohne Dauer ist keiner: Das Aggregat lässt sich aus dieser Zeile nicht mehr
    // zusammensetzen, das Laden scheitert. Steht stellvertretend für jeden echten Einzelfehler —
    // die Regel dahinter ist, dass er den Lauf nicht anhält.
    jdbc.update("update termin set endzeit = startzeit where id = ?", kaputt);

    Ausfallergebnis ergebnis = entfallenLassen.lassEntfallen(List.of(kaputt, gesund));

    assertThat(ergebnis).isEqualTo(new Ausfallergebnis(1, 0));
    assertThat(lade(gesund).istBuchbar()).isFalse();
  }
}
