package de.openclassware.elternsprechtag.sprechtag.adapter.out.persistence;

import de.openclassware.elternsprechtag.sprechtag.application.port.out.SprechtagAnsichten;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Liest Sprechtage an den Aggregaten vorbei — handgeschriebenes SQL für Übersicht und Kopfdaten.
 *
 * <p>Die Klassen kommen in beiden Fällen als reine Id-Liste aus der Verknüpfungstabelle: Ihr Name
 * gehört der Schulorganisation, und kein Statement joint über die Kontextgrenze (ADR 0003).
 * Zusammengefügt wird in den Use Cases.
 */
@RequiredArgsConstructor
@Component
class SprechtagAnsichtenJdbcAdapter implements SprechtagAnsichten {

  private static final String ZEILEN =
      """
      select id, titel, start_date, start_time, end_time, location, status, access_token
        from sprechtage
       order by start_date
      """;

  private static final String KOPF =
      """
      select id, titel, start_date, location, schulkontakt, status
        from sprechtage
       where id = :id
      """;

  private static final String KLASSEN_EINES =
      """
      select klasse_id
        from sprechtage_klassen
       where sprechtag_id = :id
      """;

  private static final String KLASSEN_ALLER =
      """
      select sprechtag_id, klasse_id
        from sprechtage_klassen
      """;

  private final NamedParameterJdbcTemplate jdbc;

  @Override
  public List<SprechtagZeile> alle() {
    // Zwei Statements statt eines Joins: Der Join vervielfachte jede Kopfzeile je Klasse, und die
    // Klassen sind nur eine Id-Liste. Zwei Abfragen für die ganze Liste, nicht zwei je Zeile.
    Map<UUID, List<UUID>> klassenJeSprechtag = new LinkedHashMap<>();
    jdbc.query(
        KLASSEN_ALLER,
        Map.of(),
        rs -> {
          klassenJeSprechtag
              .computeIfAbsent(rs.getObject("sprechtag_id", UUID.class), k -> new ArrayList<>())
              .add(rs.getObject("klasse_id", UUID.class));
        });
    return jdbc.query(
        ZEILEN,
        Map.of(),
        (rs, zeile) -> {
          UUID id = rs.getObject("id", UUID.class);
          return new SprechtagZeile(
              id,
              rs.getString("titel"),
              rs.getDate("start_date").toLocalDate(),
              rs.getTime("start_time").toLocalTime(),
              rs.getTime("end_time").toLocalTime(),
              rs.getString("location"),
              SprechtagStatus.valueOf(rs.getString("status")),
              rs.getString("access_token"),
              klassenJeSprechtag.getOrDefault(id, List.of()));
        });
  }

  @Override
  public Optional<Kopf> kopf(SprechtagId id) {
    Map<String, Object> parameter = Map.of("id", id.wert());
    // Erst die Klassen, dann der Kopf: So entsteht der Record einmal fertig statt zweimal.
    List<UUID> klasseIds = jdbc.queryForList(KLASSEN_EINES, parameter, UUID.class);
    return jdbc
        .query(
            KOPF,
            parameter,
            (rs, zeile) ->
                new Kopf(
                    SprechtagId.von(rs.getObject("id", UUID.class)),
                    rs.getString("titel"),
                    rs.getDate("start_date").toLocalDate(),
                    rs.getString("location"),
                    rs.getString("schulkontakt"),
                    SprechtagStatus.valueOf(rs.getString("status")),
                    klasseIds))
        .stream()
        .findFirst();
  }
}
