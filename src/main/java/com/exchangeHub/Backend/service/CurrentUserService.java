package com.exchangeHub.Backend.service;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.exchangeHub.Backend.entity.User;
import com.exchangeHub.Backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        // Récupérer l'authentification depuis SecurityContextHolder
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null) {
            throw new RuntimeException("Aucune authentification trouvée");
        }

        // Vérifier que le principal est un JWT
        if (!(authentication.getPrincipal() instanceof Jwt)) {
            throw new RuntimeException("Token JWT invalide");
        }

        Jwt jwt = (Jwt) authentication.getPrincipal();

        // Récupérer le claim "email" du token
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isEmpty()) {
            throw new RuntimeException("Email absent du token JWT");
        }

        // Chercher l'utilisateur en base par email
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (!userOptional.isPresent()) {
            throw new RuntimeException("Utilisateur introuvable en base (email: " + email + ")");
        }

        return userOptional.get();
    }
}
