package de.openclassware.elternsprechtag.sprechtag.application.port.out;

import de.openclassware.elternsprechtag.sprechtag.domain.Anlass;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungId;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import de.openclassware.elternsprechtag.sprechtag.domain.Zustellergebnis;
import java.util.List;

/**
 * Ausgang für die Nachrichten an die Familien. Der Adapter dahinter formuliert, bündelt je Kind an
 * einer Adresse (ADR 0007) und versendet — und meldet je Nachricht zurück, welche Buchungen sie trug
 * und wie sie ausging (Issue #110). Was daraus festgehalten wird, entscheidet der Use Case.
 *
 * <p>Jede Methode ist best-effort: Ein Fehlschlag bei einer Nachricht hält die übrigen nicht auf und
 * wirft nicht, sondern steht als {@link Zustellergebnis#FEHLGESCHLAGEN} im Ergebnis. Eine Nachricht,
 * die gar nicht erst entstehen konnte (der Sprechtag ist inzwischen verschwunden), fehlt im Ergebnis.
 */
public interface Benachrichtigungen {

  /** Bestätigt die Buchungen eines Vorgangs mit einer Nachricht an die dort hinterlegte Adresse. */
  List<Versand> bestaetige(List<BuchungId> buchungen, Anlass anlass);

  /** Erinnert an diese Buchungen — je Sprechtag, Adresse und Kind eine Nachricht. */
  List<Versand> erinnere(List<BuchungId> buchungen);

  /** Meldet die Absage an alle aktiven Buchungen dieses Sprechtags, je Adresse und Kind einmal. */
  List<Versand> sageAb(SprechtagId sprechtag);

  /**
   * Meldet den Ausfall dieser (eben entfallenen) Buchungen — je Sprechtag, Adresse und Kind eine
   * Nachricht. Ist {@code nachbuchbar}, führt die Nachricht über den Elternlink zurück in die
   * Buchung.
   */
  List<Versand> meldeAusfall(List<BuchungId> buchungen, boolean nachbuchbar);

  /**
   * Eine Nachricht: die Buchungen, die sie trug, und ihr Ausgang. Ihr Ergebnis gilt für jede dieser
   * Buchungen.
   */
  record Versand(List<BuchungId> buchungen, Zustellergebnis ergebnis) {

    public Versand {
      buchungen = List.copyOf(buchungen);
    }
  }
}
