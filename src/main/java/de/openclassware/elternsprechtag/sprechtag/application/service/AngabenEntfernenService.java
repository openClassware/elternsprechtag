package de.openclassware.elternsprechtag.sprechtag.application.service;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.AngabenEntfernen;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Sprechtage;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Termine;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungId;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungNichtGefundenException;
import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Zieht die Anonymisierung für eine einzelne Buchung vor (Issue #129). Wann das geht, entscheidet
 * der {@link Termin}; der Use Case liest nur den Status des Sprechtags und reicht ihn hinein.
 *
 * <p>Der Sprechtag wird gelesen, verändert wird allein der Termin — „eine Transaktion, ein Aggregat"
 * bleibt gewahrt (ADR 0005). Verschickt wird nichts, ein Ereignis gibt es nicht.
 */
@RequiredArgsConstructor
@Service
class AngabenEntfernenService implements AngabenEntfernen {

  private final Termine termine;
  private final Sprechtage sprechtage;
  private final Pseudonymgeber pseudonymgeber;

  @Override
  @Transactional
  public void entferne(UUID buchungId) {
    BuchungId id = BuchungId.von(buchungId);
    Termin termin =
        termine
            .ladeZuBuchung(id)
            .orElseThrow(
                () -> new BuchungNichtGefundenException("Buchung nicht gefunden: " + buchungId));
    Sprechtag sprechtag =
        sprechtage
            .lade(termin.sprechtag())
            .orElseThrow(
                () -> new IllegalStateException("Termin ohne Sprechtag: " + termin.id().wert()));

    boolean veroeffentlicht = sprechtag.status() == SprechtagStatus.VEROEFFENTLICHT;
    if (termin.entferneAngaben(
        id, pseudonymgeber.neuerLauf(), LocalDateTime.now(), veroeffentlicht)) {
      termine.speichere(termin);
    }
  }
}
