package de.openclassware.elternsprechtag.sprechtag.application.service;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.Materialisieren;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Lehrauftraege;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Lehrauftraege.LehrauftragDaten;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Sprechtage;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Termine;
import de.openclassware.elternsprechtag.sprechtag.domain.KlasseId;
import de.openclassware.elternsprechtag.sprechtag.domain.LehrkraftId;
import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
import de.openclassware.elternsprechtag.sprechtag.domain.Zeitraum;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Erzeugt die Termine eines veröffentlichten Sprechtags: je teilnehmender Lehrkraft einen Slot-Satz.
 *
 * <p>Der Use Case fügt zwei Ports in Java zusammen — die Slots rechnet der Sprechtag aus, die
 * teilnehmenden Lehrkräfte kommen über die Lehraufträge der gewählten Klassen aus der
 * Schulorganisation. Kein SQL joint über diese Grenze (ADR 0003).
 *
 * <p>Jede Lehrkraft bekommt genau <em>einen</em> Slot-Satz, geteilt über all ihre Fächer und Klassen
 * an diesem Sprechtag: Sie kann um 14:00 nur ein Gespräch führen, gleich in welchem Fach.
 *
 * <p>Der Existenz-Check macht den Aufruf idempotent. Erzeugt wird ausschließlich für einen
 * veröffentlichten Sprechtag — ein Entwurf hat noch keine Termine, und ein abgesagter bekommt keine
 * mehr.
 */
@RequiredArgsConstructor
@Service
class MaterialisierenService implements Materialisieren {

  private final Sprechtage sprechtage;
  private final Lehrauftraege lehrauftraege;
  private final Termine termine;

  @Override
  @Transactional
  public int materialisiere(UUID sprechtagId) {
    SprechtagId id = SprechtagId.von(sprechtagId);
    Sprechtag sprechtag =
        sprechtage
            .lade(id)
            .orElseThrow(
                () -> new IllegalArgumentException("Sprechtag nicht gefunden: " + sprechtagId));
    if (sprechtag.status() != SprechtagStatus.VEROEFFENTLICHT || termine.existierenFuer(id)) {
      return 0;
    }

    Set<LehrkraftId> lehrkraefte = teilnehmendeLehrkraefte(sprechtag);
    List<Zeitraum> slots = sprechtag.slots();
    if (lehrkraefte.isEmpty() || slots.isEmpty()) {
      // Kein Fehler, aber auch kein Sprechtag, an dem sich etwas buchen ließe. Der Aufrufer meldet
      // es dem Organizer (`ABDECKUNG.md` Z. 94).
      return 0;
    }

    List<Termin> neue = new ArrayList<>(lehrkraefte.size() * slots.size());
    for (LehrkraftId lehrkraft : lehrkraefte) {
      for (Zeitraum slot : slots) {
        neue.add(Termin.neu(id, lehrkraft, slot));
      }
    }
    termine.speichereAlle(neue);
    return neue.size();
  }

  /** Die Lehrkräfte mit einem Lehrauftrag in einer teilnehmenden Klasse, jede genau einmal. */
  private Set<LehrkraftId> teilnehmendeLehrkraefte(Sprechtag sprechtag) {
    Set<LehrkraftId> lehrkraefte = new LinkedHashSet<>();
    for (KlasseId klasse : sprechtag.klassen()) {
      for (LehrauftragDaten auftrag : lehrauftraege.fuerKlasse(klasse.wert())) {
        lehrkraefte.add(auftrag.lehrkraft());
      }
    }
    return lehrkraefte;
  }
}
