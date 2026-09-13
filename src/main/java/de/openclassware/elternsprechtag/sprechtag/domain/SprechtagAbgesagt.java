package de.openclassware.elternsprechtag.sprechtag.domain;

/**
 * Ein veröffentlichter Sprechtag wurde abgesagt. Trägt nur die Id — wer daran hängt (der Versand an
 * die betroffenen Eltern), ermittelt die nötigen Daten selbst.
 */
public record SprechtagAbgesagt(SprechtagId sprechtag) implements Ereignis {}
