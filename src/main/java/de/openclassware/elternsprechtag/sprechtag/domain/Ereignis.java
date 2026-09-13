package de.openclassware.elternsprechtag.sprechtag.domain;

/**
 * Etwas, das fachlich geschehen ist. Aggregate melden feinkörnige Ereignisse; ein Use Case holt sie
 * ab und bündelt sie zu einem Vorgangs-Ereignis, an dem der Versand hängt (ADR 0003).
 */
public interface Ereignis {}
