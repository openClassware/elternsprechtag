package de.openclassware.elternsprechtag.ui;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

@Route("")
@RolesAllowed("ORGANIZER")
public class RootView extends Main {

    public RootView() {

        add(new H1("Welcome!"));

    }
}
