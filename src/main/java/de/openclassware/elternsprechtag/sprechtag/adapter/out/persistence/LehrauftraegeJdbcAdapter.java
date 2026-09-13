package de.openclassware.elternsprechtag.sprechtag.adapter.out.persistence;

import de.openclassware.elternsprechtag.sprechtag.application.port.out.Lehrauftraege;
import de.openclassware.elternsprechtag.sprechtag.domain.LehrauftragId;
import de.openclassware.elternsprechtag.sprechtag.domain.LehrkraftId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Liest die Stammdaten der Schulorganisation — <b>nur lesend</b>, dieser Kontext schreibt sie nie.
 *
 * <p>Übergangsweise geht das direkt auf die bestehenden Tabellen. Sobald die Schulorganisation ihre
 * eigenen Aggregate hat, wird daraus ein Aufruf über deren Port; die Signatur hier bleibt dieselbe,
 * und genau dafür steht der Port.
 */
@RequiredArgsConstructor
@Component
class LehrauftraegeJdbcAdapter implements Lehrauftraege {

  private static final String SPALTEN =
      """
      select la.id       as id,
             la.lehrer_id as lehrer_id,
             l.kuerzel   as kuerzel,
             l.vorname   as vorname,
             l.nachname  as nachname,
             k.name      as klasse_name,
             f.name      as fach_name
        from lehrauftrag la
        join lehrer  l on l.id = la.lehrer_id
        join klassen k on k.id = la.klasse_id
        join faecher f on f.id = la.fach_id
      """;

  private static final String NACH_ID = SPALTEN + " where la.id = :id";

  private static final String NACH_KLASSE =
      SPALTEN + " where la.klasse_id = :klasseId order by f.name";

  private static final RowMapper<LehrauftragDaten> MAPPER =
      (rs, zeile) ->
          new LehrauftragDaten(
              LehrauftragId.von(rs.getObject("id", UUID.class)),
              LehrkraftId.von(rs.getObject("lehrer_id", UUID.class)),
              rs.getString("kuerzel"),
              rs.getString("vorname"),
              rs.getString("nachname"),
              rs.getString("klasse_name"),
              rs.getString("fach_name"));

  private final NamedParameterJdbcTemplate jdbc;

  @Override
  public Optional<LehrauftragDaten> lade(LehrauftragId id) {
    return jdbc.query(NACH_ID, Map.of("id", id.wert()), MAPPER).stream().findFirst();
  }

  @Override
  public List<LehrauftragDaten> fuerKlasse(UUID klasseId) {
    return jdbc.query(NACH_KLASSE, Map.of("klasseId", klasseId), MAPPER);
  }
}
