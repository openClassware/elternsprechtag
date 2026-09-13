package de.openclassware.elternsprechtag.sprechtag.application.service;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.Absagen;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Abschliessen;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Materialisieren;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Veroeffentlichen;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.ZurueckAufEntwurf;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.BuchungsAnsichten;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Ereignisse;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Sprechtage;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Termine;
import de.openclassware.elternsprechtag.sprechtag.domain.Ereignis;
import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagHatBuchungenException;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Die Statuswechsel eines Sprechtags: veröffentlichen, absagen, abschließen, zurücknehmen.
 *
 * <p>Was erlaubt ist, entscheidet das Aggregat — dieser Service führt nur die Folgen aus, die
 * außerhalb davon liegen: die Termine herstellen, die Termine verwerfen, die Ereignisse
 * veröffentlichen. Die Ereignisse werden dem Aggregat <em>nach</em> dem Speichern abgeholt; rollt
 * die Transaktion zurück, wird nie zugestellt.
 */
@RequiredArgsConstructor
@Service
class SprechtagLebenszyklusService
    implements Veroeffentlichen, Absagen, Abschliessen, ZurueckAufEntwurf {

  private final Sprechtage sprechtage;
  private final Termine termine;
  private final Materialisieren materialisieren;
  private final Ereignisse ereignisse;
  private final BuchungsAnsichten buchungsAnsichten;

  @Override
  @Transactional
  public Ergebnis veroeffentliche(UUID id) {
    Sprechtag sprechtag = lade(id);
    sprechtag.veroeffentliche();
    sprechtage.speichere(sprechtag);
    // Erst nach dem Speichern: Die Materialisierung lädt den Sprechtag erneut und soll ihn
    // veröffentlicht vorfinden — ein Aggregat ist nach dem Speichern verbraucht.
    int erzeugte = materialisieren.materialisiere(id);
    veroeffentliche(sprechtag.ereignisseAbholen());
    return new Ergebnis(erzeugte);
  }

  @Override
  @Transactional
  public void sageAb(UUID id) {
    Sprechtag sprechtag = lade(id);
    sprechtag.sageAb();
    sprechtage.speichere(sprechtag);
    veroeffentliche(sprechtag.ereignisseAbholen());
  }

  @Override
  @Transactional(readOnly = true)
  public long zaehleBetroffeneEltern(UUID id) {
    return buchungsAnsichten.zaehleAktiveElternAdressen(SprechtagId.von(id));
  }

  @Override
  @Transactional
  public void schliesseAb(UUID id) {
    Sprechtag sprechtag = lade(id);
    sprechtag.schliesseAb();
    sprechtage.speichere(sprechtag);
    veroeffentliche(sprechtag.ereignisseAbholen());
  }

  @Override
  @Transactional
  public void nimmZurueck(UUID id) {
    SprechtagId sprechtagId = SprechtagId.von(id);
    Sprechtag sprechtag = lade(id);
    // Ob gebucht wurde, steht in den Termin-Aggregaten; die Antwort geht als Frage ins Aggregat, die
    // Entscheidung fällt dort.
    sprechtag.nimmVeroeffentlichungZurueck(termine.wurdeGebucht(sprechtagId));
    sprechtage.speichere(sprechtag);
    // Die Termine verlieren mit der Veröffentlichung ihre Grundlage. Sie stehen zu lassen hieße,
    // beim nächsten Veröffentlichen auf Slots zu treffen, die zu einer alten Zeitstruktur gehören —
    // die Materialisierung ist idempotent und erzeugte dann gar nichts Neues.
    try {
      termine.entferneFuer(sprechtagId);
    } catch (DataIntegrityViolationException gebucht) {
      // Zwischen der Frage oben und dem Löschen hier liegt keine Sperre über den Terminen: Eine
      // Familie kann in genau diesem Moment gebucht haben. Der Fremdschlüssel fängt das ab — und
      // fachlich ist es derselbe Fall, also bekommt der Organizer dieselbe Antwort.
      throw new SprechtagHatBuchungenException(
          "An diesem Sprechtag wurde soeben gebucht. Er lässt sich nicht mehr zum Entwurf "
              + "zurücknehmen — wer ihn nicht halten kann, sagt ihn ab.");
    }
    veroeffentliche(sprechtag.ereignisseAbholen());
  }

  private void veroeffentliche(List<Ereignis> gemeldete) {
    gemeldete.forEach(ereignisse::veroeffentliche);
  }

  private Sprechtag lade(UUID id) {
    return sprechtage
        .lade(SprechtagId.von(id))
        .orElseThrow(() -> new IllegalArgumentException("Sprechtag nicht gefunden: " + id));
  }
}
