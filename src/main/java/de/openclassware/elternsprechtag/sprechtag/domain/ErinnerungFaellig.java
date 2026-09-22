package de.openclassware.elternsprechtag.sprechtag.domain;

import java.util.List;

/**
 * Vorgangs-Ereignis eines Scheduler-Laufs (Issue #107): genau die Buchungen, deren Erinnerung in
 * diesem Lauf als versendet markiert wurde — über beliebig viele Sprechtage und Familien hinweg.
 * Trägt keinen {@link Anlass}: Die Erinnerung hat nur einen einzigen Text.
 *
 * <p>Nicht von einem Aggregat gemeldet, sondern vom Erinnerungs-Use-Case aus den einzelnen
 * {@code Termin.erinnereBuchung}-Aufrufen gebündelt — wie {@link BuchungenBestaetigt} bei einem
 * Buchungsvorgang, nur über den ganzen Lauf statt über einen einzelnen Vorgang.
 */
public record ErinnerungFaellig(List<BuchungId> buchungen) implements Ereignis {

  public ErinnerungFaellig {
    buchungen = List.copyOf(buchungen);
    if (buchungen.isEmpty()) {
      throw new IllegalArgumentException("Ein Lauf ohne erinnerte Buchung wird nicht gemeldet");
    }
  }
}
