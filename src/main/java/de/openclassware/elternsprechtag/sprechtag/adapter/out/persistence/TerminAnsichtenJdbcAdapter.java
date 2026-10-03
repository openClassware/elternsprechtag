package de.openclassware.elternsprechtag.sprechtag.adapter.out.persistence;

import de.openclassware.elternsprechtag.sprechtag.application.port.out.TerminAnsichten;
import de.openclassware.elternsprechtag.sprechtag.domain.LehrkraftId;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import de.openclassware.elternsprechtag.sprechtag.domain.TerminId;
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

  private static final String AUSFALL_SLOTS =
      """
      select t.id              as termin_id,
             t.startzeit        as startzeit,
             t.verfuegbarkeit   as verfuegbarkeit,
             b.schueler_name    as schueler_name,
             b.eltern_name      as eltern_name,
             b.eltern_email     as eltern_email
        from termin t
        left join buchungen b on b.termin_id = t.id and b.status = 'ZUGESAGT'
       where t.sprechtag_id = :sprechtagId
         and t.lehrer_id = :lehrkraftId
       order by t.startzeit
      """;

  private static final String ENTFALLENE_JE_LEHRKRAFT =
      """
      select t.lehrer_id as lehrer_id,
             count(*)    as anzahl
        from termin t
       where t.sprechtag_id = :sprechtagId
         and t.verfuegbarkeit = 'ENTFAELLT'
       group by t.lehrer_id
      """;

  /**
   * Höchstens eine geltende Buchung je Termin — das hält das Aggregat, der Left Join verlässt sich
   * darauf.
   */
  private static final String TAGESPLAN =
      """
      select t.lehrer_id         as lehrer_id,
             t.startzeit         as startzeit,
             b.id                as buchung_id,
             b.lehrkraft_name    as lehrkraft_name,
             b.lehrkraft_kuerzel as lehrkraft_kuerzel,
             b.schueler_name     as schueler_name,
             b.klasse_name       as klasse_name,
             b.fach_name         as fach_name,
             b.eltern_name       as eltern_name,
             b.notiz             as notiz,
             b.anonymisiert_am   as anonymisiert_am
        from termin t
        left join buchungen b on b.termin_id = t.id and b.status = 'ZUGESAGT'
       where t.sprechtag_id = :sprechtagId
         and t.verfuegbarkeit = 'VERFUEGBAR'
       order by t.startzeit
      """;

  private static final String MIT_BUCHUNGEN =
      """
      select t.id
        from termin t
       where t.sprechtag_id = :sprechtagId
         and exists (select 1 from buchungen b where b.termin_id = t.id)
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

  @Override
  public List<AusfallZeile> ausfallSlots(SprechtagId sprechtag, LehrkraftId lehrkraft) {
    return jdbc.query(
        AUSFALL_SLOTS,
        Map.of("sprechtagId", sprechtag.wert(), "lehrkraftId", lehrkraft.wert()),
        (rs, zeile) -> {
          String verfuegbarkeit = rs.getString("verfuegbarkeit");
          String elternEmail = rs.getString("eltern_email");
          AusfallSlotZustand zustand =
              "ENTFAELLT".equals(verfuegbarkeit)
                  ? AusfallSlotZustand.ENTFALLEN
                  : elternEmail != null ? AusfallSlotZustand.GEBUCHT : AusfallSlotZustand.FREI;
          return new AusfallZeile(
              rs.getObject("termin_id", java.util.UUID.class),
              rs.getTimestamp("startzeit").toLocalDateTime().toLocalTime(),
              zustand,
              rs.getString("schueler_name"),
              rs.getString("eltern_name"),
              elternEmail);
        });
  }

  @Override
  public List<EntfalleneZeile> entfalleneJeLehrkraft(SprechtagId sprechtag) {
    return jdbc.query(
        ENTFALLENE_JE_LEHRKRAFT,
        Map.of("sprechtagId", sprechtag.wert()),
        (rs, zeile) ->
            new EntfalleneZeile(
                rs.getObject("lehrer_id", java.util.UUID.class), rs.getInt("anzahl")));
  }

  @Override
  public List<TagesplanZeile> tagesplan(SprechtagId sprechtag) {
    return jdbc.query(
        TAGESPLAN,
        Map.of("sprechtagId", sprechtag.wert()),
        (rs, zeile) -> {
          java.sql.Timestamp anonymisiertAm = rs.getTimestamp("anonymisiert_am");
          return new TagesplanZeile(
              rs.getObject("lehrer_id", java.util.UUID.class),
              rs.getTimestamp("startzeit").toLocalDateTime().toLocalTime(),
              rs.getObject("buchung_id") != null,
              rs.getString("lehrkraft_name"),
              rs.getString("lehrkraft_kuerzel"),
              rs.getString("schueler_name"),
              rs.getString("klasse_name"),
              rs.getString("fach_name"),
              rs.getString("eltern_name"),
              rs.getString("notiz"),
              anonymisiertAm == null ? null : anonymisiertAm.toLocalDateTime());
        });
  }

  @Override
  public List<TerminId> mitBuchungen(SprechtagId sprechtag) {
    return jdbc.query(
        MIT_BUCHUNGEN,
        Map.of("sprechtagId", sprechtag.wert()),
        (rs, zeile) -> TerminId.von(rs.getObject("id", java.util.UUID.class)));
  }
}
