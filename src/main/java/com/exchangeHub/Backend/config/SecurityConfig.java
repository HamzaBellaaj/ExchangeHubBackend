package com.exchangeHub.Backend.config;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // Extraire les rôles du JWT depuis le claim "roles"
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
            // Désactiver CSRF
            .csrf(csrf -> csrf.disable())
            
            // Configurer le contrôle d'accès
            .authorizeHttpRequests(authz -> authz
                // Endpoints publics
                .requestMatchers("/error").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("OPTIONS", "/**").permitAll()
                
                // POST /candidatures → CANDIDAT
                .requestMatchers("POST", "/candidatures").hasRole("CANDIDAT")
                
                // GET /candidatures/** → CANDIDAT, COORDINATEUR, RESPONSABLE, ADMIN
                .requestMatchers("GET", "/candidatures/**").hasAnyRole("CANDIDAT", "COORDINATEUR", "RESPONSABLE", "ADMIN")
                
                // PATCH /candidatures/**/statut → COORDINATEUR, RESPONSABLE, ADMIN
                .requestMatchers("PATCH", "/candidatures/**/statut").hasAnyRole("COORDINATEUR", "RESPONSABLE", "ADMIN")
                
                // POST /documents/upload → CANDIDAT, COORDINATEUR, ADMIN
                .requestMatchers("POST", "/documents/upload").hasAnyRole("CANDIDAT", "COORDINATEUR", "ADMIN")
                
                // GET /documents/** → CANDIDAT, COORDINATEUR, RESPONSABLE, ADMIN
                .requestMatchers("GET", "/documents/**").hasAnyRole("CANDIDAT", "COORDINATEUR", "RESPONSABLE", "ADMIN")
                
                // Tous les autres endpoints nécessitent l'authentification
                .anyRequest().authenticated()
            )
            
            // Configurer OAuth2 Resource Server avec JWT
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            )
            
            // Utiliser JWT sans sessions
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }
}

