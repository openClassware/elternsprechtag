package de.openclassware.elternsprechtag.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("elternsprechtag")
public class ElternsprechtagProperties {

    public String schoolname;


}
