package de.openclassware.elternsprechtag.schulorganisation.adapter.out.persistence;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Persistenzmodell des Lehrauftrags. Siehe {@link LehrkraftZeile} für die gemeinsamen Festlegungen.
 *
 * <p>Die drei Verweise stehen als nackte Fremdschlüssel-Spalten da und nicht als
 * {@code AggregateReference}: Spring Data JDBC würde daraus keine andere SQL-Anweisung machen, und
 * die Typisierung liegt ohnehin im Aggregat. {@code lehrer_id} heißt so, weil die Tabelle
 * {@code lehrer} heißt.
 */
@Table("lehrauftrag")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
class LehrauftragZeile {

  @Id private UUID id;
  private UUID lehrerId;
  private UUID klasseId;
  private UUID fachId;
  private boolean stillgelegt;
  @Version private long version;
}
