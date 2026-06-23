package de.openclassware.elternsprechtag.ui;

import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.router.Route;
import de.openclassware.elternsprechtag.ui.layout.MainLayout;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "organizers", layout = MainLayout.class)
@RolesAllowed("ORGANIZER")
public class OrganizerView extends Main {

    public OrganizerView() {


    }

}
