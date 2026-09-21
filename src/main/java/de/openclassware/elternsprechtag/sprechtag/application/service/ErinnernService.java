package de.openclassware.elternsprechtag.sprechtag.application.service;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.Erinnern;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.BuchungsAnsichten;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Ereignisse;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.SprechtagAnsichten;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.SprechtagAnsichten.ErinnerungsKandidat;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungId;
import de.openclassware.elternsprechtag.sprechtag.domain.ErinnerungFaellig;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Der tägliche Erinnerungs-Scheduler (Issue #107): findet die heute fälligen Buchungen, markiert
 * sie über {@link ErinnerungsMarkierungService} als erinnert und bündelt sie zu <em>einem</em>
 * {@link ErinnerungFaellig} für den Mailversand.
 *
 * <p>Zweistufig wie jede Query-dann-Aggregat-Regel im Projekt: {@link SprechtagAnsichten} und
 * {@link BuchungsAnsichten} liefern die Kandidaten (dürfen veraltet sein), verbindlich geprüft und
 * geändert wird an jedem einzelnen {@code Termin}-Aggregat. Das Markieren selbst liegt in einer
 * eigenen Transaktion je Buchung ({@link ErinnerungsMarkierungService}), damit ein einzelner echter
 * Fehlschlag nicht den ganzen Lauf zurückrollt — ein Lauf, der an einer Buchung scheitert, soll
 * nicht hunderte andere Familien um ihre Erinnerung bringen.
 *
 * <p>Diese Methode selbst bleibt {@code @Transactional}, nicht für die Buchungen — die laufen in
 * ihrer eigenen —, sondern damit {@link Ereignisse#veroeffentliche} innerhalb einer aktiven
 * Transaktion geschieht: Nur dann feuert der {@code @TransactionalEventListener(AFTER_COMMIT)} des
 * Mailversands.
 */
@RequiredArgsConstructor
@Service
@Slf4j
class ErinnernService implements Erinnern {

  private final SprechtagAnsichten sprechtagAnsichten;
  private final BuchungsAnsichten buchungsAnsichten;
  private final ErinnerungsMarkierungService markierung;
  private final Ereignisse ereignisse;

  @Override
  @Transactional
  public int erinnere() {
    LocalDate heute = LocalDate.now();
    LocalDateTime jetzt = LocalDateTime.now();
    List<BuchungId> erinnert = new ArrayList<>();

    for (ErinnerungsKandidat kandidat : sprechtagAnsichten.mitErinnerung()) {
      if (!kandidat.erinnerungsVorlauf().istFaelligAm(kandidat.datum(), heute)) {
        continue;
      }
      for (BuchungId buchungId : buchungsAnsichten.aktiveUnerinnerteBuchungen(kandidat.id())) {
        try {
          markierung.markiere(buchungId, jetzt).ifPresent(erinnert::add);
        } catch (RuntimeException e) {
          log.warn("Erinnerung an Buchung {} fehlgeschlagen: {}", buchungId.wert(), e.getMessage());
        }
      }
    }

    if (!erinnert.isEmpty()) {
      ereignisse.veroeffentliche(new ErinnerungFaellig(erinnert));
    }
    return erinnert.size();
  }
}
