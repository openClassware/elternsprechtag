package de.openclassware.elternsprechtag.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableConfigurationProperties(ElternsprechtagProperties.class)
@EnableAsync
@Configuration
public class ElternsprechtagConfiguration {

}

