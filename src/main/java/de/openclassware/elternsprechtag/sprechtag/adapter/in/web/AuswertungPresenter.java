package de.openclassware.elternsprechtag.sprechtag.adapter.in.web;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten.LehrkraftPlan;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten.SprechtagAuswertung;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.EntfallenLassen;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.EntfallenLassen.AusfallSlot;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.EntfallenLassen.Ausfallergebnis;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Stornieren;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Umbuchen;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Umbuchen.SlotOption;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Umbuchen.UmbuchAnfrage;
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
  private final Umbuchen umbuchen;
  private final EntfallenLassen entfallenLassen;

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
   * Die freien Slots derselben Lehrkraft wie die übergebene Buchung — das Angebot des
   * Umbuchen-Dialogs. Reicht nur durch: Die Auswahl trifft {@code Umbuchen} selbst.
   */
  List<SlotOption> freieSlots(UUID buchungId) {
    return umbuchen.freieSlots(buchungId);
  }

  /**
   * Reicht das Umbuchen an den Use Case durch — Spiegelbild zu {@link #storniere(UUID)}.
   *
   * @return leer, wenn es geklappt hat — sonst die Begründung der Weigerung.
   */
  Optional<Meldung> umbuche(UUID buchungId, UUID neuerTerminId) {
    try {
      umbuchen.umbuche(new UmbuchAnfrage(buchungId, neuerTerminId));
      return Optional.empty();
    } catch (RuntimeException fehler) {
      return Optional.of(SprechtagMeldungen.zu(fehler));
    }
  }

  /**
   * Ob die Ansicht je Lehrkraft die Sammelaktion „Lehrkraft fällt aus" anbietet. Dieselbe Bedingung
   * wie beim Storno: Nur an einem veröffentlichten Sprechtag gibt es Termine, deren Ausfall jemanden
   * erreichen müsste — vorher weiß niemand von ihnen, nachher ist der Tag vorbei.
   *
   * <p>Verbindlich ist die Bedingung erst im {@code EntfallenLassen}-Use-Case; die Route ist per URL
   * für jeden Status erreichbar.
   */
  boolean darfEntfallenLassen(SprechtagAuswertung auswertung) {
    return auswertung.status() == SprechtagStatus.VEROEFFENTLICHT;
  }

  /**
   * Alle Slots dieser Lehrkraft — die Vorlage des Ausfall-Dialogs. Reicht nur durch: Zustand und
   * Familien-Schlüssel setzt der Use Case zusammen.
   */
  List<AusfallSlot> ausfallSlots(UUID sprechtagId, UUID lehrerId) {
    return entfallenLassen.slotsDerLehrkraft(sprechtagId, lehrerId);
  }

  /**
   * Reicht den Ausfall-Vorgang an den Use Case durch — Spiegelbild zu {@link #storniere(UUID)}, nur
   * dass hier auch im Erfolgsfall etwas zurückkommt: die <em>tatsächlichen</em> Zahlen, die die
   * Oberfläche danach meldet. Die Größe der Auswahl ist nicht dasselbe: Ein Termin, der inzwischen
   * anderswo entfallen ist, wird still übersprungen, und eine Buchung, die zwischen Öffnen und
   * Bestätigen dazukam, zählt mit.
   */
  Ausfallausgang lassEntfallen(List<UUID> terminIds) {
    try {
      return new Ausfallausgang(entfallenLassen.lassEntfallen(terminIds), null);
    } catch (RuntimeException fehler) {
      return new Ausfallausgang(null, SprechtagMeldungen.zu(fehler));
    }
  }

  /**
   * Wie ein Ausfall-Vorgang ausgegangen ist: entweder das Ergebnis oder die Begründung einer
   * Weigerung — nie beides, nie keins.
   */
  record Ausfallausgang(Ausfallergebnis ergebnis, Meldung weigerung) {

    boolean geglueckt() {
      return weigerung == null;
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
