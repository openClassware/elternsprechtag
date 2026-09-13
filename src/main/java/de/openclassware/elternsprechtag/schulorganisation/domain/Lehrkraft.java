package de.openclassware.elternsprechtag.schulorganisation.domain;

import static de.openclassware.elternsprechtag.schulorganisation.domain.Pflichttext.pflicht;

import java.util.Objects;

/**
 * Die Person, mit der am Sprechtag gesprochen wird — Aggregat-Wurzel über ihren Namen und ihr
 * Kürzel.
 *
 * <p>Sie ist Stammdatum, kein Benutzer: Lehrkräfte haben kein Login, und dieses Aggregat trägt
 * deshalb nichts, was nach einem Konto aussieht (siehe „Auth" in {@code CLAUDE.md}).
 *
 * <p>Ihre Lehraufträge gehören nicht dazu. Ein {@link Lehrauftrag} ist eine eigene Wurzel und
 * verweist per {@link LehrkraftId} hierher — die Lehrkraft kennt sie nicht (ADR 0003). Das ist nicht
 * nur Formsache: Der spätere Stammdaten-Import ändert Namen und Lehraufträge unabhängig
 * voneinander.
 */
public final class Lehrkraft implements Aggregat {

  private final LehrkraftId id;

  /**
   * Der Stand des optimistischen Sperrens, wie er geladen wurde. {@code 0} heißt „noch nie
   * gespeichert". Das Aggregat zählt nicht selbst hoch — das tut der Persistenz-Adapter (ADR 0004).
   */
  private final long version;

  private String vorname;
  private String nachname;
  private String kuerzel;
  private boolean stillgelegt;

  private Lehrkraft(
      LehrkraftId id,
      long version,
      String vorname,
      String nachname,
      String kuerzel,
      boolean stillgelegt) {
    this.id = Objects.requireNonNull(id, "id");
    this.version = version;
    this.vorname = pflicht(vorname, "Vorname");
    this.nachname = pflicht(nachname, "Nachname");
    this.kuerzel = pflicht(kuerzel, "Kürzel");
    this.stillgelegt = stillgelegt;
  }

  /** Eine neue Lehrkraft. */
  public static Lehrkraft stelleEin(String vorname, String nachname, String kuerzel) {
    return new Lehrkraft(LehrkraftId.neu(), 0L, vorname, nachname, kuerzel, false);
  }

  /**
   * Setzt eine gespeicherte Lehrkraft unverändert wieder zusammen — für den Persistenz-Adapter.
   * Prüft keine Übergänge: Eine stillgelegte Lehrkraft muss zurückkommen können, sonst ließe sich
   * keine alte Auswertung mehr lesen.
   */
  public static Lehrkraft rekonstruiere(
      LehrkraftId id,
      long version,
      String vorname,
      String nachname,
      String kuerzel,
      boolean stillgelegt) {
    return new Lehrkraft(id, version, vorname, nachname, kuerzel, stillgelegt);
  }

  /** Namen und Kürzel korrigieren — Heirat, Tippfehler, ein neues Kürzel nach Kollision. */
  public void benenne(String vorname, String nachname, String kuerzel) {
    if (stillgelegt) {
      throw new StammdatumStillgelegtException(
          "Stillgelegte Lehrkraft lässt sich nicht mehr ändern: " + this.kuerzel);
    }
    this.vorname = pflicht(vorname, "Vorname");
    this.nachname = pflicht(nachname, "Nachname");
    this.kuerzel = pflicht(kuerzel, "Kürzel");
  }

  /**
   * Nimmt die Lehrkraft aus dem laufenden Betrieb, ohne sie zu löschen. Zweimal stilllegen ist kein
   * Fehler — der spätere Import darf denselben Abgleich wiederholen, ohne dass es knallt.
   */
  public void legeStill() {
    this.stillgelegt = true;
  }

  public LehrkraftId id() {
    return id;
  }

  public long version() {
    return version;
  }

  public String vorname() {
    return vorname;
  }

  public String nachname() {
    return nachname;
  }

  public String kuerzel() {
    return kuerzel;
  }

  public boolean istStillgelegt() {
    return stillgelegt;
  }

  /** Wie die Oberfläche die Lehrkraft nennt. */
  public String anzeigeName() {
    return vorname + " " + nachname;
  }
}
