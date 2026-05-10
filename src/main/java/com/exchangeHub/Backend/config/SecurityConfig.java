package com.exchangeHub.Backend.config;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles == null) {
                return Collections.emptySet();
            }

            return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toSet());
        });
        return converter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz -> authz
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/n8n/cv-standardization/callback").permitAll()
                .requestMatchers(HttpMethod.POST, "/cv-standardization/import").permitAll()

                .requestMatchers(HttpMethod.GET, "/programmes", "/programmes/**")
                .hasAnyRole("CANDIDAT", "COORDINATEUR", "RESPONSABLE", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/programmes")
                .hasAnyRole("RESPONSABLE", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/programmes/**")
                .hasAnyRole("RESPONSABLE", "ADMIN")

                .requestMatchers(HttpMethod.POST, "/candidatures")
                .hasRole("CANDIDAT")
                .requestMatchers(HttpMethod.GET, "/candidatures", "/candidatures/**")
                .hasAnyRole("CANDIDAT", "COORDINATEUR", "RESPONSABLE", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/candidatures/**/statut")
                .hasAnyRole("COORDINATEUR", "RESPONSABLE", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/candidatures/**/archive")
                .hasAnyRole("COORDINATEUR", "RESPONSABLE", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/candidatures/**/standardize-cv")
                .denyAll()

                .requestMatchers(HttpMethod.POST, "/entretiens")
                .hasAnyRole("COORDINATEUR", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/entretiens/**")
                .hasAnyRole("COORDINATEUR", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/entretiens/**")
                .hasAnyRole("CANDIDAT", "COORDINATEUR", "RESPONSABLE", "ADMIN")

                .requestMatchers(HttpMethod.POST, "/decisions")
                .hasAnyRole("RESPONSABLE", "ADMIN")

                .requestMatchers(HttpMethod.POST, "/documents/upload")
                .hasAnyRole("CANDIDAT", "COORDINATEUR", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/documents/**")
                .hasAnyRole("CANDIDAT", "COORDINATEUR", "RESPONSABLE", "ADMIN")

                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }
}
