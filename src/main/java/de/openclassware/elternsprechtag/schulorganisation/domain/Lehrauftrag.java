package de.openclassware.elternsprechtag.schulorganisation.domain;

import java.util.Objects;

/**
 * Die Verknüpfung <b>Lehrkraft × Klasse × Fach</b> — „Frau Berg unterrichtet Deutsch in der 5a".
 * Aggregat-Wurzel und im Sprechtag-Kontext das Ziel einer Buchung.
 *
 * <p>Die drei Verweise stehen als typisierte Ids darin, nicht als Objekte: Ein Lehrauftrag lässt
 * sich laden, ohne drei weitere Aggregate mitzuladen, und keine Änderung an einer Lehrkraft
 * durchdringt versehentlich ihre Lehraufträge (ADR 0003).
 *
 * <p>Er hat keine {@code benenne}-Methode, und das ist kein Versehen: Ein Lehrauftrag <em>ist</em>
 * sein Tripel. Wechselt einer der drei Verweise, ist es ein anderer Lehrauftrag — der alte wird
 * stillgelegt, der neue erteilt. Alles andere hieße, den Auftrag „Deutsch in der 5a" still in „Mathe
 * in der 7b" zu verwandeln, während Buchungen auf ihn zeigen.
 *
 * <p>Die <b>Eindeutigkeit je Tripel</b> ist bewusst keine Invariante hier, sondern ein
 * Datenbank-Constraint: Sie spannt über alle Lehraufträge, und ein Aggregat, das sie prüfen wollte,
 * müsste dafür alle laden.
 */
public final class Lehrauftrag implements Aggregat {

  private final LehrauftragId id;

  /** Siehe {@link Lehrkraft#version()}. */
  private final long version;

  private final LehrkraftId lehrkraft;
  private final KlasseId klasse;
  private final FachId fach;
  private boolean stillgelegt;

  private Lehrauftrag(
      LehrauftragId id,
      long version,
      LehrkraftId lehrkraft,
      KlasseId klasse,
      FachId fach,
      boolean stillgelegt) {
    this.id = Objects.requireNonNull(id, "id");
    this.version = version;
    this.lehrkraft = Objects.requireNonNull(lehrkraft, "lehrkraft");
    this.klasse = Objects.requireNonNull(klasse, "klasse");
    this.fach = Objects.requireNonNull(fach, "fach");
    this.stillgelegt = stillgelegt;
  }

  /** Ein neuer Lehrauftrag. */
  public static Lehrauftrag erteile(LehrkraftId lehrkraft, KlasseId klasse, FachId fach) {
    return new Lehrauftrag(LehrauftragId.neu(), 0L, lehrkraft, klasse, fach, false);
  }

  /**
   * Setzt einen gespeicherten Lehrauftrag unverändert wieder zusammen — für den Persistenz-Adapter.
   */
  public static Lehrauftrag rekonstruiere(
      LehrauftragId id,
      long version,
      LehrkraftId lehrkraft,
      KlasseId klasse,
      FachId fach,
      boolean stillgelegt) {
    return new Lehrauftrag(id, version, lehrkraft, klasse, fach, stillgelegt);
  }

  /**
   * Nimmt den Lehrauftrag aus dem Angebot. Bestehende Buchungen bleiben lesbar, weil sie ihr
   * Buchungsziel kopiert haben (#138) — genau das ist die Voraussetzung dafür, dass hier überhaupt
   * etwas stillgelegt werden darf.
   */
  public void legeStill() {
    this.stillgelegt = true;
  }

  public LehrauftragId id() {
    return id;
  }

  public long version() {
    return version;
  }

  public LehrkraftId lehrkraft() {
    return lehrkraft;
  }

  public KlasseId klasse() {
    return klasse;
  }

  public FachId fach() {
    return fach;
  }

  public boolean istStillgelegt() {
    return stillgelegt;
  }
}
