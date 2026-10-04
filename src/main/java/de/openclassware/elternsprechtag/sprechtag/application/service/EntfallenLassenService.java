package de.openclassware.elternsprechtag.sprechtag.application.service;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.EntfallenLassen;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.BuchungsAnsichten;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.BuchungsAnsichten.BelegZeile;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Ereignisse;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Sprechtage;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.TerminAnsichten;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Termine;
import de.openclassware.elternsprechtag.sprechtag.domain.AusfallErfasst;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungId;
import de.openclassware.elternsprechtag.sprechtag.domain.LehrkraftId;
import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagNichtVeroeffentlichtException;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
import de.openclassware.elternsprechtag.sprechtag.domain.TerminId;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sammelaktion „Lehrkraft fällt aus" (Issue #156): lässt eine Liste von Terminen entfallen, mit
 * derselben Zweistufigkeit wie {@code ErinnernService} — Lesewege liefern nur das Angebot, geprüft
 * und geändert wird an jedem einzelnen {@code Termin}-Aggregat, eine Transaktion je Termin
 * ({@link AusfallMarkierungService}), damit ein einzelner Fehlschlag nicht die übrigen zurückrollt.
 *
 * <p>Diese Methode bleibt {@code @Transactional}, nicht für die Termine — die laufen in ihrer
 * eigenen —, sondern damit {@link Ereignisse#veroeffentliche} innerhalb einer aktiven Transaktion
 * geschieht: Nur dann feuert der {@code @TransactionalEventListener(AFTER_COMMIT)} des Versands.
 */
@RequiredArgsConstructor
@Service
@Slf4j
class EntfallenLassenService implements EntfallenLassen {

  private final Termine termine;
  private final Sprechtage sprechtage;
  private final TerminAnsichten terminAnsichten;
  private final BuchungsAnsichten buchungsAnsichten;
  private final AusfallMarkierungService markierung;
  private final Ereignisse ereignisse;

  @Override
  @Transactional(readOnly = true)
  public Angebot angebot(UUID sprechtagId, UUID lehrkraftId) {
    SprechtagId id = SprechtagId.von(sprechtagId);
    List<SlotZeile> slots =
        terminAnsichten.ausfallSlots(id, LehrkraftId.von(lehrkraftId)).stream()
            .map(
                zeile ->
                    new SlotZeile(
                        zeile.terminId(),
                        zeile.zeit(),
                        zuSlotZustand(zeile.zustand()),
                        zeile.schuelerName(),
                        zeile.elternName(),
                        zeile.empfaengerSchluessel()))
            .toList();
    // Dieselbe Frage wie beim Entfallenlassen, nur als Vorschau: Sie darf bis zum Klick veralten.
    boolean nachbuchbar =
        sprechtage
            .lade(id)
            .map(sprechtag -> sprechtag.nimmtElternbuchungenAn(LocalDateTime.now()))
            .orElse(false);
    return new Angebot(slots, nachbuchbar);
  }

  private static SlotZustand zuSlotZustand(TerminAnsichten.AusfallSlotZustand zustand) {
    return switch (zustand) {
      case FREI -> SlotZustand.FREI;
      case GEBUCHT -> SlotZustand.GEBUCHT;
      case ENTFALLEN -> SlotZustand.ENTFALLEN;
    };
  }

  @Override
  @Transactional
  public Ergebnis entfallenLassen(List<UUID> terminIdsRoh) {
    if (terminIdsRoh == null || terminIdsRoh.isEmpty()) {
      return new Ergebnis(0, 0);
    }
    List<TerminId> terminIds = terminIdsRoh.stream().map(TerminId::von).toList();

    // Dieselbe Vorbedingung wie beim Storno und aus demselben Grund geprüft: Die Auswertungs-Route
    // ist per URL für jeden Sprechtag-Status erreichbar. Ermittelt über den ersten Termin — der
    // Dialog bietet ausschließlich Termine desselben Sprechtags an.
    Termin erster =
        termine
            .lade(terminIds.get(0))
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Termin nicht gefunden: " + terminIds.get(0).wert()));
    Sprechtag sprechtag =
        sprechtage
            .lade(erster.sprechtag())
            .orElseThrow(
                () -> new IllegalStateException("Termin ohne Sprechtag: " + erster.id().wert()));
    if (sprechtag.status() != SprechtagStatus.VEROEFFENTLICHT) {
      throw new SprechtagNichtVeroeffentlichtException(
          "Ausfall nur an einem veröffentlichten Sprechtag, nicht bei " + sprechtag.status());
    }
    // Entschieden beim Schreiben, nicht beim Versand: Ob die Mail den Elternlink trägt, hängt am
    // Stand im Moment des Ausfalls (Issue #109). Die Regel selbst kennt allein das Aggregat.
    boolean nachbuchbar = sprechtag.nimmtElternbuchungenAn(LocalDateTime.now());

    int entfalleneTermine = 0;
    List<BuchungId> stornierteBuchungen = new ArrayList<>();
    for (TerminId terminId : terminIds) {
      try {
        Optional<AusfallMarkierungService.Ergebnis> ergebnis = markierung.entfallenLassen(terminId);
        if (ergebnis.isPresent()) {
          entfalleneTermine++;
          ergebnis.get().stornierteBuchung().ifPresent(stornierteBuchungen::add);
        }
      } catch (RuntimeException e) {
        log.warn("Termin {} konnte nicht entfallen lassen werden: {}", terminId.wert(), e.getMessage());
      }
    }

    int benachrichtigteAdressen = 0;
    if (!stornierteBuchungen.isEmpty()) {
      benachrichtigteAdressen =
          (int)
              buchungsAnsichten.belege(stornierteBuchungen).stream()
                  .map(BelegZeile::elternEmail)
                  .distinct()
                  .count();
      ereignisse.veroeffentliche(new AusfallErfasst(stornierteBuchungen, nachbuchbar));
    }
    return new Ergebnis(entfalleneTermine, benachrichtigteAdressen);
  }
}
