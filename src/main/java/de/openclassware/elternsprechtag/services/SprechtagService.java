package de.openclassware.elternsprechtag.services;

import de.openclassware.elternsprechtag.domain.Klasse;
import de.openclassware.elternsprechtag.domain.Lehrauftrag;
import de.openclassware.elternsprechtag.domain.Lehrer;
import de.openclassware.elternsprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.domain.SprechtagStatusEnum;
import de.openclassware.elternsprechtag.domain.Termin;
import de.openclassware.elternsprechtag.domain.TerminStatusEnum;
import de.openclassware.elternsprechtag.repositories.LehrauftragRepository;
import de.openclassware.elternsprechtag.repositories.SprechtagRepository;
import de.openclassware.elternsprechtag.repositories.TerminRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class SprechtagService {

  private final SprechtagRepository sprechtagRepository;
  private final LehrauftragRepository lehrauftragRepository;
  private final TerminRepository terminRepository;

  public List<Sprechtag> findSprechtageSortedByStartDate() {
    return sprechtagRepository.findAll().stream()
        .sorted(Comparator.comparing(Sprechtag::getStartDate))
        .toList();
  }

  @Transactional
  public Sprechtag save(Sprechtag sprechtag) {
    Sprechtag saved = sprechtagRepository.save(sprechtag);
    materialisiereWennNoetig(saved);
    return saved;
  }

  public Optional<Sprechtag> findById(UUID id) {
    return sprechtagRepository.findById(id);
  }

  public Optional<Sprechtag> findByAccessToken(String accessToken) {
    return sprechtagRepository.findByAccessToken(accessToken);
  }

  @Transactional
  public Sprechtag changeStatus(UUID id, SprechtagStatusEnum newStatus) {
    Sprechtag sprechtag =
        sprechtagRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Sprechtag nicht gefunden: " + id));
    SprechtagStatusEnum current = sprechtag.getStatus();
    if (!current.allowedTransitions().contains(newStatus)) {
      throw new IllegalStateException(
          "Ungültiger Statusübergang: " + current + " -> " + newStatus);
    }
    sprechtag.setStatus(newStatus);
    Sprechtag saved = sprechtagRepository.save(sprechtag);
    materialisiereWennNoetig(saved);
    return saved;
  }

  /**
   * Materialisiert Termine, sobald ein Sprechtag veröffentlicht ist und noch keine besitzt. Der
   * Existenz-Check macht die Erzeugung idempotent (mehrere Speicher-/Statuspfade) und verhindert
   * zugleich eine Regeneration beim erneuten Speichern eines bereits veröffentlichten Sprechtags.
   */
  private void materialisiereWennNoetig(Sprechtag sprechtag) {
    if (sprechtag.getStatus() == SprechtagStatusEnum.VEROEFFENTLICHT
        && !terminRepository.existsBySprechtag(sprechtag)) {
      materialisiereTermine(sprechtag);
    }
  }

  /**
   * Erzeugt beim Veröffentlichen für jeden teilnehmenden Lehrer je Zeit-Slot einen freien Termin.
   * Teilnehmende Lehrer werden über die Lehraufträge der Sprechtag-Klassen abgeleitet; jeder Lehrer
   * bekommt genau einen Slot-Satz (geteilt über all seine Fächer/Klassen an diesem Sprechtag).
   */
  private void materialisiereTermine(Sprechtag sprechtag) {
    // Distinct Lehrer über die Lehraufträge der teilnehmenden Klassen.
    Map<UUID, Lehrer> lehrerById = new LinkedHashMap<>();
    for (Klasse klasse : sprechtag.getKlassen()) {
      for (Lehrauftrag lehrauftrag : lehrauftragRepository.findByKlasseOrderByFach_NameAsc(klasse)) {
        Lehrer lehrer = lehrauftrag.getLehrer();
        if (lehrer != null) {
          lehrerById.putIfAbsent(lehrer.getId(), lehrer);
        }
      }
    }
    if (lehrerById.isEmpty()) {
      return;
    }

    List<LocalTime> slotStarts = slotStartTimes(sprechtag);
    LocalDate datum = sprechtag.getStartDate();
    int slot = sprechtag.getSlotInMinutes();

    List<Termin> termine = new ArrayList<>();
    for (Lehrer lehrer : lehrerById.values()) {
      for (LocalTime start : slotStarts) {
        Termin termin = new Termin();
        termin.setStartzeit(LocalDateTime.of(datum, start));
        termin.setEndzeit(LocalDateTime.of(datum, start.plusMinutes(slot)));
        termin.setStatus(TerminStatusEnum.FREI);
        termin.setLehrer(lehrer);
        termin.setSprechtag(sprechtag);
        termine.add(termin);
      }
    }
    terminRepository.saveAll(termine);
  }

  /** Slot-Startzeiten von startTime bis endTime; ein Rest-Slot, der nicht mehr voll passt, entfällt. */
  private List<LocalTime> slotStartTimes(Sprechtag sprechtag) {
    List<LocalTime> starts = new ArrayList<>();
    int slot = sprechtag.getSlotInMinutes();
    LocalTime end = sprechtag.getEndTime();
    for (LocalTime start = sprechtag.getStartTime();
        !start.plusMinutes(slot).isAfter(end);
        start = start.plusMinutes(slot)) {
      starts.add(start);
    }
    return starts;
  }

  public Sprechtag duplicate(UUID id) {
    Sprechtag original =
        sprechtagRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Sprechtag nicht gefunden: " + id));
    Sprechtag copy = new Sprechtag();
    copy.setTitel(original.getTitel());
    copy.setStartDate(original.getStartDate());
    copy.setStartTime(original.getStartTime());
    copy.setEndTime(original.getEndTime());
    copy.setSlotInMinutes(original.getSlotInMinutes());
    copy.setLocation(original.getLocation());
    copy.setDescription(original.getDescription());
    copy.setAccessToken(UUID.randomUUID().toString());
    copy.setStatus(SprechtagStatusEnum.ENTWURF);
    copy.setKlassen(new ArrayList<>(original.getKlassen()));
    return sprechtagRepository.save(copy);
  }
}
