package de.openclassware.elternsprechtag.services;

import de.openclassware.elternsprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.repositories.SprechtagRepository;
import java.util.Comparator;
import java.util.List;
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
}
