package de.openclassware.elternsprechtag.sprechtag.adapter.out.persistence;

import de.openclassware.elternsprechtag.sprechtag.application.port.out.Klassen;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Liest die Klassen der Schulorganisation. Übergangsweise direkt auf deren Tabelle — mit dem Schnitt
 * des Stammdaten-Kontexts (#140) wird daraus ein echter Fremdkontext-Zugriff.
 */
@RequiredArgsConstructor
@Component
class KlassenJdbcAdapter implements Klassen {

  private static final String ALLE =
      """
      select id, name
        from klassen
       order by name
      """;

  private final NamedParameterJdbcTemplate jdbc;

  @Override
  public List<KlasseDaten> alle() {
    return jdbc.query(
        ALLE,
        Map.of(),
        (rs, zeile) -> new KlasseDaten(rs.getObject("id", UUID.class), rs.getString("name")));
  }
}
