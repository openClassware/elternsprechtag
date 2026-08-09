package de.openclassware.elternsprechtag.services;

import java.util.List;
import java.util.UUID;

/**
 * Domänen-Event: Ein Eltern-Submit wurde als Buchungen festgeschrieben. Trägt nur die Ids der in
 * <em>diesem</em> Vorgang erzeugten Buchungen — damit bestätigt der Empfänger genau diesen Vorgang
 * und nicht alle Buchungen einer Adresse; die Anzeigedaten lädt er selbst nach. Wird von
 * {@code BuchungService.buchen} veröffentlicht und strikt nach Commit ({@code AFTER_COMMIT},
 * {@code @Async}) verarbeitet, damit die Buchung festgeschrieben ist, bevor der Versand beginnt.
 */
public record BuchungenErstelltEvent(List<UUID> buchungIds) {}
