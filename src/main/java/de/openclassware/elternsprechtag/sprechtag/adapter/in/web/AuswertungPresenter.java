package de.openclassware.elternsprechtag.sprechtag.adapter.in.web;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten.LehrkraftPlan;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten.SprechtagAuswertung;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Stornieren;
import de.openclassware.elternsprechtag.sprechtag.adapter.in.web.SprechtagMeldungen.Meldung;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
class AuswertungPresenter {

  private final Auswerten auswerten;
  private final Stornieren stornieren;

  Optional<SprechtagAuswertung> werteAus(UUID sprechtagId) {
    return auswerten.werteAus(sprechtagId);
  }

  /**
   * Ob die Ansicht je Zeile eine Storno-Aktion anbietet. Nur an einem veröffentlichten Sprechtag:
   * Danach ist „wieder frei" eine Lüge, weil niemand mehr bucht, und ein Löschverlangen nach dem
   * Sprechtag ist Anonymisierung, nicht Storno.
   *
   * <p>Die Entscheidung liegt hier und nicht im View — der rendert nur, was er bekommt. Verbindlich
   * ist sie ohnehin erst im Use Case: Die Route ist per URL für jeden Status erreichbar.
   */
  boolean darfStornieren(SprechtagAuswertung auswertung) {
    return auswertung.status() == SprechtagStatus.VEROEFFENTLICHT;
  }

  /**
   * Ob die Ansicht den Einstieg ins Nachtragen anbietet. Dieselbe Bedingung wie beim Storno: Nur an
   * einem veröffentlichten Sprechtag gibt es Termine, die eine Familie belegen könnte.
   *
   * <p>Verbindlich ist auch das erst im {@code Nachtragen}-Use-Case — die Route ist per URL für
   * jeden Status erreichbar.
   */
  boolean darfNachtragen(SprechtagAuswertung auswertung) {
    return auswertung.status() == SprechtagStatus.VEROEFFENTLICHT;
  }

  /**
   * Reicht das Storno an den Use Case durch.
   *
   * @return leer, wenn es geklappt hat — sonst die Begründung der Weigerung. Eine verletzte
   *     Vorbedingung (fremder Tab, Sprechtag inzwischen abgeschlossen) soll der Organizer sehen und
   *     nicht als stille Wirkungslosigkeit erleben.
   */
  Optional<Meldung> storniere(UUID buchungId) {
    try {
      stornieren.storniere(buchungId);
      return Optional.empty();
    } catch (RuntimeException fehler) {
      return Optional.of(SprechtagMeldungen.zu(fehler));
    }
  }

  /**
   * Filtert die Lehrkraft-Pläne auf genau eine Lehrkraft. {@code lehrerId == null} bedeutet „alle
   * Lehrkräfte". Reine Teilmengen-Bildung ohne Zustand (analog {@code ManageSprechtagPresenter.filter});
   * die aktuelle Auswahl hält der View.
   */
  List<LehrkraftPlan> filter(List<LehrkraftPlan> alle, UUID lehrerId) {
    if (lehrerId == null) {
      return alle;
    }
    return alle.stream().filter(plan -> plan.lehrerId().equals(lehrerId)).toList();
  }
}
