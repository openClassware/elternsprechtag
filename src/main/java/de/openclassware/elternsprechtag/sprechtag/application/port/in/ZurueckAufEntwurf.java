package de.openclassware.elternsprechtag.sprechtag.application.port.in;

import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagHatBuchungenException;
import java.util.UUID;

/**
 * Use Case: eine Veröffentlichung zurücknehmen — für den zu früh veröffentlichten Sprechtag
 * (`ABDECKUNG.md` Z. 91).
 *
 * <p>Die materialisierten Termine werden dabei verworfen; danach ist die Zeitstruktur wieder
 * änderbar, und das nächste Veröffentlichen rechnet sie neu aus.
 */
public interface ZurueckAufEntwurf {

  /**
   * @throws SprechtagHatBuchungenException wenn an diesem Sprechtag bereits gebucht wurde — dann
   *     führt nur die Absage weiter (`ABDECKUNG.md` Z. 92)
   */
  void nimmZurueck(UUID id);
}
