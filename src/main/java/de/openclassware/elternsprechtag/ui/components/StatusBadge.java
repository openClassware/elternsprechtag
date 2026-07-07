package de.openclassware.elternsprechtag.ui.components;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Span;
import de.openclassware.elternsprechtag.domain.SprechtagStatusEnum;

@CssImport("./styles/components/status-badge.css")
public class StatusBadge extends Span {

  public StatusBadge(SprechtagStatusEnum status) {
    addClassName("status-badge");
    addClassName(modifier(status));

    Span dot = new Span();
    dot.addClassName("status-badge__dot");

    Span label = new Span(getTranslation(labelKey(status)));
    label.addClassName("status-badge__label");

    add(dot, label);
  }

  private String modifier(SprechtagStatusEnum status) {
    return switch (status) {
      case ENTWURF -> "status-badge--draft";
      case VEROEFFENTLICHT -> "status-badge--active";
      case ABGESAGT -> "status-badge--cancelled";
      case ABGESCHLOSSEN -> "status-badge--done";
    };
  }

  private String labelKey(SprechtagStatusEnum status) {
    return switch (status) {
      case ENTWURF -> "sprechtag.status.entwurf";
      case VEROEFFENTLICHT -> "sprechtag.status.aktiv";
      case ABGESAGT -> "sprechtag.status.abgesagt";
      case ABGESCHLOSSEN -> "sprechtag.status.abgeschlossen";
    };
  }
}
