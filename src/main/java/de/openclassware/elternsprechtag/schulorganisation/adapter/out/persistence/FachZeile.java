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
 * Persistenzmodell des Fachs. Siehe {@link LehrkraftZeile} für die gemeinsamen Festlegungen.
 *
 * <p>Das Feld heißt {@code shortName} nach der Spalte {@code short_name} — in der Domäne heißt
 * dasselbe {@code kuerzel}. Genau solche Übersetzungen sind der Zweck des Mappers.
 */
@Table("faecher")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
class FachZeile {

  @Id private UUID id;
  private String name;
  private String shortName;
  private boolean stillgelegt;
  @Version private long version;
}
