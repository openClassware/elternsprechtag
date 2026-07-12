package de.openclassware.elternsprechtag.services;

import de.openclassware.elternsprechtag.repositories.KlassenRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class KlassenService {

  private final KlassenRepository klassenRepository;

  /** Auswählbare Klasse (Id + Anzeigename) für die UI — hält Entities aus dem View heraus. */
  public record KlasseOption(UUID id, String name) {}

  public List<KlasseOption> findAllOptions() {
    return klassenRepository.findAllByOrderByNameAsc().stream()
        .map(klasse -> new KlasseOption(klasse.getId(), klasse.getName()))
        .toList();
  }
}
