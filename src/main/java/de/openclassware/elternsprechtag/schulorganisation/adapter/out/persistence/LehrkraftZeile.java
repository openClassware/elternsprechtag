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
 * Persistenzmodell der Lehrkraft.
 *
 * <p>Die Tabelle heißt {@code lehrer} und bleibt es: Sie stammt aus der Zeit vor dem Umbau, und ein
 * Umbenennen brächte eine Migration über eine Tabelle mit Fremdschlüsseln, ohne dass irgendjemand
 * außerhalb dieser Klasse davon etwas hätte. Nach außen heißt sie überall <b>Lehrkraft</b>
 * (siehe {@code docs/contexts/schulorganisation/CONTEXT.md}).
 *
 * <p>Die Id vergibt die Domäne, nicht die Datenbank; Spring Data erkennt eine neue Zeile an
 * {@code version == 0} (ADR 0004).
 */
@Table("lehrer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
class LehrkraftZeile {

  @Id private UUID id;
  private String vorname;
  private String nachname;
  private String kuerzel;
  private boolean stillgelegt;
  @Version private long version;
}
