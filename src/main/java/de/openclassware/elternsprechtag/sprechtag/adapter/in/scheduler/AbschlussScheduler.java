package de.openclassware.elternsprechtag.sprechtag.adapter.in.scheduler;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.Abschliessen;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Löst nachts einen Abschluss-Lauf über {@link Abschliessen} aus (Issue #124) — sonst nichts.
 *
 * <p>Eine Löschfrist, die an einem vergessenen Handgriff hängt, ist keine Frist (`ABDECKUNG.md`
 * Z. 431): Dass der Sprechtag vorbei ist, stellt die Maschine fest. Die Uhrzeit ist eigens
 * konfigurierbar ({@code elternsprechtag.scheduler.abschluss-cron}) und bewusst vom
 * {@link ErinnerungsScheduler} getrennt.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class AbschlussScheduler {

  private final Abschliessen abschliessen;

  @Scheduled(cron = "${elternsprechtag.scheduler.abschluss-cron}")
  void laufe() {
    int anzahl = abschliessen.schliesseVorbeiAb();
    log.info("Abschluss-Lauf: {} Sprechtag(e) abgeschlossen", anzahl);
  }
}
