package de.openclassware.elternsprechtag.sprechtag.adapter.in.web;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten.BuchungsZeile;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten.LehrkraftPlan;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Die Namenssuche der Auswertung (Issue #121) — der Weg drumherum für die bewusst fehlende
 * Elternsicht: Ruft eine Familie an und fragt nach ihrem Termin, findet der Organizer sie über den
 * Namen.
 *
 * <p>Der Begriff zerfällt in Wörter; eine Zeile passt, wenn <em>jedes</em> Wort als Teilstring im
 * Schüler- oder Elternnamen vorkommt, ohne Unterschied zwischen Groß- und Kleinschreibung. So findet
 * „Lena Müller" auch „Müller, Lena" — das Namensfeld schreibt keine Reihenfolge vor. Umlaute werden
 * bewusst nicht angeglichen.
 *
 * <p>Bewusst <b>Vaadin-frei</b> und ohne Zustand, damit die Regel ohne UI unit-testbar ist. Den
 * Begriff hält die View, die Entscheidungen für die Anzeige trifft der {@link AuswertungPresenter}.
 */
final class Namenssuche {

  private static final Namenssuche KEINE = new Namenssuche("", List.of());

  private final String begriff;
  private final List<String> woerter;

  private Namenssuche(String begriff, List<String> woerter) {
    this.begriff = begriff;
    this.woerter = woerter;
  }

  /** Die Suche zu einem eingegebenen Begriff; {@code null} oder nur Leerzeichen heißt: keine. */
  static Namenssuche nach(String eingabe) {
    String begriff = eingabe == null ? "" : eingabe.strip();
    if (begriff.isEmpty()) {
      return KEINE;
    }
    return new Namenssuche(
        begriff, Arrays.stream(begriff.split("\\s+")).map(Namenssuche::normiert).toList());
  }

  /** Ob überhaupt gesucht wird — ohne Begriff bleibt jede Liste, wie sie ist. */
  boolean istAktiv() {
    return !woerter.isEmpty();
  }

  /** Der Begriff, wie er im Hinweis „Keine Buchung passt zu …" steht — ohne Randleerzeichen. */
  String begriff() {
    return begriff;
  }

  boolean passt(BuchungsZeile zeile) {
    // Ein Wort enthält kein Leerzeichen und kann deshalb nicht über die Fuge zwischen beiden Namen
    // hinweg treffen.
    String namen =
        normiert(Objects.toString(zeile.schuelerName(), "")
            + " "
            + Objects.toString(zeile.elternName(), ""));
    return woerter.stream().allMatch(namen::contains);
  }

  /**
   * Die Pläne mit nur noch den passenden Zeilen — geltende wie stornierte. Ein Plan bleibt stehen,
   * solange er eine <em>sichtbare</em> passende Zeile hat; stornierte zählen dafür nur, wenn sie
   * angezeigt werden. Die Kopfzahlen ({@code anzahl}, {@code entfalleneAnzahl}) bleiben unverändert:
   * Sie beschreiben die Auslastung der Lehrkraft, nicht die Trefferliste.
   */
  List<LehrkraftPlan> filtere(List<LehrkraftPlan> plaene, boolean stornierteAnzeigen) {
    if (!istAktiv()) {
      return plaene;
    }
    return plaene.stream()
        .map(this::nurTreffer)
        .filter(plan -> !plan.zeilen().isEmpty() || (stornierteAnzeigen && !plan.stornierte().isEmpty()))
        .toList();
  }

  /** Wie viele stornierte Buchungen in diesen Plänen passen — für den Hinweis bei ausgeblendeten. */
  int stornierteTreffer(List<LehrkraftPlan> plaene) {
    if (!istAktiv()) {
      return 0;
    }
    return (int) plaene.stream().flatMap(plan -> plan.stornierte().stream()).filter(this::passt).count();
  }

  private LehrkraftPlan nurTreffer(LehrkraftPlan plan) {
    return new LehrkraftPlan(
        plan.lehrerId(),
        plan.kuerzel(),
        plan.anzeigeName(),
        plan.anzahl(),
        plan.entfalleneAnzahl(),
        plan.zeilen().stream().filter(this::passt).toList(),
        plan.stornierte().stream().filter(this::passt).toList());
  }

  private static String normiert(String text) {
    return text.toLowerCase(Locale.GERMAN);
  }
}
