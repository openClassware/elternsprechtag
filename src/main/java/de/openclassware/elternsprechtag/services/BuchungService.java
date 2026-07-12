package de.openclassware.elternsprechtag.services;

import de.openclassware.elternsprechtag.domain.Buchung;
import de.openclassware.elternsprechtag.domain.BuchungStatusEnum;
import de.openclassware.elternsprechtag.domain.Fach;
import de.openclassware.elternsprechtag.domain.Klasse;
import de.openclassware.elternsprechtag.domain.Lehrauftrag;
import de.openclassware.elternsprechtag.domain.Lehrer;
import de.openclassware.elternsprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.domain.Termin;
import de.openclassware.elternsprechtag.domain.TerminStatusEnum;
import de.openclassware.elternsprechtag.repositories.BuchungRepository;
import de.openclassware.elternsprechtag.repositories.LehrauftragRepository;
import de.openclassware.elternsprechtag.repositories.TerminRepository;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class BuchungService {

  private final TerminRepository terminRepository;
  private final LehrauftragRepository lehrauftragRepository;
  private final BuchungRepository buchungRepository;

  /** Ein wählbarer Slot in der Eltern-View (aus einem materialisierten {@link Termin}). */
  public record SlotOption(UUID terminId, LocalTime zeit, boolean belegt) {}

  /** Ein wählbares Fach der gewählten Klasse (ein {@link Lehrauftrag}) samt Slots seines Lehrers. */
  public record FachOption(
      UUID lehrauftragId,
      UUID lehrerId,
      String fachShortName,
      String fachName,
      String lehrerName,
      List<SlotOption> slots) {}

  /** Ein einzelner gewünschter Termin: welcher Lehrauftrag (Klasse+Fach+Lehrer) zu welchem Slot. */
  public record BuchungsWunsch(UUID lehrauftragId, UUID terminId) {}

  /** Ein kompletter Eltern-Submit: Angaben plus alle gewählten Fach-Slots. */
  public record BuchungsAnfrage(
      String elternName, String schuelerName, String notiz, List<BuchungsWunsch> wuensche) {}

  /** Signalisiert, dass ein gewünschter Slot zwischenzeitlich vergeben wurde. */
  public static class TerminBelegtException extends RuntimeException {
    public TerminBelegtException(String message) {
      super(message);
    }
  }

  /**
   * Baut die Fach-Auswahl für die gewählte Klasse: je {@link Lehrauftrag} der Klasse ein Eintrag mit
   * den (materialisierten) Slots seines Lehrers an diesem Sprechtag. Mappt vollständig auf DTOs,
   * damit die View ohne offene Session (open-in-view=false) arbeiten kann.
   */
  @Transactional(readOnly = true)
  public List<FachOption> ladeFachOptionen(Sprechtag sprechtag, Klasse klasse) {
    // Alle Termine des Sprechtags einmal laden und nach Lehrer gruppieren.
    Map<UUID, List<SlotOption>> slotsByLehrer = new LinkedHashMap<>();
    for (Termin termin :
        terminRepository.findBySprechtagOrderByLehrer_NachnameAscStartzeitAsc(sprechtag)) {
      slotsByLehrer
          .computeIfAbsent(termin.getLehrer().getId(), k -> new ArrayList<>())
          .add(
              new SlotOption(
                  termin.getId(),
                  termin.getStartzeit().toLocalTime(),
                  termin.getStatus() == TerminStatusEnum.BELEGT));
    }

    List<FachOption> optionen = new ArrayList<>();
    for (Lehrauftrag lehrauftrag : lehrauftragRepository.findByKlasseOrderByFach_NameAsc(klasse)) {
      Lehrer lehrer = lehrauftrag.getLehrer();
      Fach fach = lehrauftrag.getFach();
      optionen.add(
          new FachOption(
              lehrauftrag.getId(),
              lehrer.getId(),
              fach.getShortName(),
              fach.getName(),
              lehrer.getVorname() + " " + lehrer.getNachname(),
              slotsByLehrer.getOrDefault(lehrer.getId(), List.of())));
    }
    return optionen;
  }

  /**
   * Persistiert einen Eltern-Submit als N Buchungen — alles oder nichts. Ist auch nur ein Slot
   * nicht mehr frei, wird die gesamte Transaktion zurückgerollt und {@link TerminBelegtException}
   * geworfen.
   */
  @Transactional
  public List<Buchung> buchen(BuchungsAnfrage anfrage) {
    List<Buchung> buchungen = new ArrayList<>();
    LocalDateTime jetzt = LocalDateTime.now();
    try {
      for (BuchungsWunsch wunsch : anfrage.wuensche()) {
        Termin termin =
            terminRepository
                .findById(wunsch.terminId())
                .orElseThrow(
                    () -> new IllegalArgumentException("Termin nicht gefunden: " + wunsch.terminId()));
        Lehrauftrag lehrauftrag =
            lehrauftragRepository
                .findById(wunsch.lehrauftragId())
                .orElseThrow(
                    () ->
                        new IllegalArgumentException(
                            "Lehrauftrag nicht gefunden: " + wunsch.lehrauftragId()));

        if (termin.getStatus() != TerminStatusEnum.FREI) {
          throw new TerminBelegtException("Termin ist bereits vergeben: " + termin.getStartzeit());
        }
        if (termin.getLehrer() == null
            || lehrauftrag.getLehrer() == null
            || !termin.getLehrer().getId().equals(lehrauftrag.getLehrer().getId())) {
          throw new IllegalArgumentException(
              "Termin und Lehrauftrag gehören zu unterschiedlichen Lehrern.");
        }

        termin.setStatus(TerminStatusEnum.BELEGT);
        // Sofort flushen, damit ein optimistischer Lock-Konflikt hier (im try) auftritt und nicht
        // erst beim Commit außerhalb des Catch-Blocks.
        terminRepository.saveAndFlush(termin);

        Buchung buchung = new Buchung();
        buchung.setErstelltAm(jetzt);
        buchung.setStatus(BuchungStatusEnum.ZUGESAGT);
        buchung.setElternName(anfrage.elternName());
        buchung.setSchuelerName(anfrage.schuelerName());
        buchung.setNotiz(anfrage.notiz());
        buchung.setLehrauftrag(lehrauftrag);
        buchung.setTermin(termin);
        buchungen.add(buchungRepository.save(buchung));
      }
      return buchungen;
    } catch (OptimisticLockingFailureException e) {
      // Zwei Eltern haben denselben Slot parallel gebucht — als Konflikt behandeln.
      throw new TerminBelegtException("Ein gewählter Termin wurde soeben vergeben.");
    }
  }
}
