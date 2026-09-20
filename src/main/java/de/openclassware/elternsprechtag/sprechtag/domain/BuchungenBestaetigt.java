package de.openclassware.elternsprechtag.sprechtag.domain;

import java.util.List;
import java.util.Objects;

/**
 * Vorgangs-Ereignis: <em>ein</em> Buchungsvorgang ist festgeschrieben. Trägt nur die Buchungen genau
 * dieses Vorgangs — daran hängt der Versand, und deshalb bekommt eine Familie mit vier Terminen eine
 * Mail und nicht vier (ADR 0003, ABDECKUNG.md Z. 261).
 *
 * <p>Nicht von einem Aggregat gemeldet, sondern vom Use Case aus den abgeholten
 * {@link BuchungAngelegt} gebündelt: Der Vorgang umfasst mehrere Termine, kein einzelnes Aggregat
 * kennt ihn.
 *
 * <p>{@link Anlass} sagt, welcher Vorgang das war — daran wählt die Mail-Seite ihren
 * Einstiegsbaustein, ohne die feinkörnigen Aggregat-Ereignisse dafür anzufassen.
 */
public record BuchungenBestaetigt(List<BuchungId> buchungen, Anlass anlass) implements Ereignis {

  public BuchungenBestaetigt {
    buchungen = List.copyOf(buchungen);
    if (buchungen.isEmpty()) {
      throw new IllegalArgumentException("Ein Vorgang ohne Buchung wird nicht bestätigt");
    }
    Objects.requireNonNull(anlass, "anlass");
  }
}
