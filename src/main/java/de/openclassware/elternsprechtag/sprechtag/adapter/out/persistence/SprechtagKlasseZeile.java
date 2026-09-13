package de.openclassware.elternsprechtag.sprechtag.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Eine Zeile der Verknüpfungstabelle {@code sprechtage_klassen}: die Teilnahme einer Klasse an einem
 * Sprechtag.
 *
 * <p>Mehr als die Klassen-Id steht hier nicht und soll hier nicht stehen — der Name der Klasse
 * gehört der Schulorganisation und wird über deren Port gelesen, nicht mitgejoint (ADR 0003). Die
 * Rückreferenz {@code sprechtag_id} setzt Spring Data JDBC aus der Zugehörigkeit zum Aggregat.
 */
@Table("sprechtage_klassen")
record SprechtagKlasseZeile(UUID klasseId) {}
