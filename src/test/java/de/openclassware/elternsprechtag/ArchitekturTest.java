package de.openclassware.elternsprechtag;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import de.openclassware.elternsprechtag.schulorganisation.domain.Aggregat;
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
 * <p>Geprüft werden die beiden migrierten Kontexte. Das noch nicht migrierte Paket
 * ({@code services}, der Versand) steht bewusst außen vor — es ist der Bestand, den die letzte
 * Scheibe abräumt.
 */
class ArchitekturTest {

  private static final String BASIS = "de.openclassware.elternsprechtag";
  private static final String SPRECHTAG = BASIS + ".sprechtag";
  private static final String SCHULORGANISATION = BASIS + ".schulorganisation";

  private final JavaClasses klassen =
      new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS).importPackages(BASIS);

  @Test
  void domaene_importiertNurJdk() {
    for (String kontext : new String[] {SPRECHTAG, SCHULORGANISATION}) {
      noClasses()
          .that()
          .resideInAPackage(kontext + ".domain..")
          .should()
          .dependOnClassesThat()
          .resideOutsideOfPackages(kontext + ".domain..", "java..", "javax..")
          .as("Die Domäne ist technologiefrei: kein Spring, kein Lombok, kein JPA, kein Vaadin")
          .check(klassen);
    }
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
  void aggregateReferenzierenEinanderNurUeberTypisierteIds() {
    noFields()
        .that()
        .areDeclaredInClassesThat()
        .areAssignableTo(AggregateRoot.class)
        .or()
        .areDeclaredInClassesThat()
        .areAssignableTo(Aggregat.class)
        .should()
        .haveRawType(assignableTo(AggregateRoot.class))
        .orShould()
        .haveRawType(assignableTo(Aggregat.class))
        .as("Über eine Aggregat-Grenze führt eine typisierte Id, keine Objektnavigation")
        .check(klassen);
  }

  @Test
  void adapterKennenEinanderNicht() {
    for (String kontext : new String[] {SPRECHTAG, SCHULORGANISATION}) {
      slices()
          .matching(kontext + ".adapter.(**)")
          .should()
          .notDependOnEachOther()
          .as("Ein Adapter erfüllt einen Port; ein zweiter Adapter ist ihm gleichgültig")
          .check(klassen);
    }
  }

  @Test
  void anwendungKenntKeinenAdapter() {
    for (String kontext : new String[] {SPRECHTAG, SCHULORGANISATION}) {
      noClasses()
          .that()
          .resideInAPackage(kontext + ".application..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage(kontext + ".adapter..")
          .as("Die Abhängigkeit zeigt nach innen: Adapter kennen Ports, nicht umgekehrt")
          .check(klassen);
    }
  }

  @Test
  void domaeneKenntKeineAnwendungUndKeinenAdapter() {
    for (String kontext : new String[] {SPRECHTAG, SCHULORGANISATION}) {
      noClasses()
          .that()
          .resideInAPackage(kontext + ".domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(kontext + ".application..", kontext + ".adapter..")
          .as("Der Kern weiß nichts von dem, was ihn benutzt")
          .check(klassen);
    }
  }

  // --- Die Kontextgrenze (Issue #140) -----------------------------------------------------------

  /**
   * Die Abhängigkeit ist <b>einseitig</b>: Der Sprechtag liest Stammdaten, die Schulorganisation
   * weiß von Sprechtagen nichts.
   *
   * <p>Das ist die Regel, ohne die der zweite Kontext keiner wäre. Ein einziger Import in die
   * Gegenrichtung — und sei es nur eine {@code KlasseId}, die sich bequem wiederverwenden ließe —
   * macht aus zwei Kontexten ein Paket mit zwei Unterordnern.
   */
  @Test
  void dieSchulorganisationKenntDenSprechtagNicht() {
    noClasses()
        .that()
        .resideInAPackage(SCHULORGANISATION + "..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage(SPRECHTAG + "..")
        .as("Die Schulorganisation weiß nichts vom Sprechtag — die Abhängigkeit ist einseitig")
        .check(klassen);
  }

  /**
   * Und in der erlaubten Richtung geht es nur durch die Tür: über {@code port/in}, nicht an den
   * Aggregaten, Ports out oder Persistenzmodellen des anderen Kontexts vorbei.
   */
  @Test
  void derSprechtagErreichtDieSchulorganisationNurUeberDerenPortIn() {
    noClasses()
        .that()
        .resideInAPackage(SPRECHTAG + "..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            SCHULORGANISATION + ".domain..",
            SCHULORGANISATION + ".adapter..",
            SCHULORGANISATION + ".application.service..",
            SCHULORGANISATION + ".application.port.out..")
        .as("In einen fremden Kontext führt genau ein Weg: sein port/in")
        .check(klassen);
  }

  /**
   * Die Schulorganisation hat <b>keinen Web-Adapter</b>. Alle Anwendungsfälle mit Oberfläche gehören
   * dem Sprechtag; entstünde hier eine zweite Eingangstür, gäbe es zwei Orte, an denen dieselbe
   * Frage beantwortet wird.
   */
  @Test
  void dieSchulorganisationHatKeinenWebAdapter() {
    noClasses()
        .should()
        .resideInAPackage(SCHULORGANISATION + ".adapter.in..")
        .as("Die Schulorganisation hat keine Oberfläche — ihre Use Cases ruft der Sprechtag")
        .check(klassen);
  }
}
