package de.openclassware.elternsprechtag.sprechtag;

import de.openclassware.elternsprechtag.sprechtag.application.port.out.Benachrichtigungen;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.BuchungsAnsichten;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.BuchungsAnsichten.BelegZeile;
import de.openclassware.elternsprechtag.sprechtag.domain.Anlass;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungId;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import de.openclassware.elternsprechtag.sprechtag.domain.Zustellergebnis;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Test-Attrappe des {@link Benachrichtigungen}-Ports für die Use-Case-Tests: bündelt wie der echte
 * Versand je Adresse und Kind zu einer Nachricht (ADR 0007), verschickt aber nichts. Eine Adresse in
 * {@link #scheitertFuer} meldet {@link Zustellergebnis#FEHLGESCHLAGEN}, jede andere
 * {@link Zustellergebnis#ABGESCHICKT}.
 *
 * <p>Formulierung und Text prüfen die Versand-Tests unter {@code adapter.out.mail} gegen den echten
 * Adapter; hier geht es nur darum, was der Use Case aus dem Ergebnis macht.
 */
public class FakeBenachrichtigungen implements Benachrichtigungen {

  public final Set<String> scheitertFuer = new HashSet<>();

  private final BuchungsAnsichten buchungsAnsichten;

  private record Empfaenger(String elternEmail, String schuelerName, String klasse) {}

  public FakeBenachrichtigungen(BuchungsAnsichten buchungsAnsichten) {
    this.buchungsAnsichten = buchungsAnsichten;
  }

  public void reset() {
    scheitertFuer.clear();
  }

  @Override
  public List<Versand> bestaetige(List<BuchungId> buchungen, Anlass anlass) {
    return jeEmpfaenger(buchungsAnsichten.belege(buchungen));
  }

  @Override
  public List<Versand> erinnere(List<BuchungId> buchungen) {
    return jeEmpfaenger(buchungsAnsichten.belege(buchungen));
  }

  @Override
  public List<Versand> sageAb(SprechtagId sprechtag) {
    return jeEmpfaenger(buchungsAnsichten.aktiveBelege(sprechtag));
  }

  @Override
  public List<Versand> meldeAusfall(List<BuchungId> buchungen, boolean nachbuchbar) {
    return jeEmpfaenger(buchungsAnsichten.belege(buchungen));
  }

  private List<Versand> jeEmpfaenger(List<BelegZeile> zeilen) {
    Map<Empfaenger, List<BuchungId>> gruppen = new LinkedHashMap<>();
    for (BelegZeile zeile : zeilen) {
      gruppen
          .computeIfAbsent(
              new Empfaenger(zeile.elternEmail(), zeile.schuelerName(), zeile.klasse()),
              k -> new ArrayList<>())
          .add(zeile.buchung());
    }
    List<Versand> versand = new ArrayList<>();
    gruppen.forEach(
        (empfaenger, ids) ->
            versand.add(
                new Versand(
                    ids,
                    scheitertFuer.contains(empfaenger.elternEmail())
                        ? Zustellergebnis.FEHLGESCHLAGEN
                        : Zustellergebnis.ABGESCHICKT)));
    return versand;
  }
}
