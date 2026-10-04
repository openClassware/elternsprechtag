package de.openclassware.elternsprechtag.sprechtag.adapter.out.mail;

import de.openclassware.elternsprechtag.sprechtag.application.port.out.BuchungsAnsichten.BelegZeile;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Wer eine Nachricht bekommt: je Sprechtag ein Kind an einer Adresse (ADR 0007). Die Buchungen
 * eines Kindes bei mehreren Lehrkräften teilen sich eine Nachricht; Geschwister und Familien an
 * derselben Adresse — etwa der Stellvertreteradresse der Schule — bekommen je eine eigene.
 *
 * <p>Absage, Erinnerung und Ausfall gruppieren über diesen einen Schlüssel, damit ein späterer
 * Familien- oder Vorgangsbegriff ihn an genau einer Stelle ersetzt.
 */
record Empfaenger(UUID sprechtagId, String elternEmail, String schuelerName, String klasse) {

  /** Gruppiert die Belege je Empfänger, in der Reihenfolge ihres ersten Auftretens. */
  static Map<Empfaenger, List<BelegZeile>> gruppiere(List<BelegZeile> zeilen) {
    Map<Empfaenger, List<BelegZeile>> gruppen = new LinkedHashMap<>();
    for (BelegZeile zeile : zeilen) {
      gruppen
          .computeIfAbsent(
              new Empfaenger(
                  zeile.sprechtagId(), zeile.elternEmail(), zeile.schuelerName(), zeile.klasse()),
              k -> new ArrayList<>())
          .add(zeile);
    }
    return gruppen;
  }
}
