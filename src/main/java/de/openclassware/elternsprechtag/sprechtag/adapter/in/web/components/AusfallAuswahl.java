package de.openclassware.elternsprechtag.sprechtag.adapter.in.web.components;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.EntfallenLassen.SlotZeile;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.EntfallenLassen.SlotZustand;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Auswahl-Modell des {@link AusfallDialog} (Sammelaktion „Lehrkraft fällt aus", Issue #156): hält
 * die Slot-Liste einer Lehrkraft und die aktuelle Auswahl, leitet daraus „N Termine, etwa M
 * Familien" lokal ab — keine Query je Checkbox-Klick.
 *
 * <p>Bewusst <b>Vaadin-frei</b>, damit die Auswahl-Logik ohne UI unit-testbar ist. Der Dialog hält
 * eine Instanz, ruft die Methoden und rendert nur das Ergebnis. Die Vorschauzahl der Familien ist
 * eine Schätzung auf dem Read-Modell, mit dem der Dialog geöffnet wurde; verbindlich entschieden
 * wird beim Bestätigen erneut am Aggregat.
 */
class AusfallAuswahl {

  private final List<SlotZeile> slots;
  private final Set<UUID> ausgewaehlt = new LinkedHashSet<>();

  AusfallAuswahl(List<SlotZeile> slots) {
    this.slots = List.copyOf(slots);
  }

  List<SlotZeile> slots() {
    return slots;
  }

  /** Bereits entfallene Slots sind sichtbar, aber nicht wählbar. */
  boolean istWaehlbar(SlotZeile slot) {
    return slot.zustand() != SlotZustand.ENTFALLEN;
  }

  boolean istGewaehlt(SlotZeile slot) {
    return ausgewaehlt.contains(slot.terminId());
  }

  void toggle(SlotZeile slot) {
    if (!istWaehlbar(slot)) {
      return;
    }
    if (!ausgewaehlt.remove(slot.terminId())) {
      ausgewaehlt.add(slot.terminId());
    }
  }

  /** Wählt alle wählbaren Slots — der häufige Ganztags-Ausfall kostet damit einen Klick. */
  void waehleAlle() {
    for (SlotZeile slot : slots) {
      if (istWaehlbar(slot)) {
        ausgewaehlt.add(slot.terminId());
      }
    }
  }

  void waehleKeine() {
    ausgewaehlt.clear();
  }

  boolean hatAuswahl() {
    return !ausgewaehlt.isEmpty();
  }

  int anzahlTermine() {
    return ausgewaehlt.size();
  }

  /**
   * Geschätzte Anzahl betroffener Familien: eine Familie mit zwei ausgewählten Slots zählt einmal.
   * Freie Slots ohne Familien-Schlüssel zählen nicht mit.
   */
  int anzahlFamilien() {
    Set<String> familien = new LinkedHashSet<>();
    for (SlotZeile slot : slots) {
      if (ausgewaehlt.contains(slot.terminId()) && slot.familienSchluessel() != null) {
        familien.add(slot.familienSchluessel());
      }
    }
    return familien.size();
  }

  List<UUID> gewaehlteTerminIds() {
    return List.copyOf(ausgewaehlt);
  }
}
