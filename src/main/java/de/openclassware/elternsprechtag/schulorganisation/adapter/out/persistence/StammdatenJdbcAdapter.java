package de.openclassware.elternsprechtag.schulorganisation.adapter.out.persistence;

import de.openclassware.elternsprechtag.schulorganisation.application.port.out.Stammdatenansichten;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Der Leseweg: handgeschriebenes SQL über die Stammdaten-Tabellen, vorbei an den Aggregaten
 * (ADR 0004).
 *
 * <p>Die Joins hier laufen alle <b>innerhalb</b> dieses Kontexts — Lehrauftrag, Lehrkraft, Klasse
 * und Fach liegen alle in ihm. Über die Kontextgrenze joint nichts; was der Sprechtag zusätzlich
 * braucht, fügt er in Java zusammen (ADR 0003).
 */
@RequiredArgsConstructor
@Component
class StammdatenJdbcAdapter implements Stammdatenansichten {

  private static final String LEHRAUFTRAG =
      """
      select la.id        as id,
             la.lehrer_id as lehrer_id,
             l.kuerzel    as kuerzel,
             l.vorname    as vorname,
             l.nachname   as nachname,
             k.name       as klasse_name,
             f.name       as fach_name
        from lehrauftrag la
        join lehrer  l on l.id = la.lehrer_id
        join klassen k on k.id = la.klasse_id
        join faecher f on f.id = la.fach_id
      """;

  private static final String NACH_ID = LEHRAUFTRAG + " where la.id = :id";

  /**
   * Stillgelegt heißt: nimmt an nichts Neuem mehr teil. Die Bedingung gilt für alle vier beteiligten
   * Datensätze — eine stillgelegte Lehrkraft steht auch dann nicht zur Wahl, wenn ihr Lehrauftrag
   * noch aktiv ist.
   */
  private static final String NACH_KLASSE =
      LEHRAUFTRAG
          + """
           where la.klasse_id = :klasseId
             and not la.stillgelegt
             and not l.stillgelegt
             and not k.stillgelegt
             and not f.stillgelegt
           order by f.name
          """;

  private static final String KLASSEN =
      """
      select id, name
        from klassen
      """;

  private static final String AKTIVE_KLASSEN = KLASSEN + " where not stillgelegt order by name";

  private static final String ALLE_KLASSEN = KLASSEN + " order by name";

  private static final RowMapper<KlasseDaten> KLASSE_MAPPER =
      (rs, zeile) -> new KlasseDaten(rs.getObject("id", UUID.class), rs.getString("name"));

  private static final RowMapper<LehrauftragDaten> LEHRAUFTRAG_MAPPER =
      (rs, zeile) ->
          new LehrauftragDaten(
              rs.getObject("id", UUID.class),
              rs.getObject("lehrer_id", UUID.class),
              rs.getString("kuerzel"),
              rs.getString("vorname"),
              rs.getString("nachname"),
              rs.getString("klasse_name"),
              rs.getString("fach_name"));

  private final NamedParameterJdbcTemplate jdbc;

  @Override
  public List<LehrauftragDaten> lehrauftraegeEinerKlasse(UUID klasseId) {
    return jdbc.query(NACH_KLASSE, Map.of("klasseId", klasseId), LEHRAUFTRAG_MAPPER);
  }

  @Override
  public Optional<LehrauftragDaten> lehrauftrag(UUID lehrauftragId) {
    return jdbc.query(NACH_ID, Map.of("id", lehrauftragId), LEHRAUFTRAG_MAPPER).stream().findFirst();
  }

  @Override
  public List<KlasseDaten> aktiveKlassen() {
    return jdbc.query(AKTIVE_KLASSEN, Map.of(), KLASSE_MAPPER);
  }

  @Override
  public List<KlasseDaten> alleKlassen() {
    return jdbc.query(ALLE_KLASSEN, Map.of(), KLASSE_MAPPER);
  }
}
