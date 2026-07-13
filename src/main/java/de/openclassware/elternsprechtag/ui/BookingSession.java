package de.openclassware.elternsprechtag.ui;

import de.openclassware.elternsprechtag.services.BuchungService.BuchungsWunsch;
import de.openclassware.elternsprechtag.services.BuchungService.LehrkraftOption;
import de.openclassware.elternsprechtag.services.BuchungService.SlotOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Buchungs-„Warenkorb" der Eltern-View: hält die Lehrkraft-Optionen der gewählten Klasse, die
 * aktuell aufgeklappte Lehrkraft und den je Lehrauftrag gewählten Slot — und kapselt die
 * Entscheidungslogik (Slot-Zustand inkl. Zeitkonflikt, Aufräumen nach Konflikt, Anfrage-Bau).
 *
 * <p>Bewusst <b>Vaadin-frei</b>, damit die Logik ohne UI unit-testbar ist. Der View hält eine
 * Instanz, ruft die Methoden und rendert nur das Ergebnis.
 */
class BookingSession {

  /** Zustand eines Slots aus Sicht der aktuellen Auswahl. */
  enum SlotState {
    FREI,
    BELEGT,
    GEWAEHLT,
    KONFLIKT
  }

  /** Lehrkraft-Auswahl der gewählten Klasse; erst nach Klassenwahl befüllt. */
  private List<LehrkraftOption> lehrkraftOptionen = List.of();

  /** Aktuell aufgeklappte Lehrkraft in Schritt 3. */
  private LehrkraftOption activeLehrkraft;

  /** Gewählter Slot je Lehrauftrag (Schlüssel = lehrauftragId), in Auswahl-Reihenfolge. */
  private final Map<UUID, SlotOption> selection = new LinkedHashMap<>();

  /** Neue Klasse gewählt: Optionen ersetzen und Auswahl komplett verwerfen. */
  void reset(List<LehrkraftOption> optionen) {
    this.lehrkraftOptionen = optionen;
    this.activeLehrkraft = null;
    this.selection.clear();
  }

  /**
   * Nach einem Buchungskonflikt: Optionen neu setzen, zwischenzeitlich vergebene/verschwundene
   * Slots aus der Auswahl werfen und die aktive Lehrkraft neu auflösen (kann {@code null} werden).
   */
  void reload(List<LehrkraftOption> optionen) {
    this.lehrkraftOptionen = optionen;
    pruneUngueltige();
    if (activeLehrkraft != null) {
      activeLehrkraft = findLehrkraft(activeLehrkraft.lehrauftragId());
    }
  }

  List<LehrkraftOption> optionen() {
    return lehrkraftOptionen;
  }

  boolean hatOptionen() {
    return !lehrkraftOptionen.isEmpty();
  }

  void setActive(LehrkraftOption lehrkraft) {
    this.activeLehrkraft = lehrkraft;
  }

  LehrkraftOption active() {
    return activeLehrkraft;
  }

  boolean isActive(LehrkraftOption lehrkraft) {
    return activeLehrkraft != null
        && activeLehrkraft.lehrauftragId().equals(lehrkraft.lehrauftragId());
  }

  /** Slot der aktiven Lehrkraft wählen (ersetzt eine frühere Wahl für dieselbe Lehrkraft). */
  void waehle(SlotOption slot) {
    selection.put(activeLehrkraft.lehrauftragId(), slot);
  }

  /** Wahl der aktiven Lehrkraft aufheben. */
  void abwaehlen() {
    selection.remove(activeLehrkraft.lehrauftragId());
  }

  /** Wahl einer beliebigen Lehrkraft entfernen (z. B. aus der Zusammenfassung). */
  void entferne(UUID lehrauftragId) {
    selection.remove(lehrauftragId);
  }

  SlotOption gewaehlterSlot(UUID lehrauftragId) {
    return selection.get(lehrauftragId);
  }

  boolean istGewaehlt(UUID lehrauftragId) {
    return selection.containsKey(lehrauftragId);
  }

  boolean hatAuswahl() {
    return !selection.isEmpty();
  }

  int auswahlAnzahl() {
    return selection.size();
  }

  /** Zustand eines Slots relativ zur aktiven Lehrkraft und zur bisherigen Auswahl. */
  SlotState slotState(SlotOption slot) {
    if (slot.belegt()) {
      return SlotState.BELEGT;
    }
    SlotOption chosen = selection.get(activeLehrkraft.lehrauftragId());
    if (chosen != null && chosen.terminId().equals(slot.terminId())) {
      return SlotState.GEWAEHLT;
    }
    // Konflikt: derselbe Zeitpunkt ist bereits für eine andere Lehrkraft gewählt.
    for (Map.Entry<UUID, SlotOption> entry : selection.entrySet()) {
      if (!entry.getKey().equals(activeLehrkraft.lehrauftragId())
          && entry.getValue().zeit().equals(slot.zeit())) {
        return SlotState.KONFLIKT;
      }
    }
    return SlotState.FREI;
  }

  /** Gewählte Slots als Buchungswünsche (Lehrauftrag → Termin). */
  List<BuchungsWunsch> toWuensche() {
    return selection.entrySet().stream()
        .map(entry -> new BuchungsWunsch(entry.getKey(), entry.getValue().terminId()))
        .toList();
  }

  private void pruneUngueltige() {
    selection
        .entrySet()
        .removeIf(
            entry -> {
              LehrkraftOption lehrkraft = findLehrkraft(entry.getKey());
              if (lehrkraft == null) {
                return true;
              }
              return lehrkraft.slots().stream()
                  .noneMatch(
                      slot ->
                          slot.terminId().equals(entry.getValue().terminId()) && !slot.belegt());
            });
  }

  private LehrkraftOption findLehrkraft(UUID lehrauftragId) {
    return lehrkraftOptionen.stream()
        .filter(lehrkraft -> lehrkraft.lehrauftragId().equals(lehrauftragId))
        .findFirst()
        .orElse(null);
  }
}
