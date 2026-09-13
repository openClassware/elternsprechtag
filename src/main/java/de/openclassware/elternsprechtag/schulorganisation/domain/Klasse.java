package de.openclassware.elternsprechtag.schulorganisation.domain;

import static de.openclassware.elternsprechtag.schulorganisation.domain.Pflichttext.pflicht;

import java.util.Objects;

/**
 * Eine Schulklasse („10a") — Aggregat-Wurzel über ihren Namen, mehr trägt sie nicht.
 *
 * <p>Ein Aggregat mit einem einzigen Feld sieht nach Überbau aus und ist trotzdem richtig so: Die
 * Klasse ist der Anker, an dem ein Sprechtag seine Teilnehmer wählt und ein Lehrauftrag hängt, und
 * sie hat mit dem Stilllegen einen eigenen Lebenszyklus. Eine Zeile ohne Verhalten wäre sie nur so
 * lange, bis der Stammdaten-Import fragt, was mit einer Klasse geschieht, die es nächstes Jahr nicht
 * mehr gibt.
 *
 * <p>Ihre Lehraufträge und die Sprechtage, an denen sie teilnimmt, gehören nicht dazu — beide
 * verweisen per Id hierher.
 */
public final class Klasse implements Aggregat {

  private final KlasseId id;

  /** Siehe {@link Lehrkraft#version()}. */
  private final long version;

  private String name;
  private boolean stillgelegt;

  private Klasse(KlasseId id, long version, String name, boolean stillgelegt) {
    this.id = Objects.requireNonNull(id, "id");
    this.version = version;
    this.name = pflicht(name, "Klassenname");
    this.stillgelegt = stillgelegt;
  }

  /** Eine neue Klasse. */
  public static Klasse richteEin(String name) {
    return new Klasse(KlasseId.neu(), 0L, name, false);
  }

  /** Setzt eine gespeicherte Klasse unverändert wieder zusammen — für den Persistenz-Adapter. */
  public static Klasse rekonstruiere(KlasseId id, long version, String name, boolean stillgelegt) {
    return new Klasse(id, version, name, stillgelegt);
  }

  /**
   * Umbenennen, nicht aufsteigen lassen: Aus der 5a wird im nächsten Schuljahr <em>nicht</em> die
   * 6a, weil die 5a dann von anderen Kindern gebildet wird. Diese Methode korrigiert einen Namen;
   * den Jahrgangswechsel entscheidet der Import, wenn Quellsystem und Format bekannt sind.
   */
  public void benenne(String name) {
    if (stillgelegt) {
      throw new StammdatumStillgelegtException(
          "Stillgelegte Klasse lässt sich nicht mehr ändern: " + this.name);
    }
    this.name = pflicht(name, "Klassenname");
  }

  /** Siehe {@link Lehrkraft#legeStill()}. */
  public void legeStill() {
    this.stillgelegt = true;
  }

  public KlasseId id() {
    return id;
  }

  public long version() {
    return version;
  }

  public String name() {
    return name;
  }

  public boolean istStillgelegt() {
    return stillgelegt;
  }
}
