package de.openclassware.elternsprechtag.sprechtag.domain;

/**
 * Der Zustand einer Buchung. {@code STORNIERT} heißt bewusst nicht {@code ABGESAGT}: Eine Buchung
 * wird storniert, ein Sprechtag wird abgesagt (ADR 0003).
 */
public enum Buchungsstatus {
  ZUGESAGT,
  STORNIERT
}
