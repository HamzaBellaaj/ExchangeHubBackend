package com.exchangeHub.Backend.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.exchangeHub.Backend.entity.Candidature;
import com.exchangeHub.Backend.entity.User;
import com.exchangeHub.Backend.enums.Role;
import com.exchangeHub.Backend.enums.StatutCandidature;
import com.exchangeHub.Backend.exception.BadRequestException;
import com.exchangeHub.Backend.exception.ForbiddenException;

@Service
public class CandidatureWorkflowService {

    private static final Map<StatutCandidature, Set<StatutCandidature>> TRANSITIONS = Map.of(
        StatutCandidature.SOUMISE, Set.of(StatutCandidature.EN_ANALYSE),
        StatutCandidature.EN_ANALYSE, Set.of(StatutCandidature.ANALYSEE),
        StatutCandidature.ANALYSEE, Set.of(StatutCandidature.ENTRETIEN_PLANIFIE),
        StatutCandidature.ENTRETIEN_PLANIFIE, Set.of(StatutCandidature.ENTRETIEN_TERMINE),
        StatutCandidature.ENTRETIEN_TERMINE, Set.of(
            StatutCandidature.ACCEPTEE,
            StatutCandidature.REFUSEE,
            StatutCandidature.LISTE_ATTENTE)
    );

    private static final Set<StatutCandidature> STATUTS_OPERATIONNELS = Set.of(
        StatutCandidature.EN_ANALYSE,
        StatutCandidature.ANALYSEE,
        StatutCandidature.ENTRETIEN_PLANIFIE,
        StatutCandidature.ENTRETIEN_TERMINE
    );

    private static final Set<StatutCandidature> STATUTS_FINAUX = Set.of(
        StatutCandidature.ACCEPTEE,
        StatutCandidature.REFUSEE,
        StatutCandidature.LISTE_ATTENTE
    );

    public void transition(Candidature candidature, StatutCandidature target, User actor) {
        if (actor == null) {
            throw new ForbiddenException("Utilisateur courant introuvable");
        }

        if (actor.getRole() == Role.CANDIDAT) {
            throw new ForbiddenException("Un candidat ne peut pas changer le statut d'une candidature");
        }

        checkRoleAllowed(target, actor.getRole());
        applyTransition(candidature, target);
    }

    public void transitionSystem(Candidature candidature, StatutCandidature target) {
        applyTransition(candidature, target);
    }

    private void checkRoleAllowed(StatutCandidature target, Role role) {
        if (role == Role.ADMIN) {
            return;
        }

        if (STATUTS_OPERATIONNELS.contains(target) && role == Role.COORDINATEUR) {
            return;
        }

        if (STATUTS_FINAUX.contains(target) && role == Role.RESPONSABLE) {
            return;
        }

        throw new ForbiddenException("Role non autorise pour ce changement de statut");
    }

    private void applyTransition(Candidature candidature, StatutCandidature target) {
        if (candidature == null) {
            throw new BadRequestException("Candidature obligatoire");
        }

        if (target == null) {
            throw new BadRequestException("Statut obligatoire");
        }

        if (target == StatutCandidature.ARCHIVEE) {
            throw new BadRequestException("Utilisez l'endpoint d'archivage pour archiver une candidature");
        }

        StatutCandidature current = candidature.getStatut();
        if (current == target) {
            return;
        }

        if (current == null || !TRANSITIONS.getOrDefault(current, Set.of()).contains(target)) {
            throw new BadRequestException("Transition statut invalide: " + current + " -> " + target);
        }

        candidature.setStatut(target);
        candidature.setUpdatedAt(LocalDateTime.now());
    }
}
