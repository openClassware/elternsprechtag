package de.openclassware.elternsprechtag;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import de.openclassware.elternsprechtag.sprechtag.domain.AggregateRoot;
import de.openclassware.elternsprechtag.sprechtag.domain.Buchung;
import org.junit.jupiter.api.Test;

/**
 * Die Architekturregeln aus ADR 0003 als Test, nicht als Absichtserklärung.
 *
 * <p>Der Grund steht in der ADR: Regeln, die niemand prüfen kann, driften — die Findings F1–F9 sind
 * der Beleg, und zwar bei deutlich einfacheren Regeln als diesen. Fragen wie „kennt dieser View ein
 * Aggregat?" sind im Review nicht zuverlässig zu beantworten, im Test schon.
 *
 * <p>Geprüft wird ausschließlich der neue Kontext. Die noch nicht migrierten Pakete
 * ({@code domain}, {@code repositories}, {@code services}) stehen bewusst außen vor — sie sind der
 * Bestand, den die folgenden Scheiben abräumen.
 */
class ArchitekturTest {

  private static final String BASIS = "de.openclassware.elternsprechtag";
  private static final String SPRECHTAG = BASIS + ".sprechtag";

  private final JavaClasses klassen =
      new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS).importPackages(BASIS);

  @Test
  void domaene_importiertNurJdk() {
    noClasses()
        .that()
        .resideInAPackage(SPRECHTAG + ".domain..")
        .should()
        .dependOnClassesThat()
        .resideOutsideOfPackages(SPRECHTAG + ".domain..", "java..", "javax..")
        .as("Die Domäne ist technologiefrei: kein Spring, kein Lombok, kein JPA, kein Vaadin")
        .check(klassen);
  }

  @Test
  void keinViewKenntEinAggregat() {
    noClasses()
        .that()
        .resideInAnyPackage(BASIS + ".ui..", SPRECHTAG + ".adapter.in..")
        .should()
        .dependOnClassesThat()
        .areAssignableTo(AggregateRoot.class)
        .orShould()
        .dependOnClassesThat()
        .haveFullyQualifiedName(Buchung.class.getName())
        .as("Über die Presenter-Grenze gehen ausschließlich Records, nie ein Aggregat")
        .check(klassen);
  }

  @Test
  void adapterKennenEinanderNicht() {
    slices()
        .matching(SPRECHTAG + ".adapter.(**)")
        .should()
        .notDependOnEachOther()
        .as("Ein Adapter erfüllt einen Port; ein zweiter Adapter ist ihm gleichgültig")
        .check(klassen);
  }

  @Test
  void anwendungKenntKeinenAdapter() {
    noClasses()
        .that()
        .resideInAPackage(SPRECHTAG + ".application..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage(SPRECHTAG + ".adapter..")
        .as("Die Abhängigkeit zeigt nach innen: Adapter kennen Ports, nicht umgekehrt")
        .check(klassen);
  }

  @Test
  void domaeneKenntKeineAnwendungUndKeinenAdapter() {
    noClasses()
        .that()
        .resideInAPackage(SPRECHTAG + ".domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(SPRECHTAG + ".application..", SPRECHTAG + ".adapter..")
        .as("Der Kern weiß nichts von dem, was ihn benutzt")
        .check(klassen);
  }
}
