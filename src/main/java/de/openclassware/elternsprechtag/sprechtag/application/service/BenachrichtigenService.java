package de.openclassware.elternsprechtag.sprechtag.application.service;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.Benachrichtigen;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Benachrichtigungen;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Benachrichtigungen.Versand;
import de.openclassware.elternsprechtag.sprechtag.domain.AusfallErfasst;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungenBestaetigt;
import de.openclassware.elternsprechtag.sprechtag.domain.ErinnerungFaellig;
import de.openclassware.elternsprechtag.sprechtag.domain.Mailart;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagAbgesagt;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Versendet über {@link Benachrichtigungen} und hält den Ausgang je Buchung fest (Issue #110) — der
 * erste Weg, auf dem der Versand zurückschreibt.
 *
 * <p>Bewusst <b>ohne</b> umschließende Transaktion: Der Versand soll keine Datenbankverbindung
 * halten, während er auf den Mailserver wartet (JavaMail wartet voreingestellt unbegrenzt). Das
 * Festhalten danach läuft in einer eigenen Transaktion ({@link ZustellungsVermerkService}).
 */
@RequiredArgsConstructor
@Service
class BenachrichtigenService implements Benachrichtigen {

  private final Benachrichtigungen benachrichtigungen;
  private final ZustellungsVermerkService vermerk;

  @Override
  public void bestaetige(BuchungenBestaetigt vorgang) {
    vermerke(
        Mailart.BESTAETIGUNG,
        benachrichtigungen.bestaetige(vorgang.buchungen(), vorgang.anlass()));
  }

  @Override
  public void erinnere(ErinnerungFaellig vorgang) {
    vermerke(Mailart.ERINNERUNG, benachrichtigungen.erinnere(vorgang.buchungen()));
  }

  @Override
  public void sageAb(SprechtagAbgesagt vorgang) {
    vermerke(Mailart.ABSAGE, benachrichtigungen.sageAb(vorgang.sprechtag()));
  }

  @Override
  public void meldeAusfall(AusfallErfasst vorgang) {
    vermerke(Mailart.AUSFALL, benachrichtigungen.meldeAusfall(vorgang.buchungen()));
  }

  private void vermerke(Mailart art, List<Versand> versand) {
    if (!versand.isEmpty()) {
      vermerk.vermerke(art, versand);
    }
  }
}
