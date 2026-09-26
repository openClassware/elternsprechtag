package de.openclassware.elternsprechtag.sprechtag.application.service;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Sprechtage;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Termine;
import de.openclassware.elternsprechtag.sprechtag.domain.ElternbuchungGeschlossenException;
import de.openclassware.elternsprechtag.sprechtag.domain.Familie;
import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
import de.openclassware.elternsprechtag.sprechtag.domain.TerminId;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Schreibt einen Eltern-Submit fest — alles oder nichts. Die eigentliche Mechanik (Aggregate laden,
 * Zeitkonflikt prüfen, sofort speichern, Ereignisse bündeln) liegt in {@link
 * BuchungsVorgangService} und wird sich mit dem Organizer-Nachtrag geteilt; dieser Service trägt
 * nur die Vorbedingung des Eltern-Submits: Der Elternlink nimmt noch Buchungen an (Issue #122).
 *
 * <p>Die Prüfung sitzt hier und nicht nur beim Öffnen der Seite, weil zwischen Öffnen und Abschicken
 * Zeit vergeht: Mitternacht kann den Anmeldeschluss überschreiten, der Organizer kann absagen. Sie
 * läuft wie im {@code NachtragenService} <b>vor</b> der Zeitkonflikt-Prüfung — die geschlossene
 * Anmeldung ist der grundsätzlichere Fehler.
 */
@RequiredArgsConstructor
@Service
class BuchenService implements Buchen {

  private final BuchungsVorgangService vorgang;
  private final Termine termine;
  private final Sprechtage sprechtage;

  @Override
  @Transactional
  public int buchen(BuchungsAnfrage anfrage) {
    Familie familie =
        new Familie(anfrage.elternName(), anfrage.schuelerName(), anfrage.elternEmail());
    List<BuchungsVorgangService.Wunsch> wuensche = wuensche(anfrage.wuensche());
    // Vor jedem Schreiben geprüft — die Ablehnung darf keine halbe Buchung, kein Ereignis und
    // keine Mail hinterlassen.
    pruefeElternbuchungOffen(wuensche);
    List<Termin> geladen = vorgang.ladeUndPruefeZeitkonflikt(wuensche);
    return vorgang.schreibeFest(familie, wuensche, geladen);
  }

  /**
   * Fragt für jeden Wunsch den Sprechtag seines Termins. Mehrfaches Laden desselben Sprechtags ist
   * wie im {@code NachtragenService} hingenommen: Ein Submit trägt wenige Wünsche.
   */
  private void pruefeElternbuchungOffen(List<BuchungsVorgangService.Wunsch> wuensche) {
    LocalDate heute = LocalDate.now();
    for (BuchungsVorgangService.Wunsch wunsch : wuensche) {
      Termin termin =
          termine
              .lade(TerminId.von(wunsch.terminId()))
              .orElseThrow(
                  () -> new IllegalArgumentException("Termin nicht gefunden: " + wunsch.terminId()));
      Sprechtag sprechtag =
          sprechtage
              .lade(termin.sprechtag())
              .orElseThrow(
                  () -> new IllegalStateException("Termin ohne Sprechtag: " + termin.id().wert()));
      if (!sprechtag.nimmtElternbuchungenAn(heute)) {
        throw new ElternbuchungGeschlossenException(
            "Der Elternlink nimmt keine Buchung mehr an — Status "
                + sprechtag.status()
                + ", Anmeldeschluss "
                + sprechtag.anmeldeschluss());
      }
    }
  }

  private static List<BuchungsVorgangService.Wunsch> wuensche(List<BuchungsWunsch> wuensche) {
    return wuensche.stream()
        .map(
            wunsch ->
                new BuchungsVorgangService.Wunsch(
                    wunsch.lehrauftragId(), wunsch.terminId(), wunsch.notiz()))
        .toList();
  }
}
