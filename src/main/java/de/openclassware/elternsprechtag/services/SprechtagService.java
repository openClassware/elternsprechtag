package de.openclassware.elternsprechtag.services;

import de.openclassware.elternsprechtag.domain.Klasse;
import de.openclassware.elternsprechtag.domain.Lehrauftrag;
import de.openclassware.elternsprechtag.domain.Lehrer;
import de.openclassware.elternsprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.domain.SprechtagStatusEnum;
import de.openclassware.elternsprechtag.domain.Termin;
import de.openclassware.elternsprechtag.domain.TerminStatusEnum;
import de.openclassware.elternsprechtag.repositories.KlassenRepository;
import de.openclassware.elternsprechtag.repositories.LehrauftragRepository;
import de.openclassware.elternsprechtag.repositories.SprechtagRepository;
import de.openclassware.elternsprechtag.repositories.TerminRepository;
import de.openclassware.elternsprechtag.services.KlassenService.KlasseOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class SprechtagService {

  private final SprechtagRepository sprechtagRepository;
  private final LehrauftragRepository lehrauftragRepository;
  private final TerminRepository terminRepository;
  private final KlassenRepository klassenRepository;
  private final ApplicationEventPublisher eventPublisher;

  /** Read-Model einer Sprechtag-Zeile für Übersicht/Verwaltung — enthält nur, was Views rendern. */
  public record SprechtagRow(
      UUID id,
      String titel,
      LocalDate startDate,
      LocalTime startTime,
      LocalTime endTime,
      String location,
      SprechtagStatusEnum status,
      String accessToken,
      List<String> klassen) {}

  /**
   * Mutierbares Formularmodell zum Anlegen/Bearbeiten. Entkoppelt den Edit-View von der JPA-Entity
   * (Binder-kompatibel: Getter/Setter, No-Args-Konstruktor).
   */
  @Getter
  @Setter
  @NoArgsConstructor
  public static class SprechtagForm {
    private String titel;
    private String location;
    private String description;
    private LocalDate startDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer slotInMinutes = 15;
    private String accessToken = UUID.randomUUID().toString();
    private Set<KlasseOption> klassen = new LinkedHashSet<>();
  }

  /** Read-Model der öffentlichen Eltern-Buchungsseite: Kopfdaten + wählbare Klassen. */
  public record SprechtagPublic(
      UUID id,
      String titel,
      LocalDate startDate,
      LocalTime startTime,
      LocalTime endTime,
      String location,
      String description,
      Integer slotInMinutes,
      SprechtagStatusEnum status,
      List<KlasseOption> klassen) {}

  @Transactional(readOnly = true)
  public List<SprechtagRow> findAllRows() {
    return sprechtagRepository.findAll().stream()
        .sorted(Comparator.comparing(Sprechtag::getStartDate))
        .map(this::toRow)
        .toList();
  }

  private SprechtagRow toRow(Sprechtag sprechtag) {
    return new SprechtagRow(
        sprechtag.getId(),
        sprechtag.getTitel(),
        sprechtag.getStartDate(),
        sprechtag.getStartTime(),
        sprechtag.getEndTime(),
        sprechtag.getLocation(),
        sprechtag.getStatus(),
        sprechtag.getAccessToken(),
        sprechtag.getKlassen().stream()
            .map(Klasse::getName)
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList());
  }

  @Transactional(readOnly = true)
  public Optional<SprechtagForm> loadForm(UUID id) {
    return sprechtagRepository.findById(id).map(this::toForm);
  }

  private SprechtagForm toForm(Sprechtag sprechtag) {
    SprechtagForm form = new SprechtagForm();
    form.setTitel(sprechtag.getTitel());
    form.setLocation(sprechtag.getLocation());
    form.setDescription(sprechtag.getDescription());
    form.setStartDate(sprechtag.getStartDate());
    form.setStartTime(sprechtag.getStartTime());
    form.setEndTime(sprechtag.getEndTime());
    form.setSlotInMinutes(sprechtag.getSlotInMinutes());
    form.setAccessToken(sprechtag.getAccessToken());
    form.setKlassen(
        sprechtag.getKlassen().stream()
            .map(klasse -> new KlasseOption(klasse.getId(), klasse.getName()))
            .collect(Collectors.toCollection(LinkedHashSet::new)));
    return form;
  }

  /**
   * Legt einen Sprechtag an ({@code id == null}) oder aktualisiert einen bestehenden aus dem
   * Formularmodell und setzt den Zielstatus. Materialisiert Termine, falls veröffentlicht wird.
   */
  @Transactional
  public UUID createOrUpdate(UUID id, SprechtagForm form, SprechtagStatusEnum status) {
    Sprechtag sprechtag =
        id == null
            ? new Sprechtag()
            : sprechtagRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sprechtag nicht gefunden: " + id));
    sprechtag.setTitel(form.getTitel());
    sprechtag.setLocation(form.getLocation());
    sprechtag.setDescription(form.getDescription());
    sprechtag.setStartDate(form.getStartDate());
    sprechtag.setStartTime(form.getStartTime());
    sprechtag.setEndTime(form.getEndTime());
    sprechtag.setSlotInMinutes(form.getSlotInMinutes());
    sprechtag.setAccessToken(form.getAccessToken());
    List<UUID> klasseIds = form.getKlassen().stream().map(KlasseOption::id).toList();
    sprechtag.setKlassen(new ArrayList<>(klassenRepository.findAllById(klasseIds)));
    sprechtag.setStatus(status);

    Sprechtag saved = sprechtagRepository.save(sprechtag);
    materialisiereWennNoetig(saved);
    return saved.getId();
  }

  @Transactional(readOnly = true)
  public Optional<SprechtagPublic> findPublicByAccessToken(String accessToken) {
    if (accessToken == null || accessToken.isBlank()) {
      return Optional.empty();
    }
    return sprechtagRepository.findByAccessToken(accessToken).map(this::toPublic);
  }

  private SprechtagPublic toPublic(Sprechtag sprechtag) {
    return new SprechtagPublic(
        sprechtag.getId(),
        sprechtag.getTitel(),
        sprechtag.getStartDate(),
        sprechtag.getStartTime(),
        sprechtag.getEndTime(),
        sprechtag.getLocation(),
        sprechtag.getDescription(),
        sprechtag.getSlotInMinutes(),
        sprechtag.getStatus(),
        sprechtag.getKlassen().stream()
            .sorted(Comparator.comparing(Klasse::getName, String.CASE_INSENSITIVE_ORDER))
            .map(klasse -> new KlasseOption(klasse.getId(), klasse.getName()))
            .toList());
  }

  @Transactional
  public void changeStatus(UUID id, SprechtagStatusEnum newStatus) {
    Sprechtag sprechtag =
        sprechtagRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Sprechtag nicht gefunden: " + id));
    SprechtagStatusEnum current = sprechtag.getStatus();
    if (!current.allowedTransitions().contains(newStatus)) {
      throw new IllegalStateException("Ungültiger Statusübergang: " + current + " -> " + newStatus);
    }
    sprechtag.setStatus(newStatus);
    Sprechtag saved = sprechtagRepository.save(sprechtag);
    materialisiereWennNoetig(saved);
    if (current == SprechtagStatusEnum.VEROEFFENTLICHT
        && newStatus == SprechtagStatusEnum.ABGESAGT) {
      // Nach Commit (AFTER_COMMIT, @Async) werden die betroffenen Eltern benachrichtigt; der
      // Statuswechsel selbst bleibt eine reguläre, für sich abgeschlossene Transaktion.
      eventPublisher.publishEvent(new SprechtagAbgesagtEvent(saved.getId()));
    }
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

  @Transactional
  public UUID duplicate(UUID id) {
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
    return sprechtagRepository.save(copy).getId();
  }
}
