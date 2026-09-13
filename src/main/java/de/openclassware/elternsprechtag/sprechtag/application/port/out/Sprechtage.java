package de.openclassware.elternsprechtag.sprechtag.application.port.out;

import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Lesender Zugriff auf die Kopfdaten eines Sprechtags.
 *
 * <p>Übergangsweise: Solange {@code Sprechtag} noch eine JPA-Entity mit eigenem Service ist, holt
 * dieser Port nur, was Auswertung, Buchungsoptionen und die Bestätigungsmail von ihm brauchen. Mit
 * der Sprechtag-Scheibe wird er durch das Aggregat ersetzt.
 */
public interface Sprechtage {

  Optional<Kopf> ladeKopf(SprechtagId id);

  /** {@code ort} darf {@code null} sein; {@code schulkontakt} ist am Sprechtag Pflicht. */
  record Kopf(
      SprechtagId id,
      String titel,
      LocalDate datum,
      String ort,
      String schulkontakt,
      List<UUID> klasseIds) {}
}
