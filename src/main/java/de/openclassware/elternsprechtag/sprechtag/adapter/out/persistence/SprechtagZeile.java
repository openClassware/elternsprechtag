package de.openclassware.elternsprechtag.sprechtag.adapter.out.persistence;

import de.openclassware.elternsprechtag.sprechtag.domain.ErinnerungsVorlauf;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import java.time.LocalDate;
import java.time.LocalTime;
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
 * Persistenzmodell des Sprechtag-Aggregats — die Wurzel, an der auch das optimistische Sperren
 * hängt.
 *
 * <p>Die Spaltennamen stammen noch aus der Zeit der JPA-Entity ({@code start_date},
 * {@code location}, …) und bleiben es: Ein Umbenennen brächte eine Migration über eine Tabelle mit
 * Fremdschlüsseln, ohne dass irgendjemand außerhalb dieser Klasse davon etwas hätte.
 *
 * <p>Die Id wird nicht von der Datenbank erzeugt — die Domäne vergibt sie ({@code SprechtagId.neu()});
 * Spring Data erkennt eine neue Zeile an {@code version == 0}.
 */
@Table("sprechtage")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
class SprechtagZeile {

  @Id private UUID id;
  private String titel;
  private String location;
  private String description;
  private String schulkontakt;
  private LocalDate startDate;
  private LocalTime startTime;
  private LocalTime endTime;
  private int slotInMinutes;
  private String accessToken;
  private SprechtagStatus status;
  private ErinnerungsVorlauf erinnerungVorlauf;
  private int anmeldefristTage;
  @Version private long version;

  /**
   * Die teilnehmenden Klassen. {@code Set} statt {@code List}: Eine Liste verlangte eine
   * Index-Spalte — eine Ordnung, die fachlich nicht existiert. Beim Speichern schreibt Spring Data
   * JDBC diese Zeilen neu (Delete-and-Insert); das ist genau die Semantik „so und nicht anders sieht
   * die Teilnehmerliste aus".
   */
  @MappedCollection(idColumn = "sprechtag_id")
  private Set<SprechtagKlasseZeile> klassen = new LinkedHashSet<>();
}
