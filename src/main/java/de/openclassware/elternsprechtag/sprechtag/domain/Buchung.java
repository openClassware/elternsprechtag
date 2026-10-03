package de.openclassware.elternsprechtag.sprechtag.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Eine Buchung auf einem Slot — <em>innere Entity</em> von {@link Termin}, kein eigenes Aggregat
 * (ADR 0003). Sie wird nur über ihren Root erzeugt und verändert; nach außen ist sie lesbar.
 *
 * <p>Alles, was sie zum Anzeigen braucht, trägt sie selbst: {@link Familie} und
 * {@link Buchungsziel} sind Stand zum Buchungszeitpunkt, kein Verweis in fremde Stammdaten.
 */
public final class Buchung {

  private final BuchungId id;
  private final LocalDateTime erstelltAm;
  private Familie familie;
  private final Buchungsziel ziel;
  private Notiz notiz;
  private Buchungsstatus status;

  /**
   * Wann die Erinnerung <em>vorgemerkt</em> wurde — gesetzt vor dem Versand, als Schutz gegen einen
   * zweiten. Ob sie die Familie erreicht hat, steht nicht hier, sondern in der {@link Zustellung}
   * der Art {@link Mailart#ERINNERUNG} (Issue #110). Der Name ist älter als diese Unterscheidung.
   */
  private LocalDateTime erinnerungVersendetAm;

  /** Wann Familie und Notiz einem Pseudonym gewichen sind; {@code null} heißt: noch nicht. */
  private LocalDateTime anonymisiertAm;

  private Buchung(
      BuchungId id,
      LocalDateTime erstelltAm,
      Familie familie,
      Buchungsziel ziel,
      Notiz notiz,
      Buchungsstatus status,
      LocalDateTime erinnerungVersendetAm,
      LocalDateTime anonymisiertAm) {
    this.id = Objects.requireNonNull(id, "id");
    this.erstelltAm = Objects.requireNonNull(erstelltAm, "erstelltAm");
    this.familie = Objects.requireNonNull(familie, "familie");
    this.ziel = Objects.requireNonNull(ziel, "ziel");
    this.status = Objects.requireNonNull(status, "status");
    this.notiz = notiz;
    this.erinnerungVersendetAm = erinnerungVersendetAm;
    this.anonymisiertAm = anonymisiertAm;
  }

  /**
   * Frisch zugesagte Buchung. Nur für {@link Termin} — die Invariante „ein Slot, höchstens eine
   * aktive Buchung" prüft der Root, und er ist die einzige Stelle, die das kann.
   *
   * @param notiz darf {@code null} sein
   */
  static Buchung zugesagt(
      BuchungId id, LocalDateTime erstelltAm, Familie familie, Buchungsziel ziel, Notiz notiz) {
    return new Buchung(id, erstelltAm, familie, ziel, notiz, Buchungsstatus.ZUGESAGT, null, null);
  }

  /**
   * Setzt eine gespeicherte Buchung unverändert wieder zusammen — für den Persistenz-Adapter. Prüft
   * keine Invarianten: Was in der Datenbank steht, ist schon geschehen.
   *
   * @param notiz darf {@code null} sein
   * @param erinnerungVersendetAm darf {@code null} sein — dann ist noch keine Erinnerung
   *     vorgemerkt worden
   * @param anonymisiertAm darf {@code null} sein — dann trägt die Buchung noch die Angaben der
   *     Familie
   */
  public static Buchung rekonstruiere(
      BuchungId id,
      LocalDateTime erstelltAm,
      Familie familie,
      Buchungsziel ziel,
      Notiz notiz,
      Buchungsstatus status,
      LocalDateTime erinnerungVersendetAm,
      LocalDateTime anonymisiertAm) {
    return new Buchung(
        id, erstelltAm, familie, ziel, notiz, status, erinnerungVersendetAm, anonymisiertAm);
  }

  /** Nimmt die Zusage zurück; gibt zurück, ob sich dadurch etwas geändert hat. */
  boolean storniere() {
    if (status == Buchungsstatus.STORNIERT) {
      return false;
    }
    status = Buchungsstatus.STORNIERT;
    return true;
  }

  /**
   * Merkt die Erinnerung vor; gibt zurück, ob sich dadurch etwas geändert hat. Nur für
   * {@link Termin} — er ist die einzige Stelle, die eine Buchung ändert. Verschickt wird erst nach
   * dem Commit; den Ausgang hält die {@link Zustellung} fest.
   *
   * <p>Kein Versand für eine bereits erinnerte oder eine stornierte Buchung (Issue #107,
   * `ABDECKUNG.md` Z. 250): Beides meldet {@code false}, ohne den Zeitstempel zu berühren.
   */
  boolean erinnere(LocalDateTime jetzt) {
    if (status != Buchungsstatus.ZUGESAGT || erinnerungVersendetAm != null) {
      return false;
    }
    erinnerungVersendetAm = Objects.requireNonNull(jetzt, "jetzt");
    return true;
  }

  /**
   * Ersetzt die Familie durch ein Pseudonym und leert die Notiz — nach Ablauf der Frist (Issue
   * #126) oder vorgezogen auf Verlangen der Familie (#129). Nur für {@link Termin}. Status, Ziel und
   * Zeitstempel bleiben — sie sind die Auslastung, nicht die Person.
   *
   * <p>Ein zweiter Aufruf überschreibt das Pseudonym nur ein weiteres Mal; der Vermerk
   * {@code anonymisiertAm} behält den ersten Zeitpunkt — da sind die Angaben gefallen.
   */
  void anonymisiere(Familie ersatz, LocalDateTime jetzt) {
    familie = Objects.requireNonNull(ersatz, "ersatz");
    notiz = null;
    if (anonymisiertAm == null) {
      anonymisiertAm = Objects.requireNonNull(jetzt, "jetzt");
    }
  }

  /** Ob die Angaben der Familie bereits einem Pseudonym gewichen sind. */
  public boolean istAnonymisiert() {
    return anonymisiertAm != null;
  }

  public boolean istAktiv() {
    return status == Buchungsstatus.ZUGESAGT;
  }

  public BuchungId id() {
    return id;
  }

  public LocalDateTime erstelltAm() {
    return erstelltAm;
  }

  public Familie familie() {
    return familie;
  }

  public Buchungsziel ziel() {
    return ziel;
  }

  public Optional<Notiz> notiz() {
    return Optional.ofNullable(notiz);
  }

  public Buchungsstatus status() {
    return status;
  }

  public Optional<LocalDateTime> erinnerungVersendetAm() {
    return Optional.ofNullable(erinnerungVersendetAm);
  }

  public Optional<LocalDateTime> anonymisiertAm() {
    return Optional.ofNullable(anonymisiertAm);
  }
}
