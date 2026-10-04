package de.openclassware.elternsprechtag.sprechtag.domain;

import java.util.List;

/**
 * Vorgangs-Ereignis einer Ausfall-Sammelaktion (Issue #156): die Buchungen, die dabei mit-storniert
 * wurden — über beliebig viele Termine derselben Lehrkraft hinweg.
 *
 * <p>Nicht von einem Aggregat gemeldet, sondern vom {@code EntfallenLassen}-Use-Case aus den
 * einzelnen {@code Termin.lassEntfallen}-Aufrufen gebündelt — wie {@link ErinnerungFaellig} bei
 * einem Scheduler-Lauf, nur über eine Organizer-Sammelaktion statt über einen Zeittrigger.
 *
 * <p>{@code nachbuchbar} hält fest, ob der Sprechtag zum Zeitpunkt des Ausfalls noch Elternbuchungen
 * annahm ({@link Sprechtag#nimmtElternbuchungenAn}). Nur dann führt die Nachricht über den
 * Elternlink zurück in die Buchung (Issue #109); danach verspräche der Link, was die Seite nicht
 * hält. Entschieden wird hier, beim Schreiben — der Versand liest es nur ab.
 */
public record AusfallErfasst(List<BuchungId> buchungen, boolean nachbuchbar) implements Ereignis {

  public AusfallErfasst {
    buchungen = List.copyOf(buchungen);
    if (buchungen.isEmpty()) {
      throw new IllegalArgumentException("Ein Ausfall ohne stornierte Buchung wird nicht gemeldet");
    }
  }
}
