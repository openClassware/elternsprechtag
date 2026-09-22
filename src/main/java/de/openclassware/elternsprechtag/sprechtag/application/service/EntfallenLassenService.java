package de.openclassware.elternsprechtag.sprechtag.application.service;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.EntfallenLassen;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Ereignisse;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Sprechtage;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Termine;
import de.openclassware.elternsprechtag.sprechtag.application.service.TerminAusfallService.Ausfall;
import de.openclassware.elternsprechtag.sprechtag.domain.AusfallGemeldet;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungId;
import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagNichtVeroeffentlichtException;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
import de.openclassware.elternsprechtag.sprechtag.domain.TerminId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Der Ausfall-Vorgang (#156): lässt eine Auswahl von Terminen entfallen — jeden in seiner eigenen
 * Transaktion über {@link TerminAusfallService} — und bündelt die dabei stornierten Buchungen zu
 * <em>einem</em> {@link AusfallGemeldet} für den späteren Mailversand.
 *
 * <p>Derselbe Zuschnitt wie im Erinnerungs-Scheduler, und aus demselben Grund: Diese Methode bleibt
 * {@code @Transactional} nicht für die Termine — die laufen in ihrer eigenen —, sondern damit
 * {@link Ereignisse#veroeffentliche} innerhalb einer aktiven Transaktion geschieht. Nur dann feuert
 * ein {@code @TransactionalEventListener(AFTER_COMMIT)}, und nur dann beginnt kein Versand, bevor
 * der Vorgang festgeschrieben ist.
 *
 * <p>Der Zähler steht bewusst nicht auf der übergebenen Auswahl: Gezählt wird, was das jeweilige
 * Aggregat tatsächlich geändert hat.
 */
@RequiredArgsConstructor
@Service
@Slf4j
class EntfallenLassenService implements EntfallenLassen {

  private final TerminAusfallService ausfaelle;
  private final Termine termine;
  private final Sprechtage sprechtage;
  private final Ereignisse ereignisse;

  @Override
  @Transactional
  public Ausfallergebnis lassEntfallen(List<UUID> terminIds) {
    List<TerminId> auswahl = terminIds.stream().map(TerminId::von).distinct().toList();
    pruefeVeroeffentlicht(auswahl);

    List<BuchungId> storniert = new ArrayList<>();
    // Die Adresse ist der Bündelungsschlüssel der Benachrichtigung — eine Familie mit zwei
    // entfallenden Terminen bekommt eine Nachricht und zählt hier einmal.
    Set<String> adressen = new LinkedHashSet<>();
    int entfallen = 0;

    for (TerminId terminId : auswahl) {
      Optional<Ausfall> ausfall;
      try {
        ausfall = ausfaelle.lassEntfallen(terminId);
      } catch (RuntimeException fehler) {
        log.warn("Termin {} konnte nicht entfallen: {}", terminId.wert(), fehler.getMessage());
        continue;
      }
      if (ausfall.isEmpty()) {
        // Unbekannt oder längst entfallen — still übersprungen, kein zweites Ereignis.
        continue;
      }
      entfallen++;
      if (ausfall.get().trafEineFamilie()) {
        storniert.add(ausfall.get().stornierteBuchung());
        adressen.add(ausfall.get().elternAdresse());
      }
    }

    if (!storniert.isEmpty()) {
      ereignisse.veroeffentliche(new AusfallGemeldet(storniert));
    }
    return new Ausfallergebnis(entfallen, adressen.size());
  }

  /**
   * Die Vorbedingung des ganzen Vorgangs — geprüft, <b>bevor</b> der erste Termin geschrieben ist.
   *
   * <p>Dass sie vor der Schleife steht und nicht in ihr, ist der Punkt: Eine Abweisung mitten im
   * Lauf ließe die zuvor bearbeiteten Termine samt ihrer Stornos festgeschrieben zurück — jede
   * Einzeltransaktion hat ja schon committet —, während das gebündelte Ereignis nie veröffentlicht
   * würde. Familien stünden vor einer Absage, von der sie nichts erfahren; genau die Lücke, die
   * dieser Vorgang schließen soll.
   *
   * <p>Sie geht der Einzelfall-Nachsicht vor: Ein bereits entfallener Termin an einem nicht
   * veröffentlichten Sprechtag wird abgewiesen, nicht still übersprungen. Erst wenn der Sprechtag
   * stimmt, ist die Auswahl überhaupt eine Auswahl.
   *
   * <p>Der Preis ist ein zweites Laden der Aggregate — bei der Auswahl eines Dialogs eine Handvoll
   * Zeilen. Der Sprechtag wird dabei nur gelesen; geschrieben wird allein am Termin, je in seiner
   * eigenen Transaktion.
   */
  private void pruefeVeroeffentlicht(List<TerminId> auswahl) {
    Set<SprechtagId> geprueft = new HashSet<>();
    for (TerminId terminId : auswahl) {
      Optional<Termin> termin;
      try {
        termin = termine.lade(terminId);
      } catch (RuntimeException fehler) {
        // Was sich nicht laden lässt, trägt auch keinen Sprechtag bei. Protokolliert wird es
        // gleich in der Schleife, die über denselben Termin stolpert — hier zweimal zu warnen
        // machte aus einem Fehler zwei.
        continue;
      }
      // Unbekannte Ids werden still übersprungen, und jeder Sprechtag wird nur einmal geladen:
      // Eine Auswahl aus einem Dialog stammt in aller Regel ohnehin von genau einem.
      if (termin.isEmpty() || !geprueft.add(termin.get().sprechtag())) {
        continue;
      }
      Sprechtag sprechtag =
          sprechtage
              .lade(termin.get().sprechtag())
              .orElseThrow(
                  () -> new IllegalStateException("Termin ohne Sprechtag: " + terminId.wert()));
      if (sprechtag.status() != SprechtagStatus.VEROEFFENTLICHT) {
        throw new SprechtagNichtVeroeffentlichtException(
            "Termine entfallen nur an einem veröffentlichten Sprechtag, nicht bei "
                + sprechtag.status());
      }
    }
  }
}
