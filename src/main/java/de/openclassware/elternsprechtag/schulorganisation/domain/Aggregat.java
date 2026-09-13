package de.openclassware.elternsprechtag.schulorganisation.domain;

/**
 * Markiert die Aggregat-Wurzeln dieses Kontexts. Sie tragen keine gemeinsame Basisklasse: Anders als
 * im Sprechtag-Kontext meldet hier kein Aggregat Ereignisse — Stammdaten ändern sich, ohne dass
 * jemand darauf reagieren müsste.
 *
 * <p>Die Marke ist trotzdem kein Selbstzweck. An ihr hängt die Architekturregel „Aggregate
 * referenzieren einander ausschließlich über typisierte Ids" ({@code ArchitekturTest}), und sie
 * beantwortet beim Lesen die Frage, wo eine Aggregat-Grenze verläuft — in einem Kontext, dessen vier
 * Wurzeln sonst wie vier Tabellenzeilen aussähen.
 *
 * <p>Bewusst <b>nicht</b> der {@code AggregateRoot} des Sprechtag-Kontexts: Über die Kontextgrenze
 * geht keine gemeinsame Oberklasse, sonst wäre die Grenze keine (ADR 0003).
 */
public interface Aggregat {}
