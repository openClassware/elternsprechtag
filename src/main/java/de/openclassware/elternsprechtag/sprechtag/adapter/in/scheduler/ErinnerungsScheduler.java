package de.openclassware.elternsprechtag.sprechtag.adapter.in.scheduler;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.Erinnern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Der erste zeitgesteuerte Vorgang im Produkt (Issue #107): löst täglich einen Lauf von
 * {@link Erinnern} aus — sonst nichts. Wie ein Presenter ruft dieser Trigger ausschließlich den
 * Use-Case-Port, nie den Service direkt.
 *
 * <p>Bewusst ohne Knopf beim Organizer (`ABDECKUNG.md` Z. 292): Wer den Sprechtag drei Wochen
 * vorher vorbereitet hat, ist genau der, der am Vortag anderes zu tun hat. Die Uhrzeit ist
 * konfigurierbar ({@code elternsprechtag.scheduler.erinnerung-cron}), damit sie sich ohne
 * Deployment verschieben lässt, falls sie sich mit dem SMTP-Fenster oder anderen Läufen beißt.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class ErinnerungsScheduler {

  private final Erinnern erinnern;

  @Scheduled(cron = "${elternsprechtag.scheduler.erinnerung-cron}")
  void laufe() {
    int anzahl = erinnern.erinnere();
    log.info("Erinnerungs-Lauf: {} Buchung(en) erinnert", anzahl);
  }
}
