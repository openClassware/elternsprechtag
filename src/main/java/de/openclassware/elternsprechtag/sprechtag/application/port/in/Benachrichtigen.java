package de.openclassware.elternsprechtag.sprechtag.application.port.in;

import de.openclassware.elternsprechtag.sprechtag.domain.AusfallErfasst;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungenBestaetigt;
import de.openclassware.elternsprechtag.sprechtag.domain.ErinnerungFaellig;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagAbgesagt;

/**
 * Use Case: die Familien über einen festgeschriebenen Vorgang benachrichtigen und festhalten, wen
 * die Nachricht erreicht hat (Issue #110). Kein Knopf: Ausgelöst wird er ausschließlich nach dem
 * Commit des Vorgangs, über dessen gebündeltes Ereignis.
 *
 * <p>Jede Methode ist best-effort und wirft nicht wegen einer einzelnen Adresse. Was scheitert,
 * steht danach als fehlgeschlagene Zustellung an den betroffenen Buchungen.
 */
public interface Benachrichtigen {

  void bestaetige(BuchungenBestaetigt vorgang);

  void erinnere(ErinnerungFaellig vorgang);

  void sageAb(SprechtagAbgesagt vorgang);

  void meldeAusfall(AusfallErfasst vorgang);
}
