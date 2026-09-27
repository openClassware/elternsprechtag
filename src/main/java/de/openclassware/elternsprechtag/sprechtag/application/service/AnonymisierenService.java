package de.openclassware.elternsprechtag.sprechtag.application.service;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.Anonymisieren;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.SprechtagAnsichten;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Sprechtage;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.TerminAnsichten;
import de.openclassware.elternsprechtag.sprechtag.domain.Aufbewahrungsfrist;
import de.openclassware.elternsprechtag.sprechtag.domain.Pseudonymisierung;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import de.openclassware.elternsprechtag.sprechtag.domain.TerminId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

/**
 * Der tägliche Anonymisierungs-Lauf (Issue #126). Zweistufig wie jeder Lauf im Projekt:
 * {@link SprechtagAnsichten} und {@link TerminAnsichten} liefern die Kandidaten (dürfen veraltet
 * sein), verbindlich entschieden wird am Aggregat.
 *
 * <p>Je Sprechtag werden <em>zuerst</em> seine Termine anonymisiert und gespeichert,
 * <em>danach</em> bekommt er den Vermerk {@code anonymisiertAm}. Scheitert ein Termin, bleibt der
 * Vermerk aus, und der nächste Lauf wiederholt den ganzen Sprechtag — bereits ersetzte Pseudonyme
 * werden dabei nur noch einmal überschrieben. Der Lauf ist damit idempotent, ohne dass eine
 * Transaktion mehr als ein Aggregat umfasst ({@link AnonymisierungsSchrittService}).
 *
 * <p>Frist und Ersatz-E-Mail sind Betriebseinstellung ({@code elternsprechtag.aufbewahrungsfrist-tage}
 * über {@link AufbewahrungsfristConfig}, die auch die Sprechtag-Liste nutzt;
 * {@code elternsprechtag.anonymisierung-email} über {@link Pseudonymgeber}). Die Frist zählt ab der
 * Endzeit des Sprechtags, nicht ab dem Abschluss (#124).
 */
@Service
@Slf4j
@RequiredArgsConstructor
class AnonymisierenService implements Anonymisieren {

  private final SprechtagAnsichten sprechtagAnsichten;
  private final TerminAnsichten terminAnsichten;
  private final Sprechtage sprechtage;
  private final AnonymisierungsSchrittService schritte;
  private final Aufbewahrungsfrist frist;
  private final Pseudonymgeber pseudonymgeber;

  @Override
  public int anonymisiere() {
    LocalDateTime jetzt = LocalDateTime.now();
    Pseudonymisierung pseudonyme = pseudonymgeber.neuerLauf();
    int anonymisiert = 0;
    LocalDate spaetestensAm = frist.spaetestesDatumAbgelaufenBis(jetzt.toLocalDate());
    for (SprechtagId id : sprechtagAnsichten.anonymisierungsKandidaten(spaetestensAm)) {
      // Ohne Vermerk bleibt der Sprechtag fällig — in beiden Fehlerfällen versucht der nächste Lauf
      // ihn erneut.
      try {
        if (anonymisiereSprechtag(id, pseudonyme, jetzt)) {
          anonymisiert++;
        }
      } catch (OptimisticLockingFailureException konflikt) {
        log.warn("Anonymisierung von Sprechtag {} übersprungen: gleichzeitig geändert", id.wert());
      } catch (RuntimeException e) {
        log.error("Anonymisierung von Sprechtag {} fehlgeschlagen", id.wert(), e);
      }
    }
    return anonymisiert;
  }

  private boolean anonymisiereSprechtag(
      SprechtagId id, Pseudonymisierung pseudonyme, LocalDateTime jetzt) {
    boolean faellig =
        sprechtage.lade(id).map(s -> s.istAnonymisierungFaellig(frist, jetzt)).orElse(false);
    if (!faellig) {
      return false;
    }
    for (TerminId termin : terminAnsichten.mitBuchungen(id)) {
      schritte.anonymisiereTermin(termin, pseudonyme, jetzt);
    }
    return schritte.vermerkeAnonymisierung(id, frist, jetzt);
  }
}
