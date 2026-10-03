package de.openclassware.elternsprechtag.sprechtag;

import de.openclassware.elternsprechtag.sprechtag.application.port.out.Benachrichtigungen;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.BuchungsAnsichten;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.BuchungsAnsichten.BelegZeile;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.BuchungsAnsichten.Empfaenger;
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
 * Versand je Adresse zu einer Nachricht, verschickt aber nichts. Eine Adresse in
 * {@link #scheitertFuer} meldet {@link Zustellergebnis#FEHLGESCHLAGEN}, jede andere
 * {@link Zustellergebnis#ABGESCHICKT}.
 *
 * <p>Formulierung und Text prüfen die Versand-Tests unter {@code adapter.out.mail} gegen den echten
 * Adapter; hier geht es nur darum, was der Use Case aus dem Ergebnis macht.
 */
public class FakeBenachrichtigungen implements Benachrichtigungen {

  public final Set<String> scheitertFuer = new HashSet<>();

  private final BuchungsAnsichten buchungsAnsichten;

  public FakeBenachrichtigungen(BuchungsAnsichten buchungsAnsichten) {
    this.buchungsAnsichten = buchungsAnsichten;
  }

  public void reset() {
    scheitertFuer.clear();
  }

  @Override
  public List<Versand> bestaetige(List<BuchungId> buchungen, Anlass anlass) {
    return jeAdresse(buchungen);
  }

  @Override
  public List<Versand> erinnere(List<BuchungId> buchungen) {
    return jeAdresse(buchungen);
  }

  @Override
  public List<Versand> sageAb(SprechtagId sprechtag) {
    Map<String, List<BuchungId>> gruppen = new LinkedHashMap<>();
    for (Empfaenger empfaenger : buchungsAnsichten.aktiveEmpfaenger(sprechtag)) {
      gruppen
          .computeIfAbsent(empfaenger.elternEmail(), k -> new ArrayList<>())
          .add(empfaenger.buchung());
    }
    return versand(gruppen);
  }

  @Override
  public List<Versand> meldeAusfall(List<BuchungId> buchungen, boolean nachbuchbar) {
    return jeAdresse(buchungen);
  }

  private List<Versand> jeAdresse(List<BuchungId> buchungen) {
    Map<String, List<BuchungId>> gruppen = new LinkedHashMap<>();
    for (BelegZeile zeile : buchungsAnsichten.belege(buchungen)) {
      gruppen.computeIfAbsent(zeile.elternEmail(), k -> new ArrayList<>()).add(zeile.buchung());
    }
    return versand(gruppen);
  }

  private List<Versand> versand(Map<String, List<BuchungId>> gruppen) {
    List<Versand> versand = new ArrayList<>();
    gruppen.forEach(
        (adresse, ids) ->
            versand.add(
                new Versand(
                    ids,
                    scheitertFuer.contains(adresse)
                        ? Zustellergebnis.FEHLGESCHLAGEN
                        : Zustellergebnis.ABGESCHICKT)));
    return versand;
  }
}
