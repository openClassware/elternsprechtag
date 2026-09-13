package de.openclassware.elternsprechtag.sprechtag.domain;

import java.util.Optional;

/**
 * Das Anliegen, das genau einer Lehrkraft gilt — Freitext, höchstens {@value #MAX_ZEICHEN} Zeichen.
 * Dieselbe Grenze zieht die Buchungsoberfläche und seit V2 auch die Spalte; sie steht hier, damit
 * sie nicht erst am INSERT auffällt.
 *
 * <p>Eine Notiz ist immer inhaltlich vorhanden: Leerraum ist keine Notiz, sondern keine — dafür
 * gibt es {@link #vielleicht(String)}.
 */
public record Notiz(String text) {

  public static final int MAX_ZEICHEN = 500;

  public Notiz {
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("Eine Notiz ohne Inhalt ist keine Notiz");
    }
    text = text.trim();
    if (text.length() > MAX_ZEICHEN) {
      throw new IllegalArgumentException(
          "Die Notiz ist länger als " + MAX_ZEICHEN + " Zeichen: " + text.length());
    }
  }

  /** Aus Rohtext von außen: leer oder {@code null} ergibt keine Notiz, nicht eine leere. */
  public static Optional<Notiz> vielleicht(String roh) {
    return roh == null || roh.isBlank() ? Optional.empty() : Optional.of(new Notiz(roh));
  }
}
