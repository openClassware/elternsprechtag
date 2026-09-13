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

/** Persistenzmodell der Klasse. Siehe {@link LehrkraftZeile} für die gemeinsamen Festlegungen. */
@Table("klassen")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
class KlasseZeile {

  @Id private UUID id;
  private String name;
  private boolean stillgelegt;
  @Version private long version;
}
