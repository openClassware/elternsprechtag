package de.openclassware.elternsprechtag.ui.component;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import lombok.Getter;

@CssImport("./styles/form-panel.css")
public class FormPanel extends Div {

    private final Div title;
    private final Div description;
    @Getter
    private final FormLayout formLayout;

    public FormPanel() {
        this.title = new Div();
        this.title.addClassName("form-panel__title");
        this.description = new Div();
        this.description.addClassName("form-panel__description");
        this.formLayout = new FormLayout();
        this.formLayout.setAutoResponsive(true);
        this.formLayout.setExpandFields(true);
        this.formLayout.setExpandColumns(true);
        this.formLayout.setWidthFull();
        this.formLayout.addClassName("form-panel__form");
        Div div = new Div();
        div.add(title, description);
        add(div, formLayout);
        addClassName("form-panel");
    }

    @Override
    public void setTitle(String title) {
       this.title.setText(title);
    }

    public void setDescription(String description) {
        this.description.setText(description);
    }

}
