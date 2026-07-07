package de.openclassware.elternsprechtag.services;

import de.openclassware.elternsprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.domain.SprechtagStatusEnum;
import de.openclassware.elternsprechtag.repositories.SprechtagRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class SprechtagService {

  private final SprechtagRepository sprechtagRepository;

  public List<Sprechtag> findSprechtageSortedByStartDate() {
    return sprechtagRepository.findAll().stream()
        .sorted(Comparator.comparing(Sprechtag::getStartDate))
        .toList();
  }

  public Sprechtag save(Sprechtag sprechtag) {
    return sprechtagRepository.save(sprechtag);
  }

  public Optional<Sprechtag> findById(UUID id) {
    return sprechtagRepository.findById(id);
  }

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
    return sprechtagRepository.save(sprechtag);
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
