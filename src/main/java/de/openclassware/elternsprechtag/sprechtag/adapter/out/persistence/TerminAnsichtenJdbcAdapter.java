package de.openclassware.elternsprechtag.sprechtag.adapter.out.persistence;

import de.openclassware.elternsprechtag.sprechtag.application.port.out.TerminAnsichten;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Die Leseseite der Termine: ein Statement statt rund 600 Aggregat-Ladevorgänge (ADR 0003).
 *
 * <p>„Buchbar" wird hier abgeleitet — aus der Verfügbarkeit und der Existenz einer aktiven Buchung,
 * genau wie {@code Termin.istBuchbar()} es im Aggregat tut. Es gibt keine Spalte dafür, und es soll
 * auch keine geben: Zwei Wahrheiten für einen Fakt waren der Grund, {@code TerminStatusEnum}
 * abzuschaffen.
 */
@RequiredArgsConstructor
@Component
class TerminAnsichtenJdbcAdapter implements TerminAnsichten {

  private static final String SLOTS =
      """
      select t.id        as termin_id,
             t.lehrer_id as lehrer_id,
             t.startzeit as startzeit,
             (t.verfuegbarkeit = 'VERFUEGBAR'
              and not exists (select 1
                                from buchungen b
                               where b.termin_id = t.id
                                 and b.status = 'ZUGESAGT')) as buchbar
        from termin t
       where t.sprechtag_id = :sprechtagId
       order by t.startzeit
      """;

  private final NamedParameterJdbcTemplate jdbc;

  @Override
  public List<SlotZeile> slots(SprechtagId sprechtag) {
    return jdbc.query(
        SLOTS,
        Map.of("sprechtagId", sprechtag.wert()),
        (rs, zeile) ->
            new SlotZeile(
                rs.getObject("termin_id", java.util.UUID.class),
                rs.getObject("lehrer_id", java.util.UUID.class),
                rs.getTimestamp("startzeit").toLocalDateTime().toLocalTime(),
                rs.getBoolean("buchbar")));
  }
}
