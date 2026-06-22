package de.openclassware.elternsprechtag.config;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import de.openclassware.elternsprechtag.ui.LoginView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) {
    http.with(
        VaadinSecurityConfigurer.vaadin(),
        configurer -> {
          configurer.loginView(LoginView.class);
        });
    return http.build();
  }

  @Value("${elternsprechtag.security.organizer.username}")
  private String organizerUsername;

  @Value("${elternsprechtag.security.organizer.password}")
  private String organizerPassword;

  @Bean
  public UserDetailsService userDetailsService() {
    UserDetails userDetails =
        User.withUsername(organizerUsername).password(organizerPassword).roles("ORGANIZER").build();

    return new InMemoryUserDetailsManager(userDetails);
  }
}
