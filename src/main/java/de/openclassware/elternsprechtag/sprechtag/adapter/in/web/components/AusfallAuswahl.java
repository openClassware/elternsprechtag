package de.openclassware.elternsprechtag.sprechtag.adapter.in.web.components;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.EntfallenLassen.AusfallSlot;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.EntfallenLassen.Slotzustand;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Die Auswahl im Ausfall-Dialog: hält die Slot-Liste einer Lehrkraft und die angekreuzten Termine —
 * und leitet daraus ab, was die Vorschau nennt: „N Termine, etwa M Familien".
 *
 * <p>Bewusst <b>Vaadin-frei</b>, damit diese Entscheidungslogik ohne UI unit-testbar bleibt; der
 * Dialog hält eine Instanz, ruft die Methoden und rendert nur das Ergebnis (wie
 * {@code BookingSession} zum Eltern-View).
 *
 * <p>Der Punkt der Klasse ist das Wort <b>lokal</b> in der Spec: Die Zahlen entstehen aus den
 * Zeilen, die beim Öffnen einmal geladen wurden, nicht aus einer Query je Häkchen. Dafür trägt jeder
 * gebuchte Slot seinen Familien-Schlüssel mit — dieselbe Eltern-Adresse, nach der der Versand
 * bündelt. Deshalb ist die Zahl auch eine <em>Schätzung</em>: Das Read-Modell darf veraltet sein,
 * verbindlich entschieden wird am Aggregat.
 */
class AusfallAuswahl {

  private final List<AusfallSlot> slots;

  /** Die angekreuzten Termine. Nur wählbare kommen hinein — siehe {@link #waehle(UUID, boolean)}. */
  private final Set<UUID> gewaehlt = new HashSet<>();

  AusfallAuswahl(List<AusfallSlot> slots) {
    this.slots = List.copyOf(slots);
  }

  /** Alle Slots der Lehrkraft, chronologisch wie geliefert — auch die bereits entfallenen. */
  List<AusfallSlot> slots() {
    return slots;
  }

  /**
   * Bereits entfallene Slots sind sichtbar, aber nicht wählbar: Ein zweites Kreuz änderte nichts
   * (der Use Case überspringt sie still), und ein Gegenstück, das sie zurückholte, gibt es nicht.
   */
  boolean istWaehlbar(AusfallSlot slot) {
    return slot.zustand() != Slotzustand.ENTFALLEN;
  }

  boolean hatWaehlbare() {
    return slots.stream().anyMatch(this::istWaehlbar);
  }

  /**
   * Kreuzt einen Termin an oder ab. Unbekannte und nicht wählbare Ids verpuffen — die Prüfung liegt
   * hier und nicht in jeder aufrufenden Checkbox.
   */
  void waehle(UUID terminId, boolean an) {
    if (!an) {
      gewaehlt.remove(terminId);
      return;
    }
    if (slots.stream().anyMatch(slot -> slot.terminId().equals(terminId) && istWaehlbar(slot))) {
      gewaehlt.add(terminId);
    }
  }

  boolean istGewaehlt(UUID terminId) {
    return gewaehlt.contains(terminId);
  }

  /** „Alle auswählen" in einem Klick — der häufige Ganztagsfall — und derselbe Weg zurück. */
  void waehleAlle(boolean an) {
    gewaehlt.clear();
    if (an) {
      slots.stream().filter(this::istWaehlbar).map(AusfallSlot::terminId).forEach(gewaehlt::add);
    }
  }

  /**
   * Ob jeder wählbare Slot angekreuzt ist. Ohne wählbare Slots ist es {@code false}: Sonst stünde
   * der Knopf an einem Tag, an dem nichts mehr zu wählen ist, auf „alles gewählt".
   */
  boolean alleWaehlbarenGewaehlt() {
    return hatWaehlbare()
        && slots.stream().filter(this::istWaehlbar).allMatch(slot -> istGewaehlt(slot.terminId()));
  }

  boolean hatAuswahl() {
    return !gewaehlt.isEmpty();
  }

  /** Wie viele Termine gewählt sind — freie zählen mit, sie entfallen genauso. */
  int anzahlTermine() {
    return gewaehlt.size();
  }

  /**
   * Wie viele Familien die Auswahl <em>etwa</em> trifft: verschiedene Eltern-Adressen unter den
   * gewählten gebuchten Slots. Zwei Geschwister teilen sich die Adresse und zählen einmal — genau
   * so bündelt auch der Versand, der genau eine Nachricht je Adresse schickt.
   */
  int anzahlFamilien() {
    Set<String> adressen = new HashSet<>();
    for (AusfallSlot slot : slots) {
      if (istGewaehlt(slot.terminId()) && slot.familienSchluessel() != null) {
        adressen.add(slot.familienSchluessel());
      }
    }
    return adressen.size();
  }

  /**
   * Die Auswahl als Termin-Ids, in der chronologischen Reihenfolge der Slots statt in der der
   * Klicks: Was der Vorgang der Reihe nach abarbeitet und protokolliert, soll nicht davon abhängen,
   * in welcher Folge jemand geklickt hat.
   */
  List<UUID> terminIds() {
    return slots.stream().map(AusfallSlot::terminId).filter(this::istGewaehlt).toList();
  }
}
