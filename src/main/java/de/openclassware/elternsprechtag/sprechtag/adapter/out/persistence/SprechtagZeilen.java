package de.openclassware.elternsprechtag.sprechtag.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

/**
 * Spring-Data-JDBC-Repository des Sprechtag-Aggregats: ein Repository je Aggregat-Root, Speichern
 * schreibt das ganze Aggregat, Laden lädt es ganz. Nur innerhalb des Persistenz-Adapters sichtbar —
 * nach außen gilt der Port {@code Sprechtage}.
 */
interface SprechtagZeilen extends CrudRepository<SprechtagZeile, UUID> {

  /**
   * Der Sprechtag hinter einem Elternlink. Als abgeleitete Query geschrieben, weil Spring Data JDBC
   * nur so auch die Kindzeilen (die Klassen) mitlädt — handgeschriebenes SQL läge zwar näher am
   * Stil der Query-Adapter, gäbe hier aber ein halbes Aggregat zurück.
   */
  Optional<SprechtagZeile> findByAccessToken(String accessToken);
}
