package de.openclassware.elternsprechtag.sprechtag.application.service;

import de.openclassware.elternsprechtag.sprechtag.application.port.out.Lehrauftraege;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Lehrauftraege.LehrauftragDaten;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Wie aus Lehraufträgen eine Lehrkraft-Liste wird — die eine Stelle für eine Regel, die zwei Use
 * Cases gleich brauchen: die Auswertung für den Terminplan, die Buchungsoptionen für die Auswahl.
 *
 * <p>Die Regel: je Lehrkraft ein Eintrag, und zwar der Lehrauftrag mit dem alphabetisch ersten Fach
 * (der Port liefert nach Fachname sortiert, das erste {@code putIfAbsent} gewinnt); anschließend
 * nach Nachname sortiert. Welcher Lehrauftrag den Eintrag stellt, ist für Kürzel und Name
 * gleichgültig — für die {@code lehrauftragId} als Buchungsziel dagegen nicht, und deshalb ist die
 * Auswahl festgelegt statt beliebig.
 */
final class Lehrkraftauswahl {

  private Lehrkraftauswahl() {}

  /**
   * Die Lehrkräfte dieser Klassen, dedupliziert und nach Nachname sortiert. Mehrere Klassen ergeben
   * eine Liste — eine Lehrkraft, die zwei teilnehmende Klassen unterrichtet, erscheint einmal.
   */
  static List<LehrauftragDaten> jeLehrkraft(Lehrauftraege lehrauftraege, List<UUID> klasseIds) {
    Map<UUID, LehrauftragDaten> auftragJeLehrkraft = new LinkedHashMap<>();
    for (UUID klasseId : klasseIds) {
      for (LehrauftragDaten auftrag : lehrauftraege.fuerKlasse(klasseId)) {
        auftragJeLehrkraft.putIfAbsent(auftrag.lehrkraft().wert(), auftrag);
      }
    }
    List<LehrauftragDaten> auftraege = new ArrayList<>(auftragJeLehrkraft.values());
    auftraege.sort(Comparator.comparing(LehrauftragDaten::nachname, String.CASE_INSENSITIVE_ORDER));
    return auftraege;
  }
}
