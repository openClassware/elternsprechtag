package de.openclassware.elternsprechtag.sprechtag.application.port.in;

import java.util.UUID;

/**
 * Use Case: einen neuen Sprechtag als Entwurf anlegen. Jeder Sprechtag beginnt so — veröffentlicht
 * wird er über {@link Veroeffentlichen}, nie durch das Anlegen selbst.
 */
public interface Anlegen {

  /**
   * @return die Id des neuen Sprechtags
   */
  UUID lege(SprechtagFormular formular);

  /**
   * Legt an und veröffentlicht in einem Zug — der Anlege-Knopf des Formulars. Beides in <em>einer</em>
   * Transaktion: Scheitert das Veröffentlichen, bleibt kein halb angelegter Entwurf zurück.
   */
  Veroeffentlichen.Ergebnis legeUndVeroeffentliche(SprechtagFormular formular);
}
