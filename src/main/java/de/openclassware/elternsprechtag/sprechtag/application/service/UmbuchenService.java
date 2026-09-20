package de.openclassware.elternsprechtag.sprechtag.application.service;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.Umbuchen;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Ereignisse;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Sprechtage;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Termine;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.TerminAnsichten;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.TerminAnsichten.SlotZeile;
import de.openclassware.elternsprechtag.sprechtag.domain.Anlass;
import de.openclassware.elternsprechtag.sprechtag.domain.Buchung;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungBereitsStorniertException;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungId;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungNichtGefundenException;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungenBestaetigt;
import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagNichtVeroeffentlichtException;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
import de.openclassware.elternsprechtag.sprechtag.domain.TerminBelegtException;
import de.openclassware.elternsprechtag.sprechtag.domain.TerminId;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verschiebt eine Buchung auf einen anderen freien Slot derselben Lehrkraft — Storno und Neubuchung
 * in einem Zug (ADR 0005). Kein Fall für {@link BuchungsVorgangService}: Der dort geteilte
 * Mechanismus bucht nur, hier kommt eine Stornierung am eigenen Aggregat hinzu, und das Buchungsziel
 * wird nicht neu aufgelöst, sondern aus der alten Buchung übernommen.
 *
 * <p><b>Reihenfolge entscheidet.</b> Zuerst wird der neue Slot gebucht und sofort gespeichert —
 * erst wenn er sicher steht, wird der alte storniert. Scheitert die Neubuchung, bleibt die alte
 * Buchung unangetastet; scheitert das Speichern der Stornierung, rollt die ganze Transaktion
 * zurück und nimmt die eben gespeicherte Neubuchung mit zurück. So steht die Familie nie ohne
 * Termin da.
 */
@RequiredArgsConstructor
@Service
class UmbuchenService implements Umbuchen {

  private final Termine termine;
  private final Sprechtage sprechtage;
  private final TerminAnsichten terminAnsichten;
  private final Ereignisse ereignisse;

  @Override
  @Transactional(readOnly = true)
  public List<SlotOption> freieSlots(UUID buchungId) {
    Termin alterTermin =
        termine
            .ladeZuBuchung(BuchungId.von(buchungId))
            .orElseThrow(
                () -> new BuchungNichtGefundenException("Buchung nicht gefunden: " + buchungId));
    UUID lehrkraft = alterTermin.lehrkraft().wert();
    // Die vorhandene Slot-Abfrage der Eltern-Ansicht, hier in Java auf eine Lehrkraft eingeengt —
    // kein neues SQL, kein neuer Query-Port.
    return terminAnsichten.slots(alterTermin.sprechtag()).stream()
        .filter(SlotZeile::buchbar)
        .filter(slot -> slot.lehrkraftId().equals(lehrkraft))
        .map(slot -> new SlotOption(slot.terminId(), slot.zeit()))
        .toList();
  }

  @Override
  @Transactional
  public UUID umbuche(UmbuchAnfrage anfrage) {
    BuchungId alteId = BuchungId.von(anfrage.buchungId());
    Termin alterTermin =
        termine
            .ladeZuBuchung(alteId)
            .orElseThrow(
                () -> new BuchungNichtGefundenException("Buchung nicht gefunden: " + anfrage.buchungId()));

    // Dieselbe Vorbedingung wie beim Storno und aus demselben Grund hier geprüft: Die
    // Auswertungs-Route ist per URL für jeden Sprechtag-Status erreichbar.
    Sprechtag sprechtag =
        sprechtage
            .lade(alterTermin.sprechtag())
            .orElseThrow(
                () -> new IllegalStateException("Termin ohne Sprechtag: " + alterTermin.id().wert()));
    if (sprechtag.status() != SprechtagStatus.VEROEFFENTLICHT) {
      throw new SprechtagNichtVeroeffentlichtException(
          "Umbuchen nur an einem veröffentlichten Sprechtag, nicht bei " + sprechtag.status());
    }

    // Dieselbe Prüfung wie beim Storno: Nur die aktuell aktive Buchung ihres Termins darf umgebucht
    // werden — ein zweiter Klick aus einem alten Browser-Tab soll nicht stillschweigend eine
    // inzwischen andere Buchung verschieben.
    Optional<Buchung> aktive = alterTermin.aktiveBuchung();
    if (aktive.isEmpty() || !aktive.get().id().equals(alteId)) {
      throw new BuchungBereitsStorniertException(
          "Buchung ist nicht die aktive Buchung ihres Termins: " + anfrage.buchungId());
    }
    Buchung alte = aktive.get();

    Termin neuerTermin =
        termine
            .lade(TerminId.von(anfrage.neuerTerminId()))
            .orElseThrow(
                () ->
                    new IllegalArgumentException("Termin nicht gefunden: " + anfrage.neuerTerminId()));

    LocalDateTime jetzt = LocalDateTime.now();
    BuchungId neueId;
    try {
      // Das Buchungsziel kommt unverändert aus der alten Buchung — nicht neu aus der
      // Schulorganisation geholt. Ein inzwischen stillgelegter Lehrauftrag blockiert das Umbuchen
      // deshalb nicht.
      neueId = neuerTermin.buche(alte.familie(), alte.ziel(), alte.notiz().orElse(null), jetzt);
      // Sofort speichern, damit ein Versionskonflikt hier auftritt und die alte Buchung unberührt
      // bleibt — nach demselben Muster wie in BuchungsVorgangService.
      termine.speichere(neuerTermin);
    } catch (OptimisticLockingFailureException konflikt) {
      throw new TerminBelegtException("Der gewählte Termin wurde soeben vergeben.");
    }

    alterTermin.storniere(alteId);
    try {
      termine.speichere(alterTermin);
    } catch (OptimisticLockingFailureException konflikt) {
      // Zwischen dem Laden und dem Stornieren kam jemand anders an dieselbe Buchung — fachlich
      // derselbe Fall wie beim Storno. Die Transaktion rollt vollständig zurück, die eben
      // gespeicherte Neubuchung eingeschlossen.
      throw new BuchungBereitsStorniertException(
          "Buchung wurde soeben von anderer Stelle verändert: " + anfrage.buchungId());
    }
    // Das Storno-Ereignis wird wie beim reinen Storno verworfen — nur die Neubuchung wird bestätigt.
    alterTermin.ereignisseAbholen();

    List<BuchungId> gebucht = BuchungsVorgangService.angelegteBuchungen(neuerTermin);
    ereignisse.veroeffentliche(new BuchungenBestaetigt(gebucht, Anlass.UMBUCHUNG));
    return neueId.wert();
  }
}
