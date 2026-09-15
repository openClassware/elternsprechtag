package de.openclassware.elternsprechtag.sprechtag.application.service;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.Stornieren;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Sprechtage;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Termine;
import de.openclassware.elternsprechtag.sprechtag.domain.Buchung;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungBereitsStorniertException;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungId;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungNichtGefundenException;
import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagNichtVeroeffentlichtException;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nimmt eine Buchung zurück — die Gegenbewegung zum Buchen und der Weg, auf dem im Abdeckungs-Maßstab
 * mehrere Fälle ruhen: Eltern-Storno per Anruf, Dubletten, Tippfehler in der Adresse, Löschverlangen
 * vor dem Sprechtag.
 *
 * <p>Der Vorgang <b>verschickt nichts</b>. Das Aggregat meldet zwar {@code BuchungStorniert}; die
 * Meldung wird hier abgeholt und verworfen, statt sie über den Ereignis-Port zu veröffentlichen. Die
 * Entscheidung steht in {@link Stornieren} und ist Teil des Vertrags — eine Storno-Mail wäre der
 * dritte Zweck der Eltern-Adresse und bräuchte einen eigenen ADR (ADR 0002).
 *
 * <p>Drei Vorbedingungen, jede mit eigener Ausnahme: Die Buchung existiert, sie ist die
 * <em>aktive</em> Buchung ihres Termins, und ihr Sprechtag ist veröffentlicht. Die letzte gehört
 * hierher und nicht bloß in die Oberfläche — die Auswertungs-Route ist per URL für jeden
 * Sprechtag-Status erreichbar.
 */
@RequiredArgsConstructor
@Service
class StornierenService implements Stornieren {

  private final Termine termine;
  private final Sprechtage sprechtage;

  @Override
  @Transactional
  public void storniere(UUID buchungId) {
    BuchungId id = BuchungId.von(buchungId);
    Termin termin =
        termine
            .ladeZuBuchung(id)
            .orElseThrow(
                () -> new BuchungNichtGefundenException("Buchung nicht gefunden: " + buchungId));

    // Der Sprechtag wird nur gelesen, verändert wird allein der Termin — die Regel „eine
    // Transaktion, ein Aggregat" bleibt gewahrt.
    Sprechtag sprechtag =
        sprechtage
            .lade(termin.sprechtag())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Termin ohne Sprechtag: " + termin.id().wert()));
    if (sprechtag.status() != SprechtagStatus.VEROEFFENTLICHT) {
      throw new SprechtagNichtVeroeffentlichtException(
          "Storno nur an einem veröffentlichten Sprechtag, nicht bei " + sprechtag.status());
    }

    // Das Aggregat storniert idempotent — es hat keinen Grund, sich über einen zweiten Aufruf zu
    // beschweren. Hier gibt es einen: Nach dem Storno kann eine andere Familie den Slot gebucht
    // haben, und ein zweiter Klick aus einem alten Browser-Tab soll das nicht stillschweigend
    // übergehen. Storniert wird deshalb nur, was gerade die aktive Buchung ist.
    Optional<Buchung> aktive = termin.aktiveBuchung();
    if (aktive.isEmpty() || !aktive.get().id().equals(id)) {
      throw new BuchungBereitsStorniertException(
          "Buchung ist nicht die aktive Buchung ihres Termins: " + buchungId);
    }

    termin.storniere(id);
    try {
      // Sofort speichern, nach dem Muster aus dem Buchen: Ein Versionskonflikt soll hier auftreten
      // und nicht erst beim Commit, wo ihn kein catch mehr übersetzen könnte.
      termine.speichere(termin);
    } catch (OptimisticLockingFailureException konflikt) {
      // Zwei Organizer-Tabs haben dieselbe Zeile gleichzeitig storniert — fachlich derselbe Fall
      // wie der zweite Klick oben, nur eine Spur später bemerkt. Ohne diese Übersetzung stünde der
      // Organizer vor Vaadins Fehlerseite statt vor einer Begründung.
      throw new BuchungBereitsStorniertException(
          "Buchung wurde soeben von anderer Stelle verändert: " + buchungId);
    }
    // Abgeholt und verworfen: Das Storno benachrichtigt niemanden. Ohne dieses Abholen bliebe die
    // Meldung im Puffer eines Aggregats liegen, das ohnehin verbraucht ist — die Zeile steht hier,
    // damit die Absicht sichtbar ist und nicht wie ein Versehen aussieht.
    termin.ereignisseAbholen();
  }
}
