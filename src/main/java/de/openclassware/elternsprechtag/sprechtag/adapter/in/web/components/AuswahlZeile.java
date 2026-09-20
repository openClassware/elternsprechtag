package de.openclassware.elternsprechtag.sprechtag.adapter.in.web.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.openclassware.elternsprechtag.sprechtag.adapter.Formats;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchungsoptionen.LehrkraftOption;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchungsoptionen.SlotOption;

/**
 * Eine Zeile der Auswahl-Zusammenfassung: Lehrkraft mit Uhrzeit, ihre Fächer und, sofern
 * geschrieben, die Notiz schreibgeschützt und im vollen Wortlaut.
 *
 * <p>{@code onSelect} und {@code onRemove} sind optional ({@code null} lässt sie weg) — die
 * Bestätigungsseite zeigt dieselbe Zeile nur an, ohne Sprung zur Lehrkraft oder Entfernen-Button.
 * Der Entfernen-Button liegt bewusst außerhalb der klickbaren Fläche, damit sein Klick nicht
 * zugleich das Panel aufklappt.
 */
public class AuswahlZeile extends Div {

  public AuswahlZeile(
      LehrkraftOption lehrkraft,
      SlotOption slot,
      String notiz,
      Runnable onSelect,
      Runnable onRemove) {
    addClassName("elternsprechtag-view__summary-row");

    Span badge = new Span(lehrkraft.kuerzel());
    badge.addClassName("elternsprechtag-view__summary-badge");

    Div info = createInfo(lehrkraft, slot, notiz);

    if (onSelect == null) {
      add(badge, info);
      return;
    }

    Div link = new Div();
    link.addClassName("elternsprechtag-view__summary-link");
    link.add(badge, info);
    link.addClickListener(event -> onSelect.run());
    add(link);

    if (onRemove != null) {
      Button remove = new Button(VaadinIcon.CLOSE_SMALL.create());
      remove.addClassName("elternsprechtag-view__summary-remove");
      remove.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL);
      remove.addClickListener(event -> onRemove.run());
      add(remove);
    }
  }

  private Div createInfo(LehrkraftOption lehrkraft, SlotOption slot, String notiz) {
    Div info = new Div();
    info.addClassName("elternsprechtag-view__summary-info");

    Div main = new Div();
    main.addClassName("elternsprechtag-view__summary-main");
    main.setText(
        getTranslation(
            "elternsprechtag.summary.row", lehrkraft.lehrerName(), Formats.time(slot.zeit())));

    Div sub = new Div();
    sub.addClassName("elternsprechtag-view__summary-sub");
    sub.setText(String.join(", ", lehrkraft.faecher()));
    info.add(main, sub);

    if (notiz != null && !notiz.isEmpty()) {
      Paragraph notizText = new Paragraph(notiz);
      notizText.addClassName("elternsprechtag-view__summary-notiz");
      info.add(notizText);
    }
    return info;
  }
}
