package de.openclassware.elternsprechtag.sprechtag.domain;

/**
 * Die Angaben einer noch geltenden Zusage an einem laufenden Sprechtag sollen fallen (Issue #129).
 *
 * <p>Das würde einen Geistertermin hinterlassen: Die Lehrkraft erwartet „Eltern-3f9a-001", es kommt
 * niemand, und den Slot bekommt auch keine andere Familie. Vor dem Sprechtag ist ein Löschverlangen
 * deshalb ein Storno mit entfernten Angaben — erst stornieren, dann entfernen.
 */
public class BuchungNochAktivException extends RuntimeException {

  public BuchungNochAktivException(String message) {
    super(message);
  }
}
