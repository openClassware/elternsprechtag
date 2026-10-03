package de.openclassware.elternsprechtag.sprechtag.adapter.out.persistence;

import de.openclassware.elternsprechtag.sprechtag.application.port.out.Zustellungen;
import de.openclassware.elternsprechtag.sprechtag.domain.Zustellung;
import java.sql.Timestamp;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;

/**
 * Erfüllt den {@code Zustellungen}-Port mit handgeschriebenem SQL statt Spring Data JDBC: Der
 * Schlüssel ist zusammengesetzt (Buchung, Art), und geschrieben wird immer als Upsert — eine neue
 * Nachricht derselben Art ersetzt die vorige, ohne dass jemand sie vorher laden müsste. Version und
 * Neu-Erkennung, für die Spring Data hier sorgen würde, braucht dieses Aggregat nicht.
 */
@RequiredArgsConstructor
@Component
class ZustellungenJdbcAdapter implements Zustellungen {

  private static final String UPSERT =
      """
      insert into zustellungen (buchung_id, art, ergebnis, zeitpunkt)
      values (:buchungId, :art, :ergebnis, :zeitpunkt)
      on conflict (buchung_id, art)
      do update set ergebnis = excluded.ergebnis, zeitpunkt = excluded.zeitpunkt
      """;

  private final NamedParameterJdbcTemplate jdbc;

  @Override
  public void speichere(List<Zustellung> zustellungen) {
    if (zustellungen.isEmpty()) {
      return;
    }
    SqlParameterSource[] zeilen =
        zustellungen.stream()
            .map(
                zustellung ->
                    new MapSqlParameterSource()
                        .addValue("buchungId", zustellung.buchung().wert())
                        .addValue("art", zustellung.art().name())
                        .addValue("ergebnis", zustellung.ergebnis().name())
                        .addValue("zeitpunkt", Timestamp.valueOf(zustellung.zeitpunkt())))
            .toArray(SqlParameterSource[]::new);
    jdbc.batchUpdate(UPSERT, zeilen);
  }
}
