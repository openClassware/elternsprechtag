package de.openclassware.elternsprechtag.sprechtag.adapter.in.web.components;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.openclassware.elternsprechtag.sprechtag.adapter.Formats;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchungsoptionen.LehrkraftOption;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchungsoptionen.SlotOption;

/**
 * Karte einer Lehrkraft in der Buchungsliste: Kürzel, Name, Fächer und, sofern schon gewählt, ihr
 * Termin als Pille. Ein Klick auf die Karte klappt sie auf ({@code onClick}); ob sie aufgeklappt
 * oder bereits gewählt ist, entscheidet der Aufrufer über {@code selected}.
 */
public class LehrkraftKarte extends Div {

  public LehrkraftKarte(
      LehrkraftOption lehrkraft, boolean selected, SlotOption gewaehlterSlot, Runnable onClick) {
    addClassName("elternsprechtag-view__lehrkraft");
    if (selected) {
      addClassName("elternsprechtag-view__lehrkraft--selected");
    }

    Span badge = new Span(lehrkraft.kuerzel());
    badge.addClassName("elternsprechtag-view__lehrkraft-badge");

    Div info = new Div();
    info.addClassName("elternsprechtag-view__lehrkraft-info");
    Div name = new Div();
    name.addClassName("elternsprechtag-view__lehrkraft-name");
    name.setText(lehrkraft.lehrerName());
    Div faecher = new Div();
    faecher.addClassName("elternsprechtag-view__lehrkraft-faecher");
    faecher.setText(String.join(", ", lehrkraft.faecher()));
    info.add(name, faecher);

    add(badge, info);

    if (gewaehlterSlot != null) {
      Span pill = new Span();
      pill.addClassName("elternsprechtag-view__lehrkraft-pill");
      pill.add(VaadinIcon.CHECK.create(), new Span(Formats.time(gewaehlterSlot.zeit())));
      add(pill);
    }

    addClickListener(event -> onClick.run());
  }
}
