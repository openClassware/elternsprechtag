package de.openclassware.elternsprechtag.services;

import de.openclassware.elternsprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.repositories.SprechtagRepository;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class SprechtagService {

  private final SprechtagRepository sprechtagRepository;

  public List<Sprechtag> findSprechtageSortedByStartDate() {

      Sprechtag sprechtag = new Sprechtag();
      sprechtag.setTitel("Sommersprechtag");
      sprechtag.setStartDate(LocalDate.now());
      sprechtag.setStartTime(LocalTime.now().minus(Duration.ofHours(1)));
      sprechtag.setEndTime(LocalTime.now());
      return List.of(
              sprechtag, sprechtag, sprechtag, sprechtag, sprechtag
      );

//    return sprechtagRepository.findAll().stream()
//        .sorted(Comparator.comparing(Sprechtag::getStartDate))
//        .toList();
  }
}
