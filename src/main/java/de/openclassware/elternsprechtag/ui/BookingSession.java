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
 * aktuell aufgeklappte Lehrkraft und je Lehrauftrag den gewählten Slot samt seiner Notiz — und
 * kapselt die Entscheidungslogik (Slot-Zustand inkl. Zeitkonflikt, Aufräumen nach Konflikt,
 * Anfrage-Bau).
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

  /**
   * Eine Wahl im Warenkorb: der Slot dieser Lehrkraft und die Notiz, die daran hängt
   * ({@code null}, solange keine geschrieben wurde). Beides sitzt in einem Wert, damit die Notiz
   * gar nicht erst getrennt von ihrer Wahl gepflegt — und dabei vergessen — werden kann.
   */
  private record Wahl(SlotOption slot, String notiz) {}

  /** Wahl je Lehrauftrag (Schlüssel = lehrauftragId), in Auswahl-Reihenfolge. */
  private final Map<UUID, Wahl> selection = new LinkedHashMap<>();

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

  /**
   * Slot der aktiven Lehrkraft wählen (ersetzt eine frühere Wahl für dieselbe Lehrkraft). Wer nur
   * die Uhrzeit korrigiert, behält seine Notiz.
   */
  void waehle(SlotOption slot) {
    UUID lehrauftragId = activeLehrkraft.lehrauftragId();
    Wahl bisher = selection.get(lehrauftragId);
    selection.put(lehrauftragId, new Wahl(slot, bisher == null ? null : bisher.notiz()));
  }

  /** Wahl der aktiven Lehrkraft aufheben. */
  void abwaehlen() {
    entferne(activeLehrkraft.lehrauftragId());
  }

  /** Wahl einer beliebigen Lehrkraft entfernen (z. B. aus der Zusammenfassung). */
  void entferne(UUID lehrauftragId) {
    selection.remove(lehrauftragId);
  }

  /**
   * Notiz der Lehrkraft setzen; leerer Text zählt als keine Notiz. Der Text wird hier bewusst
   * <b>nicht</b> beschnitten — der Aufruf kommt bei jedem Tastendruck, und ein Trim mittendrin
   * ließe Modell und Eingabefeld auseinanderlaufen. Beschnitten wird erst an der Grenze, in
   * {@link #toWuensche()}. Ohne Wahl gibt es nichts, woran die Notiz hängen könnte — der Aufruf
   * verpufft dann.
   */
  void setNotiz(UUID lehrauftragId, String text) {
    Wahl wahl = selection.get(lehrauftragId);
    if (wahl == null) {
      return;
    }
    selection.put(
        lehrauftragId, new Wahl(wahl.slot(), text == null || text.isBlank() ? null : text));
  }

  /** Notiz der Lehrkraft — nie {@code null}, damit der View sie direkt anzeigen kann. */
  String notiz(UUID lehrauftragId) {
    Wahl wahl = selection.get(lehrauftragId);
    return wahl == null || wahl.notiz() == null ? "" : wahl.notiz();
  }

  SlotOption gewaehlterSlot(UUID lehrauftragId) {
    Wahl wahl = selection.get(lehrauftragId);
    return wahl == null ? null : wahl.slot();
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
    SlotOption chosen = gewaehlterSlot(activeLehrkraft.lehrauftragId());
    if (chosen != null && chosen.terminId().equals(slot.terminId())) {
      return SlotState.GEWAEHLT;
    }
    // Konflikt: derselbe Zeitpunkt ist bereits für eine andere Lehrkraft gewählt.
    for (Map.Entry<UUID, Wahl> entry : selection.entrySet()) {
      if (!entry.getKey().equals(activeLehrkraft.lehrauftragId())
          && entry.getValue().slot().zeit().equals(slot.zeit())) {
        return SlotState.KONFLIKT;
      }
    }
    return SlotState.FREI;
  }

  /**
   * Gewählte Slots als Buchungswünsche (Lehrauftrag → Termin → Notiz). Jeder Wunsch trägt die
   * Notiz seiner Lehrkraft, hier beschnitten; ohne Text bleibt sie {@code null}.
   */
  List<BuchungsWunsch> toWuensche() {
    return selection.entrySet().stream()
        .map(
            entry ->
                new BuchungsWunsch(
                    entry.getKey(),
                    entry.getValue().slot().terminId(),
                    beschnitten(entry.getValue().notiz())))
        .toList();
  }

  private static String beschnitten(String notiz) {
    return notiz == null ? null : notiz.trim();
  }

  /** Wirft ungültig gewordene Wahlen — samt ihrer Notizen — aus dem Warenkorb. */
  private void pruneUngueltige() {
    selection.entrySet().removeIf(entry -> !istBuchbar(entry.getKey(), entry.getValue()));
  }

  /** Steht die gewählte Lehrkraft noch in den Optionen, und ist ihr Termin noch frei? */
  private boolean istBuchbar(UUID lehrauftragId, Wahl wahl) {
    LehrkraftOption lehrkraft = findLehrkraft(lehrauftragId);
    return lehrkraft != null
        && lehrkraft.slots().stream()
            .anyMatch(slot -> slot.terminId().equals(wahl.slot().terminId()) && !slot.belegt());
  }

  private LehrkraftOption findLehrkraft(UUID lehrauftragId) {
    return lehrkraftOptionen.stream()
        .filter(lehrkraft -> lehrkraft.lehrauftragId().equals(lehrauftragId))
        .findFirst()
        .orElse(null);
  }
}
