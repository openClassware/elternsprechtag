package de.openclassware.elternsprechtag.schulorganisation.domain;

/**
 * Ein stillgelegtes Stammdatum wird nicht mehr geändert.
 *
 * <p>Stilllegen ist das Gegenstück zum Löschen: Der Datensatz bleibt, weil ältere Buchungen und
 * Auswertungen ihn bezeugen, aber er nimmt an nichts Neuem mehr teil. Ließe er sich danach noch
 * umbenennen, wäre unklar, welchen Stand eine Auswertung eigentlich zeigt — und der spätere Import
 * bekäme einen Weg, stillgelegte Datensätze unbemerkt wiederzubeleben.
 */
public class StammdatumStillgelegtException extends RuntimeException {

  public StammdatumStillgelegtException(String meldung) {
    super(meldung);
  }
}
