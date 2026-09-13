package de.openclassware.elternsprechtag.sprechtag.adapter.out.persistence;

import de.openclassware.elternsprechtag.sprechtag.application.port.out.Sprechtage;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Liest die Kopfdaten eines Sprechtags. Übergangsweise direkt auf der Tabelle, die noch dem
 * JPA-{@code Sprechtag} gehört — dieser Kontext liest sie hier nur, geschrieben wird sie weiterhin
 * ausschließlich vom {@code SprechtagService}.
 */
@RequiredArgsConstructor
@Component
class SprechtageJdbcAdapter implements Sprechtage {

  private static final String KOPF =
      """
      select id, titel, start_date, location, schulkontakt
        from sprechtage
       where id = :id
      """;

  private static final String KLASSEN =
      """
      select klasse_id
        from sprechtage_klassen
       where sprechtag_id = :id
      """;

  private final NamedParameterJdbcTemplate jdbc;

  @Override
  public Optional<Kopf> ladeKopf(SprechtagId id) {
    Map<String, Object> parameter = Map.of("id", id.wert());
    // Zwei Statements statt eines Joins: Der Join vervielfachte die Kopfzeile je Klasse, und die
    // Klassen sind nur eine Id-Liste.
    Optional<Kopf> kopf =
        jdbc
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
                        List.of()))
            .stream()
            .findFirst();
    if (kopf.isEmpty()) {
      return Optional.empty();
    }
    List<UUID> klasseIds = jdbc.queryForList(KLASSEN, parameter, UUID.class);
    Kopf ohneKlassen = kopf.get();
    return Optional.of(
        new Kopf(
            ohneKlassen.id(),
            ohneKlassen.titel(),
            ohneKlassen.datum(),
            ohneKlassen.ort(),
            ohneKlassen.schulkontakt(),
            klasseIds));
  }
}
