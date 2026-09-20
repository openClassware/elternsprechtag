package de.openclassware.elternsprechtag.sprechtag.domain;

/**
 * Wozu ein {@link BuchungenBestaetigt} gehört — daran wählt die Mail-Seite ihren Einstiegsbaustein.
 * Ein gewöhnlicher Nachtrag und ein Eltern-Submit sind {@link #BUCHUNG}; sie listen den vollständigen
 * Vorgang. Das Umbuchen ist {@link #UMBUCHUNG}: Es bündelt nur den einen geänderten Termin, und der
 * Standardtext („Ihre Buchung ist eingegangen") würde bei einer Familie mit mehreren Terminen in die
 * Irre führen — sie müsste glauben, die anderen seien weg.
 */
public enum Anlass {
  BUCHUNG,
  UMBUCHUNG
}
