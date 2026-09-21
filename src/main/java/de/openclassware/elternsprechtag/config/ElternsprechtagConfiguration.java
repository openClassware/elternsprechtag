package de.openclassware.elternsprechtag.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableConfigurationProperties(ElternsprechtagProperties.class)
@EnableAsync
@EnableScheduling
@Configuration
public class ElternsprechtagConfiguration {

}

