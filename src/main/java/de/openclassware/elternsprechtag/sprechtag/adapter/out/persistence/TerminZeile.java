package de.openclassware.elternsprechtag.sprechtag.adapter.out.persistence;

import de.openclassware.elternsprechtag.sprechtag.domain.Verfuegbarkeit;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Persistenzmodell des Termin-Aggregats — die Wurzel, an der auch das optimistische Sperren hängt.
 *
 * <p>{@link Version} steht bewusst <em>nur</em> hier: Es sperrt das ganze Aggregat einschließlich
 * seiner Buchungen und ist damit genau der Mechanismus, auf den der Buchungsvorgang baut (ADR 0005).
 *
 * <p>Die Id wird nicht von der Datenbank erzeugt — die Domäne vergibt sie
 * ({@code TerminId.neu()}). Spring Data erkennt eine neue Zeile deshalb an {@code version == 0}.
 * Beim Speichern schreibt Spring Data JDBC die Kind-Zeilen neu (Delete-and-Insert); weil die Ids von
 * außen kommen, bleiben sie über wiederholtes Speichern stabil — Voraussetzung dafür, dass die
 * Bestätigungsmail sie nach dem Commit noch findet.
 */
@Table("termin")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
class TerminZeile {

  @Id private UUID id;
  private LocalDateTime startzeit;
  private LocalDateTime endzeit;
  private Verfuegbarkeit verfuegbarkeit;
  @Version private long version;
  private UUID lehrerId;
  private UUID sprechtagId;

  /**
   * Die Buchungen dieses Slots. {@code Set} statt {@code List}, weil eine Liste zusätzlich eine
   * Index-Spalte verlangen würde — eine Ordnung, die fachlich nicht existiert. Die chronologische
   * Reihenfolge stellen die Leseseite und der Mapper her.
   */
  @MappedCollection(idColumn = "termin_id")
  private Set<BuchungZeile> buchungen = new LinkedHashSet<>();
}
