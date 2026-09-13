package de.openclassware.elternsprechtag.sprechtag.adapter.out.persistence;

import de.openclassware.elternsprechtag.sprechtag.domain.Buchungsstatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Persistenzmodell einer Buchung — Kind-Zeile des {@link TerminZeile}-Aggregats. Spring-Data-
 * Annotationen liegen ausschließlich hier, niemals am Aggregat (ADR 0004).
 *
 * <p>{@code termin_id} ist bewusst <b>kein</b> Feld: Die Spalte ist die Rückreferenz der
 * {@code @MappedCollection} am Root und wird von Spring Data gesetzt. Ein zweites Feld darauf wäre
 * eine zweite Wahrheit.
 *
 * <p>{@code lehrkraftId}, {@code lehrkraftName}, {@code lehrkraftKuerzel}, {@code klasseName} und
 * {@code fachName} sind der <em>eingefrorene</em> Stand zum Buchungszeitpunkt;
 * {@code lehrauftragId} bleibt nur Herkunftsspur (ADR 0003).
 */
@Table("buchungen")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
class BuchungZeile {

  @Id private UUID id;
  private LocalDateTime erstelltAm;
  private Buchungsstatus status;
  private String schuelerName;
  private String elternName;
  private String elternEmail;
  private String notiz;
  private UUID lehrauftragId;
  private UUID lehrkraftId;
  private String lehrkraftName;
  private String lehrkraftKuerzel;
  private String klasseName;
  private String fachName;
}
