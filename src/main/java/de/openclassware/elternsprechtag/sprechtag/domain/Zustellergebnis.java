package de.openclassware.elternsprechtag.sprechtag.domain;

/**
 * Was der Versand über eine Nachricht weiß. {@link #ABGESCHICKT} heißt: Der Mailserver hat sie
 * angenommen — nicht, dass sie angekommen ist. Ein Rückläufer Minuten später erreicht das Produkt
 * nicht (Issue #110, Bounce-Auswertung „darf fehlen").
 */
public enum Zustellergebnis {
  ABGESCHICKT,
  FEHLGESCHLAGEN
}
