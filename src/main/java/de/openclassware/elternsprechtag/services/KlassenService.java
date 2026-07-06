package de.openclassware.elternsprechtag.services;

import de.openclassware.elternsprechtag.domain.Klasse;
import de.openclassware.elternsprechtag.repositories.KlassenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class KlassenService {

    private final KlassenRepository klassenRepository;

    public List<Klasse> findAllOrderByName() {
        return klassenRepository.findAllByOrderByNameAsc();
    }

}
