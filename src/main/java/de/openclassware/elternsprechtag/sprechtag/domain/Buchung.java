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
  private final Familie familie;
  private final Buchungsziel ziel;
  private final Notiz notiz;
  private Buchungsstatus status;

  private Buchung(
      BuchungId id,
      LocalDateTime erstelltAm,
      Familie familie,
      Buchungsziel ziel,
      Notiz notiz,
      Buchungsstatus status) {
    this.id = Objects.requireNonNull(id, "id");
    this.erstelltAm = Objects.requireNonNull(erstelltAm, "erstelltAm");
    this.familie = Objects.requireNonNull(familie, "familie");
    this.ziel = Objects.requireNonNull(ziel, "ziel");
    this.status = Objects.requireNonNull(status, "status");
    this.notiz = notiz;
  }

  /**
   * Frisch zugesagte Buchung. Nur für {@link Termin} — die Invariante „ein Slot, höchstens eine
   * aktive Buchung" prüft der Root, und er ist die einzige Stelle, die das kann.
   *
   * @param notiz darf {@code null} sein
   */
  static Buchung zugesagt(
      BuchungId id, LocalDateTime erstelltAm, Familie familie, Buchungsziel ziel, Notiz notiz) {
    return new Buchung(id, erstelltAm, familie, ziel, notiz, Buchungsstatus.ZUGESAGT);
  }

  /**
   * Setzt eine gespeicherte Buchung unverändert wieder zusammen — für den Persistenz-Adapter. Prüft
   * keine Invarianten: Was in der Datenbank steht, ist schon geschehen.
   *
   * @param notiz darf {@code null} sein
   */
  public static Buchung rekonstruiere(
      BuchungId id,
      LocalDateTime erstelltAm,
      Familie familie,
      Buchungsziel ziel,
      Notiz notiz,
      Buchungsstatus status) {
    return new Buchung(id, erstelltAm, familie, ziel, notiz, status);
  }

  /** Nimmt die Zusage zurück; gibt zurück, ob sich dadurch etwas geändert hat. */
  boolean storniere() {
    if (status == Buchungsstatus.STORNIERT) {
      return false;
    }
    status = Buchungsstatus.STORNIERT;
    return true;
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
}
