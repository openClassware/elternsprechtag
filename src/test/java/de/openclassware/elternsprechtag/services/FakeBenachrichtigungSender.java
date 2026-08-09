package de.openclassware.elternsprechtag.services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Test-Attrappe des {@link BenachrichtigungSender}-Ports: sammelt die übergebenen Nachrichten
 * (Empfänger, Betreff, Text) und kann für einzelne Adressen einen Zustellfehler simulieren, um das
 * Best-effort-Verhalten zu prüfen.
 */
class FakeBenachrichtigungSender implements BenachrichtigungSender {

  final List<Nachricht> empfangen = new ArrayList<>();
  /** Auch die gescheiterten Zustellungen — damit ein Test „versucht, aber fehlgeschlagen" von
   * „gar nicht erst versucht" unterscheiden kann. */
  final List<Nachricht> versucht = new ArrayList<>();
  final Set<String> scheitertFuer = new HashSet<>();

  @Override
  public void sende(Nachricht nachricht) {
    versucht.add(nachricht);
    if (scheitertFuer.contains(nachricht.empfaenger())) {
      throw new RuntimeException("Zustellung fehlgeschlagen: " + nachricht.empfaenger());
    }
    empfangen.add(nachricht);
  }

  void reset() {
    empfangen.clear();
    versucht.clear();
    scheitertFuer.clear();
  }
}
