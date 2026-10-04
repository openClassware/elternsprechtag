package de.openclassware.elternsprechtag.sprechtag.adapter.out.persistence;

/**
 * Wer eine Benachrichtigung bekommt, in SQL: je Adresse und Kind (ADR 0007). Jede Zählung und
 * Gruppierung „je Empfänger" der Leseseite baut auf diesen Spalten der Buchung (Alias {@code b}) —
 * einmal geschrieben, damit ein späterer Familien- oder Vorgangsbegriff ihn hier ersetzt und nicht
 * an verstreuten Stellen.
 */
final class Empfaengerschluessel {

  /** Die Spalten des Schlüssels — für {@code group by} und {@code count(distinct (...))}. */
  static final String SPALTEN = "b.eltern_email, b.schueler_name, b.klasse_name";

  /** Der Schlüssel als ein Text, für Leser, die ihn nur vergleichen und nie zerlegen. */
  static final String ALS_TEXT = "json_build_array(" + SPALTEN + ")::text";

  private Empfaengerschluessel() {}
}
