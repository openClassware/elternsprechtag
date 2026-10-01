package de.openclassware.elternsprechtag.sprechtag.adapter.in.web;

import de.openclassware.elternsprechtag.config.ElternsprechtagProperties;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen.BuchungsAnfrage;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchungsoptionen;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchungsoptionen.LehrkraftOption;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Sprechtagszugang;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Sprechtagszugang.OeffentlicherSprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.ElternbuchungGeschlossenException;
import de.openclassware.elternsprechtag.sprechtag.domain.TerminBelegtException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
class ElternsprechtagPresenter {

  private final Sprechtagszugang sprechtagszugang;
  private final Buchungsoptionen buchungsoptionen;
  private final Buchen buchenUseCase;
  private final ElternsprechtagProperties properties;

  /** Welcher Screen der Eltern-View aus dem Zugriff folgt. */
  enum Zugang {
    BUCHBAR,
    /** Nicht mehr buchbar, aber der Sprechtag ist bekannt: Kopf und Schulkontakt. */
    HINWEISSEITE,
    NICHT_VERFUEGBAR
  }

  /**
   * Die Texte einer Hinweisseite als Übersetzungs-Keys. Anmeldung beendet (#123), vorbei und
   * abgesagt (#131) teilen den Aufbau und unterscheiden sich nur hier.
   */
  record Hinweisseite(String titelKey, String hinweisKey, String kontaktKey) {}

  /**
   * Ergebnis der Zugriffsprüfung; {@code sprechtag} ist bei {@link Zugang#BUCHBAR} und {@link
   * Zugang#HINWEISSEITE} gesetzt, {@code hinweisseite} nur bei {@link Zugang#HINWEISSEITE}.
   */
  record ZugangsErgebnis(
      Zugang zugang, OeffentlicherSprechtag sprechtag, Hinweisseite hinweisseite) {}

  /**
   * Spiegelt den Zugangsstand des Use Case; ein unbekanntes Token ist nicht verfügbar. Keine der
   * Hinweisseiten gibt Buchungsauskunft — das Token hängt am Sprechtag, nicht an der Familie.
   */
  ZugangsErgebnis pruefeZugang(String accessToken) {
    Optional<OeffentlicherSprechtag> gefunden = sprechtagszugang.oeffne(accessToken);
    if (gefunden.isEmpty()) {
      return new ZugangsErgebnis(Zugang.NICHT_VERFUEGBAR, null, null);
    }
    OeffentlicherSprechtag sprechtag = gefunden.get();
    return switch (sprechtag.stand()) {
      case BUCHBAR -> new ZugangsErgebnis(Zugang.BUCHBAR, sprechtag, null);
      case ANMELDUNG_BEENDET -> hinweisseite(sprechtag, "elternsprechtag.beendet");
      case VORBEI -> hinweisseite(sprechtag, "elternsprechtag.vorbei");
      case ABGESAGT -> hinweisseite(sprechtag, "elternsprechtag.cancelled");
      case NICHT_VERFUEGBAR -> new ZugangsErgebnis(Zugang.NICHT_VERFUEGBAR, null, null);
    };
  }

  private static ZugangsErgebnis hinweisseite(OeffentlicherSprechtag sprechtag, String prefix) {
    return new ZugangsErgebnis(
        Zugang.HINWEISSEITE,
        sprechtag,
        new Hinweisseite(prefix + ".title", prefix + ".hinweis", prefix + ".kontakt"));
  }

  List<LehrkraftOption> ladeLehrkraftOptionen(UUID sprechtagId, UUID klasseId) {
    return buchungsoptionen.ladeLehrkraftOptionen(sprechtagId, klasseId);
  }

  /**
   * Schreibt den Eltern-Submit atomar fest und gibt die Anzahl gebuchter Termine zurück. Wirft
   * {@link TerminBelegtException} und {@link ElternbuchungGeschlossenException}.
   */
  int buchen(BuchungsAnfrage anfrage) {
    return buchenUseCase.buchen(anfrage);
  }

  String getSchoolname() {
    return properties.getSchoolname();
  }
}
