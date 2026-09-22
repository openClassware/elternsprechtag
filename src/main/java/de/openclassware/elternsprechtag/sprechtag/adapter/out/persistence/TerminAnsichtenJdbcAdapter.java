package de.openclassware.elternsprechtag.sprechtag.adapter.out.persistence;

import de.openclassware.elternsprechtag.sprechtag.application.port.out.TerminAnsichten;
import de.openclassware.elternsprechtag.sprechtag.domain.LehrkraftId;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import de.openclassware.elternsprechtag.sprechtag.domain.Verfuegbarkeit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

  /**
   * Der Join steht auf {@code status = 'ZUGESAGT'} und nicht in einer {@code where}-Klausel: So
   * bleibt er ein Left Join, der freie und entfallene Slots mitbringt — und ein Slot, der in seiner
   * Historie drei stornierte Buchungen trägt, ergibt trotzdem genau eine Zeile.
   */
  private static final String SLOTS_DER_LEHRKRAFT =
      """
      select t.id             as termin_id,
             t.startzeit      as startzeit,
             t.verfuegbarkeit as verfuegbarkeit,
             b.schueler_name  as schueler_name,
             b.eltern_name    as eltern_name,
             b.eltern_email   as eltern_email
        from termin t
        left join buchungen b on b.termin_id = t.id
                             and b.status = 'ZUGESAGT'
       where t.sprechtag_id = :sprechtagId
         and t.lehrer_id = :lehrkraftId
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
                rs.getObject("termin_id", UUID.class),
                rs.getObject("lehrer_id", UUID.class),
                rs.getTimestamp("startzeit").toLocalDateTime().toLocalTime(),
                rs.getBoolean("buchbar")));
  }

  @Override
  public List<LehrkraftSlotZeile> slotsDerLehrkraft(SprechtagId sprechtag, LehrkraftId lehrkraft) {
    return jdbc.query(
        SLOTS_DER_LEHRKRAFT,
        Map.of("sprechtagId", sprechtag.wert(), "lehrkraftId", lehrkraft.wert()),
        (rs, zeile) ->
            new LehrkraftSlotZeile(
                rs.getObject("termin_id", UUID.class),
                rs.getTimestamp("startzeit").toLocalDateTime().toLocalTime(),
                Verfuegbarkeit.ENTFAELLT.name().equals(rs.getString("verfuegbarkeit")),
                rs.getString("schueler_name"),
                rs.getString("eltern_name"),
                rs.getString("eltern_email")));
  }
}
