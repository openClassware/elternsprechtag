package de.openclassware.elternsprechtag.sprechtag.adapter.out.mail;

import de.openclassware.elternsprechtag.sprechtag.application.port.out.Benachrichtigungen;
import de.openclassware.elternsprechtag.sprechtag.domain.Anlass;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungId;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Erfüllt den {@link Benachrichtigungen}-Port per E-Mail. Reicht nur durch: Jede Mailart hat ihren
 * eigenen Fachservice, der formuliert, je Adresse bündelt, versendet und den Ausgang meldet. Was
 * davon festgehalten wird, entscheidet der Use Case — dieser Adapter kennt keine Persistenz.
 */
@RequiredArgsConstructor
@Component
class MailBenachrichtigungen implements Benachrichtigungen {

  private final BuchungBestaetigungService bestaetigung;
  private final ErinnerungBenachrichtigungService erinnerung;
  private final AbsageBenachrichtigungService absage;
  private final AusfallBenachrichtigungService ausfall;

  @Override
  public List<Versand> bestaetige(List<BuchungId> buchungen, Anlass anlass) {
    return bestaetigung.bestaetige(buchungen, anlass);
  }

  @Override
  public List<Versand> erinnere(List<BuchungId> buchungen) {
    return erinnerung.erinnere(buchungen);
  }

  @Override
  public List<Versand> sageAb(SprechtagId sprechtag) {
    return absage.benachrichtige(sprechtag);
  }

  @Override
  public List<Versand> meldeAusfall(List<BuchungId> buchungen) {
    return ausfall.benachrichtige(buchungen);
  }
}
