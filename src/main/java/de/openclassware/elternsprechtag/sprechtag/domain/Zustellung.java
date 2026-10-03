package de.openclassware.elternsprechtag.sprechtag.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Der Ausgang einer Nachricht an die Familie einer Buchung — je {@link Mailart} höchstens einer
 * (Issue #110). Eine neue Nachricht derselben Art ersetzt ihn.
 *
 * <p>Ein eigenes Aggregat und keine innere Entity der {@link Buchung}: Der Versand schreibt
 * asynchron nach dem Commit zurück, genau dann, wenn der Organizer am selben {@link Termin}
 * weiterarbeitet. Im Termin hübe jeder Vermerk dessen Version und kostete im Konfliktfall entweder
 * die Aktion des Organizers oder den Vermerk — für einen Stand, für den der Termin keine Invariante
 * schützt. Verbunden sind beide über die {@link BuchungId}.
 *
 * <p>Trägt keine personenbezogenen Angaben; die Anonymisierung muss es nicht anfassen.
 */
public final class Zustellung extends AggregateRoot {

  private final BuchungId buchung;
  private final Mailart art;
  private final Zustellergebnis ergebnis;
  private final LocalDateTime zeitpunkt;

  private Zustellung(
      BuchungId buchung, Mailart art, Zustellergebnis ergebnis, LocalDateTime zeitpunkt) {
    this.buchung = Objects.requireNonNull(buchung, "buchung");
    this.art = Objects.requireNonNull(art, "art");
    this.ergebnis = Objects.requireNonNull(ergebnis, "ergebnis");
    this.zeitpunkt = Objects.requireNonNull(zeitpunkt, "zeitpunkt");
  }

  /** Hält fest, wie die Nachricht dieser Art an die Familie dieser Buchung ausgegangen ist. */
  public static Zustellung vermerke(
      BuchungId buchung, Mailart art, Zustellergebnis ergebnis, LocalDateTime zeitpunkt) {
    return new Zustellung(buchung, art, ergebnis, zeitpunkt);
  }

  /** Ob die Familie diese Nachricht nicht bekommen hat — der Anlass für den Anruf. */
  public boolean istFehlgeschlagen() {
    return ergebnis == Zustellergebnis.FEHLGESCHLAGEN;
  }

  public BuchungId buchung() {
    return buchung;
  }

  public Mailart art() {
    return art;
  }

  public Zustellergebnis ergebnis() {
    return ergebnis;
  }

  public LocalDateTime zeitpunkt() {
    return zeitpunkt;
  }
}
