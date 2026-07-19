package de.openclassware.elternsprechtag.services;

import de.openclassware.elternsprechtag.services.AbsageBenachrichtigungService.AbsageEmpfaenger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Test-Attrappe des {@link BenachrichtigungSender}-Ports: sammelt die übergebenen Empfänger und kann
 * für einzelne Adressen einen Zustellfehler simulieren, um das Best-effort-Verhalten zu prüfen.
 */
class FakeBenachrichtigungSender implements BenachrichtigungSender {

  final List<AbsageEmpfaenger> empfangen = new ArrayList<>();
  final Set<String> scheitertFuer = new HashSet<>();

  @Override
  public void sende(AbsageEmpfaenger empfaenger) {
    if (scheitertFuer.contains(empfaenger.email())) {
      throw new RuntimeException("Zustellung fehlgeschlagen: " + empfaenger.email());
    }
    empfangen.add(empfaenger);
  }

  void reset() {
    empfangen.clear();
    scheitertFuer.clear();
  }
}
