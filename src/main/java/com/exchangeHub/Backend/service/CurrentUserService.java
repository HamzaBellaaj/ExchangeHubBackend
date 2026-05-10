package com.exchangeHub.Backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.exchangeHub.Backend.entity.User;
import com.exchangeHub.Backend.enums.Role;
import com.exchangeHub.Backend.exception.ForbiddenException;
import com.exchangeHub.Backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new ForbiddenException("Token JWT invalide");
        }

        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            throw new ForbiddenException("Email absent du token JWT");
        }

        return userRepository.findByEmail(email).orElseGet(() -> createUserFromJwt(jwt));
    }

    private User createUserFromJwt(Jwt jwt) {
        String keycloakId = jwt.getSubject(); // Le 'sub' du JWT correspond à l'ID Keycloak
        String email = jwt.getClaimAsString("email");
        String givenName = jwt.getClaimAsString("given_name");
        String familyName = jwt.getClaimAsString("family_name");
        
        // Extraction du rôle principal
        Role userRole = Role.CANDIDAT; // Rôle par défaut
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null && !roles.isEmpty()) {
            String roleName = roles.get(0).toUpperCase();
            try {
                userRole = Role.valueOf(roleName);
            } catch (IllegalArgumentException e) {
                // Ignore, garde le rôle par défaut
            }
        }

        User newUser = User.builder()
            .id(UUID.randomUUID()) // ID interne (base PostgreSQL)
            .keycloakId(keycloakId)
            .email(email)
            .prenom(givenName)
            .nom(familyName)
            .role(userRole)
            .actif(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        return userRepository.save(newUser);
    }
}
