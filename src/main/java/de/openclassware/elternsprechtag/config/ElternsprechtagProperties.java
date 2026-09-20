package de.openclassware.elternsprechtag.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("elternsprechtag")
public class ElternsprechtagProperties {

    public String schoolname;

    /**
     * Stellvertreteradresse für Familien ohne eigene E-Mail-Adresse — nur beim Nachtragen relevant.
     * Bewusst ohne Beispiel-Default in {@code application.properties}: An diese Adresse gingen sonst
     * echte Absagen, sobald jemand die Instanz unkonfiguriert betreibt. {@code null}/leer bedeutet
     * „nicht konfiguriert" — dann bietet die Nachtrag-Ansicht den Schalter gar nicht erst an.
     */
    public String stellvertreteradresse;

}
