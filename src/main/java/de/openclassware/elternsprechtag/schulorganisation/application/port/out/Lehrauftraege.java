package de.openclassware.elternsprechtag.schulorganisation.application.port.out;

import de.openclassware.elternsprechtag.schulorganisation.domain.Lehrauftrag;
import de.openclassware.elternsprechtag.schulorganisation.domain.LehrauftragId;
import java.util.Optional;

/**
 * Aggregat-Repository der Lehraufträge. Siehe {@link Lehrkraefte} für die gemeinsamen Zusagen.
 *
 * <p>Nicht zu verwechseln mit dem gleichnamigen Port des Sprechtag-Kontexts: Der liest, was dieser
 * Kontext über seinen {@code port/in} herausgibt. Hier wird geschrieben.
 */
public interface Lehrauftraege {

  Optional<Lehrauftrag> lade(LehrauftragId id);

  /**
   * Schreibt das Aggregat.
   *
   * @throws org.springframework.dao.DuplicateKeyException wenn es das Tripel (Lehrkraft, Klasse,
   *     Fach) unter den <b>aktiven</b> Lehraufträgen schon gibt — die Eindeutigkeit steht als
   *     Teil-Index in der Datenbank, nicht als Invariante im Aggregat. Stillgelegte Lehraufträge
   *     stehen ihr nicht im Weg: Dasselbe Tripel darf im nächsten Schuljahr wieder erteilt werden.
   */
  void speichere(Lehrauftrag lehrauftrag);
}
