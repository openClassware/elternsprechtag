package de.openclassware.elternsprechtag.services;

import java.util.UUID;

/**
 * Domänen-Event: Ein veröffentlichter Sprechtag wurde abgesagt (Übergang
 * {@code VEROEFFENTLICHT -> ABGESAGT}). Trägt nur die Sprechtag-Id — die Empfänger-Ermittlung lädt
 * die nötigen Daten selbst. Wird von {@code SprechtagService.changeStatus} veröffentlicht und
 * strikt nach Commit ({@code AFTER_COMMIT}, {@code @Async}) verarbeitet, damit die Absage bereits
 * festgeschrieben ist, bevor der Versand beginnt.
 */
public record SprechtagAbgesagtEvent(UUID sprechtagId) {}
