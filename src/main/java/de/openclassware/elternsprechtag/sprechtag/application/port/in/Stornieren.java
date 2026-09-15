package de.openclassware.elternsprechtag.sprechtag.application.port.in;

import de.openclassware.elternsprechtag.sprechtag.domain.BuchungBereitsStorniertException;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungNichtGefundenException;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagNichtVeroeffentlichtException;
import java.util.UUID;

/**
 * Use Case: der Organizer nimmt eine Buchung zurück — der einzige Storno-Weg im Produkt.
 *
 * <p>Eltern stornieren bewusst nicht selbst: Sie rufen an, der Organizer nimmt die Buchung aus dem
 * Plan (`ABDECKUNG.md`, Phase 3). Das Storno verschickt <b>nichts</b> — kein Ereignis, keine Mail.
 * Der Anlass ist praktisch immer der Anruf der Familie; beim Tippfehler in der Adresse ginge eine
 * Mail erneut an einen Dritten, beim Löschverlangen wäre sie widersinnig.
 */
public interface Stornieren {

  /**
   * Nimmt die Buchung zurück: Sie geht auf {@code STORNIERT}, ihr Termin ist danach wieder buchbar.
   *
   * @throws BuchungNichtGefundenException wenn es die Buchung nicht gibt
   * @throws BuchungBereitsStorniertException wenn sie nicht (mehr) die aktive Buchung ihres Termins
   *     ist — ein zweites Storno derselben Zeile scheitert und lässt einen inzwischen neu gebuchten
   *     Termin unangetastet. Dieselbe Ausnahme trägt den Versionskonflikt, wenn zwei Tabs die Zeile
   *     gleichzeitig stornieren
   * @throws SprechtagNichtVeroeffentlichtException wenn ihr Sprechtag nicht veröffentlicht ist. Die
   *     Prüfung liegt hier und nicht nur in der Oberfläche: Die Auswertungs-Route ist per URL für
   *     jeden Sprechtag-Status erreichbar
   */
  void storniere(UUID buchungId);
}
