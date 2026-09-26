package de.openclassware.elternsprechtag.sprechtag.adapter.in.scheduler;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.Anonymisieren;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Löst nachts einen Anonymisierungs-Lauf über {@link Anonymisieren} aus (Issue #126) — sonst
 * nichts. Die Uhrzeit ist eigens konfigurierbar
 * ({@code elternsprechtag.scheduler.anonymisierung-cron}) und bewusst von {@link AbschlussScheduler}
 * und {@link ErinnerungsScheduler} getrennt: Ein Fehlschlag des einen Laufs lässt die anderen stehen.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class AnonymisierungsScheduler {

  private final Anonymisieren anonymisieren;

  @Scheduled(cron = "${elternsprechtag.scheduler.anonymisierung-cron}")
  void laufe() {
    int anzahl = anonymisieren.anonymisiere();
    log.info("Anonymisierungs-Lauf: {} Sprechtag(e) anonymisiert", anzahl);
  }
}
