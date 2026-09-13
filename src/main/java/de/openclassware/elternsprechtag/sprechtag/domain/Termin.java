package de.openclassware.elternsprechtag.sprechtag.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Ein Slot einer Lehrkraft an einem Sprechtag — Aggregat-Wurzel über ihre {@link Buchung}en.
 *
 * <p>Der Schnitt folgt der Invariante, nicht dem Lebenszyklus: <b>ein Slot, höchstens eine aktive
 * Buchung</b> liegt damit innerhalb einer Konsistenzgrenze und hat in diesem Objekt seinen Hüter.
 * Der Termin liegt bewusst <em>nicht</em> im Sprechtag, obwohl er daraus entsteht — ein Lock über
 * alle rund 600 Termine eines Sprechtags würde jede buchende Familie serialisieren (ADR 0003).
 *
 * <p>Es gibt keinen gespeicherten Zustand „belegt": Das ist die Frage, ob eine aktive Buchung
 * existiert, und wird abgeleitet. Gespeichert ist nur die {@link Verfuegbarkeit} — die Absicht des
 * Organizers, diesen Slot anzubieten oder nicht.
 *
 * <p>Sprechtag und Lehrkraft stehen als {@link SprechtagId} bzw. {@link LehrkraftId} darin; es gibt
 * keine Objektnavigation über eine Aggregat-Grenze.
 */
public final class Termin extends AggregateRoot {

  private final TerminId id;
  private final SprechtagId sprechtag;
  private final LehrkraftId lehrkraft;
  private final Zeitraum zeitraum;
  private final List<Buchung> buchungen;
  private Verfuegbarkeit verfuegbarkeit;

  /**
   * Der Stand des optimistischen Sperrens am Root, wie er geladen wurde. {@code 0} heißt „noch nie
   * gespeichert". Das Aggregat zählt nicht selbst hoch — das tut der Persistenz-Adapter; hier steht
   * der Wert nur, damit er beim Speichern mitgeht (ADR 0004, ADR 0005).
   */
  private final long version;

  private Termin(
      TerminId id,
      SprechtagId sprechtag,
      LehrkraftId lehrkraft,
      Zeitraum zeitraum,
      Verfuegbarkeit verfuegbarkeit,
      long version,
      List<Buchung> buchungen) {
    this.id = Objects.requireNonNull(id, "id");
    this.sprechtag = Objects.requireNonNull(sprechtag, "sprechtag");
    this.lehrkraft = Objects.requireNonNull(lehrkraft, "lehrkraft");
    this.zeitraum = Objects.requireNonNull(zeitraum, "zeitraum");
    this.verfuegbarkeit = Objects.requireNonNull(verfuegbarkeit, "verfuegbarkeit");
    this.version = version;
    this.buchungen = new ArrayList<>(buchungen);
  }

  /** Ein frisch materialisierter, angebotener Slot ohne Buchung. */
  public static Termin neu(SprechtagId sprechtag, LehrkraftId lehrkraft, Zeitraum zeitraum) {
    return new Termin(
        TerminId.neu(), sprechtag, lehrkraft, zeitraum, Verfuegbarkeit.VERFUEGBAR, 0L, List.of());
  }

  /**
   * Setzt einen gespeicherten Termin unverändert wieder zusammen — für den Persistenz-Adapter.
   * Prüft keine Invarianten und meldet kein Ereignis.
   */
  public static Termin rekonstruiere(
      TerminId id,
      SprechtagId sprechtag,
      LehrkraftId lehrkraft,
      Zeitraum zeitraum,
      Verfuegbarkeit verfuegbarkeit,
      long version,
      List<Buchung> buchungen) {
    return new Termin(id, sprechtag, lehrkraft, zeitraum, verfuegbarkeit, version, buchungen);
  }

  /**
   * Sagt diesen Slot der Familie zu und meldet {@link BuchungAngelegt}.
   *
   * <p>Hier liegt die Kernregel: Ein Slot mit aktiver Buchung ist vergeben
   * ({@link TerminBelegtException}), ein entfallender Slot wird nicht gebucht
   * ({@link TerminEntfaelltException}), und das Ziel muss dieselbe Lehrkraft nennen wie der Termin
   * ({@link FremdeLehrkraftException}). Geprüft wird beim Schreiben, nicht beim Anzeigen — zwischen
   * „frei" in der Ansicht und dem Submit liegt beliebig viel Zeit.
   *
   * <p>Eine stornierte Buchung bleibt liegen und gibt den Slot frei; über die Zeit trägt ein Termin
   * deshalb mehrere Buchungen, aber höchstens eine aktive.
   *
   * @param notiz darf {@code null} sein
   * @return die Id der neuen Buchung — sie steht sofort fest, weil die Domäne ihre Ids selbst
   *     vergibt
   */
  public BuchungId buche(Familie familie, Buchungsziel ziel, Notiz notiz, LocalDateTime jetzt) {
    Objects.requireNonNull(ziel, "ziel");
    if (verfuegbarkeit == Verfuegbarkeit.ENTFAELLT) {
      throw new TerminEntfaelltException("Dieser Termin entfällt: " + zeitraum.beginn());
    }
    if (aktiveBuchung().isPresent()) {
      throw new TerminBelegtException("Termin ist bereits vergeben: " + zeitraum.beginn());
    }
    if (!lehrkraft.equals(ziel.lehrkraft())) {
      throw new FremdeLehrkraftException(
          "Termin und Buchungsziel gehören zu unterschiedlichen Lehrkräften.");
    }
    BuchungId buchungId = BuchungId.neu();
    buchungen.add(Buchung.zugesagt(buchungId, jetzt, familie, ziel, notiz));
    melde(new BuchungAngelegt(id, buchungId));
    return buchungId;
  }

  /**
   * Nimmt eine Zusage zurück und meldet {@link BuchungStorniert}. Eine bereits stornierte Buchung
   * bleibt wie sie ist und meldet nichts — dreimal stornieren verschickt nicht dreimal etwas.
   *
   * @throws BuchungNichtGefundenException wenn die Buchung nicht zu diesem Termin gehört
   */
  public void storniere(BuchungId buchungId) {
    Buchung buchung =
        buchungen.stream()
            .filter(kandidat -> kandidat.id().equals(buchungId))
            .findFirst()
            .orElseThrow(
                () ->
                    new BuchungNichtGefundenException(
                        "Buchung gehört nicht zu diesem Termin: " + buchungId.wert()));
    if (buchung.storniere()) {
      melde(new BuchungStorniert(id, buchungId));
    }
  }

  /**
   * Nimmt den Slot aus dem Angebot — Absicht des Organizers, keine Folge einer Buchung.
   *
   * <p>Nur für einen Slot ohne aktive Buchung. Was mit einer daran hängenden Buchung geschehen muss,
   * ist beschlossen (`ABDECKUNG.md`: die Buchung wird storniert und die Familie erfährt davon), aber
   * noch nicht gebaut — es ist die Ausfall-Strecke. Bis dahin verweigert das Aggregat den Übergang,
   * statt einen Zustand zuzulassen, dessen Folgen niemand ausführt: ein entfallender Termin mit
   * einer Familie, die weiter auf ihre Zusage vertraut.
   */
  public void lassEntfallen() {
    if (aktiveBuchung().isPresent()) {
      throw new TerminHatBuchungException(
          "Ein Termin mit aktiver Buchung kann noch nicht entfallen: " + zeitraum.beginn());
    }
    verfuegbarkeit = Verfuegbarkeit.ENTFAELLT;
  }

  /** Angeboten und frei. Abgeleitet, nicht gespeichert. */
  public boolean istBuchbar() {
    return verfuegbarkeit == Verfuegbarkeit.VERFUEGBAR && aktiveBuchung().isEmpty();
  }

  /** Die höchstens eine aktive Buchung dieses Slots. */
  public Optional<Buchung> aktiveBuchung() {
    return buchungen.stream().filter(Buchung::istAktiv).findFirst();
  }

  public TerminId id() {
    return id;
  }

  public SprechtagId sprechtag() {
    return sprechtag;
  }

  public LehrkraftId lehrkraft() {
    return lehrkraft;
  }

  public Zeitraum zeitraum() {
    return zeitraum;
  }

  public Verfuegbarkeit verfuegbarkeit() {
    return verfuegbarkeit;
  }

  public long version() {
    return version;
  }

  /** Alle Buchungen dieses Slots, aktive und stornierte, in der Reihenfolge ihres Entstehens. */
  public List<Buchung> buchungen() {
    return List.copyOf(buchungen);
  }
}
