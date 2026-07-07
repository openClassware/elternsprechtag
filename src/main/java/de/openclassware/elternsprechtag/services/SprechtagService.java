package de.openclassware.elternsprechtag.services;

import de.openclassware.elternsprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.repositories.SprechtagRepository;
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
}
