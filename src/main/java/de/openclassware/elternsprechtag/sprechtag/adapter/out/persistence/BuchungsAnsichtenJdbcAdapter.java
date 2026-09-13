package de.openclassware.elternsprechtag.sprechtag.adapter.out.persistence;

import de.openclassware.elternsprechtag.sprechtag.application.port.out.BuchungsAnsichten;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungId;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Die Leseseite der Buchungen. Jedes Statement liest ausschließlich die eingefrorenen Spalten der
 * Buchung und joint höchstens auf {@code termin} — nie in die Stammdaten. Damit bleibt die
 * Auswertung eines vergangenen Sprechtags von einem späteren Import unberührt (ADR 0003).
 */
@RequiredArgsConstructor
@Component
class BuchungsAnsichtenJdbcAdapter implements BuchungsAnsichten {

  private static final String AKTIVE_BUCHUNGEN =
      """
      select b.lehrkraft_id      as lehrkraft_id,
             b.lehrkraft_name    as lehrkraft_name,
             b.lehrkraft_kuerzel as lehrkraft_kuerzel,
             t.startzeit         as startzeit,
             b.schueler_name     as schueler_name,
             b.klasse_name       as klasse_name,
             b.fach_name         as fach_name,
             b.eltern_name       as eltern_name,
             b.notiz             as notiz
        from buchungen b
        join termin t on t.id = b.termin_id
       where t.sprechtag_id = :sprechtagId
         and b.status = 'ZUGESAGT'
       order by t.startzeit
      """;

  private static final String BELEGE =
      """
      select t.sprechtag_id    as sprechtag_id,
             t.startzeit       as startzeit,
             b.lehrkraft_name  as lehrkraft_name,
             b.fach_name       as fach_name,
             b.notiz           as notiz,
             b.eltern_name     as eltern_name,
             b.schueler_name   as schueler_name,
             b.eltern_email    as eltern_email,
             b.klasse_name     as klasse_name
        from buchungen b
        join termin t on t.id = b.termin_id
       where b.id in (:ids)
       order by t.startzeit
      """;

  private static final String AKTIVE_ADRESSEN =
      """
      select distinct b.eltern_email
        from buchungen b
        join termin t on t.id = b.termin_id
       where t.sprechtag_id = :sprechtagId
         and b.status = 'ZUGESAGT'
      """;

  private static final String ANZAHL_AKTIVE_ADRESSEN =
      """
      select count(distinct b.eltern_email)
        from buchungen b
        join termin t on t.id = b.termin_id
       where t.sprechtag_id = :sprechtagId
         and b.status = 'ZUGESAGT'
      """;

  private final NamedParameterJdbcTemplate jdbc;

  @Override
  public List<AuswertungsZeile> aktiveBuchungen(SprechtagId sprechtag) {
    return jdbc.query(
        AKTIVE_BUCHUNGEN,
        Map.of("sprechtagId", sprechtag.wert()),
        (rs, zeile) ->
            new AuswertungsZeile(
                rs.getObject("lehrkraft_id", UUID.class),
                rs.getString("lehrkraft_name"),
                rs.getString("lehrkraft_kuerzel"),
                rs.getTimestamp("startzeit").toLocalDateTime().toLocalTime(),
                rs.getString("schueler_name"),
                rs.getString("klasse_name"),
                rs.getString("fach_name"),
                rs.getString("eltern_name"),
                rs.getString("notiz")));
  }

  @Override
  public List<BelegZeile> belege(List<BuchungId> buchungen) {
    if (buchungen.isEmpty()) {
      // Ein leeres IN wäre kein gültiges SQL — und ohne Buchung gibt es ohnehin nichts zu belegen.
      return List.of();
    }
    List<UUID> ids = buchungen.stream().map(BuchungId::wert).toList();
    return jdbc.query(
        BELEGE,
        Map.of("ids", ids),
        (rs, zeile) ->
            new BelegZeile(
                rs.getObject("sprechtag_id", UUID.class),
                rs.getTimestamp("startzeit").toLocalDateTime().toLocalTime(),
                rs.getString("lehrkraft_name"),
                rs.getString("fach_name"),
                rs.getString("notiz"),
                rs.getString("eltern_name"),
                rs.getString("schueler_name"),
                rs.getString("eltern_email"),
                rs.getString("klasse_name")));
  }

  @Override
  public List<String> aktiveElternAdressen(SprechtagId sprechtag) {
    return jdbc.queryForList(
        AKTIVE_ADRESSEN, Map.of("sprechtagId", sprechtag.wert()), String.class);
  }

  @Override
  public long zaehleAktiveElternAdressen(SprechtagId sprechtag) {
    Long anzahl =
        jdbc.queryForObject(
            ANZAHL_AKTIVE_ADRESSEN, Map.of("sprechtagId", sprechtag.wert()), Long.class);
    return anzahl == null ? 0L : anzahl;
  }
}
