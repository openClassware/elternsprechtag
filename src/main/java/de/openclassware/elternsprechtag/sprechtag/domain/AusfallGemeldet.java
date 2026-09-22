package de.openclassware.elternsprechtag.sprechtag.domain;

import java.util.List;

/**
 * Vorgangs-Ereignis der Sammelaktion „Lehrkraft fällt aus" (#156): genau die Buchungen, die in
 * <em>einem</em> Ausfall-Vorgang storniert wurden — über beliebig viele Termine und Familien
 * hinweg.
 *
 * <p>Nicht von einem Aggregat gemeldet, sondern vom Use Case aus den einzelnen
 * {@code Termin.lassEntfallen}-Aufrufen gebündelt — wie {@link ErinnerungFaellig} bei einem
 * Scheduler-Lauf. Daran hängt der Versand: eine Nachricht je betroffener Eltern-Adresse, nicht eine
 * je Termin.
 *
 * <p>Die entfallenen Termine ohne Buchung stehen bewusst nicht darin: Sie betreffen niemanden, der
 * davon zu erfahren hätte.
 */
public record AusfallGemeldet(List<BuchungId> stornierteBuchungen) implements Ereignis {

  public AusfallGemeldet {
    stornierteBuchungen = List.copyOf(stornierteBuchungen);
    if (stornierteBuchungen.isEmpty()) {
      throw new IllegalArgumentException("Ein Ausfall ohne stornierte Buchung wird nicht gemeldet");
    }
  }
}
