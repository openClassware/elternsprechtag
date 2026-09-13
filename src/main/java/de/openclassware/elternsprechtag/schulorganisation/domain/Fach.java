package de.openclassware.elternsprechtag.schulorganisation.domain;

import static de.openclassware.elternsprechtag.schulorganisation.domain.Pflichttext.pflicht;

import java.util.Objects;

/**
 * Ein Unterrichtsfach (Name und Kürzel) — Aggregat-Wurzel, fachlich nur als Teil eines {@link
 * Lehrauftrag}s relevant.
 *
 * <p>Trotzdem eine eigene Wurzel und kein Wertobjekt am Lehrauftrag: Ein Fach wird umbenannt und
 * stillgelegt, ohne dass ein Lehrauftrag davon weiß, und es überlebt jeden einzelnen von ihnen.
 */
public final class Fach implements Aggregat {

  private final FachId id;

  /** Siehe {@link Lehrkraft#version()}. */
  private final long version;

  private String name;
  private String kuerzel;
  private boolean stillgelegt;

  private Fach(FachId id, long version, String name, String kuerzel, boolean stillgelegt) {
    this.id = Objects.requireNonNull(id, "id");
    this.version = version;
    this.name = pflicht(name, "Fachname");
    this.kuerzel = pflicht(kuerzel, "Fachkürzel");
    this.stillgelegt = stillgelegt;
  }

  /** Ein neues Fach. */
  public static Fach fuehreEin(String name, String kuerzel) {
    return new Fach(FachId.neu(), 0L, name, kuerzel, false);
  }

  /** Setzt ein gespeichertes Fach unverändert wieder zusammen — für den Persistenz-Adapter. */
  public static Fach rekonstruiere(
      FachId id, long version, String name, String kuerzel, boolean stillgelegt) {
    return new Fach(id, version, name, kuerzel, stillgelegt);
  }

  public void benenne(String name, String kuerzel) {
    if (stillgelegt) {
      throw new StammdatumStillgelegtException(
          "Stillgelegtes Fach lässt sich nicht mehr ändern: " + this.name);
    }
    this.name = pflicht(name, "Fachname");
    this.kuerzel = pflicht(kuerzel, "Fachkürzel");
  }

  /** Siehe {@link Lehrkraft#legeStill()}. */
  public void legeStill() {
    this.stillgelegt = true;
  }

  public FachId id() {
    return id;
  }

  public long version() {
    return version;
  }

  public String name() {
    return name;
  }

  public String kuerzel() {
    return kuerzel;
  }

  public boolean istStillgelegt() {
    return stillgelegt;
  }
}
