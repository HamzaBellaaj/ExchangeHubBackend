package com.exchangeHub.Backend.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.exchangeHub.Backend.dto.CreateDecisionRequest;
import com.exchangeHub.Backend.dto.DecisionResponse;
import com.exchangeHub.Backend.entity.Candidature;
import com.exchangeHub.Backend.entity.Decision;
import com.exchangeHub.Backend.entity.User;
import com.exchangeHub.Backend.enums.Role;
import com.exchangeHub.Backend.enums.StatutCandidature;
import com.exchangeHub.Backend.enums.TypeDecision;
import com.exchangeHub.Backend.exception.BadRequestException;
import com.exchangeHub.Backend.exception.ConflictException;
import com.exchangeHub.Backend.exception.ForbiddenException;
import com.exchangeHub.Backend.exception.ResourceNotFoundException;
import com.exchangeHub.Backend.repository.CandidatureRepository;
import com.exchangeHub.Backend.repository.DecisionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DecisionService {

    private final DecisionRepository decisionRepository;
    private final CandidatureRepository candidatureRepository;
    private final CurrentUserService currentUserService;
    private final CandidatureWorkflowService candidatureWorkflowService;

    public DecisionResponse createDecision(CreateDecisionRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        checkDecisionAccess(currentUser);
        validateRequest(request);

        Candidature candidature = candidatureRepository.findById(request.getCandidatureId())
            .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable"));

        if (decisionRepository.existsByCandidature_Id(candidature.getId())) {
            throw new ConflictException("Cette candidature a deja une decision");
        }

        StatutCandidature targetStatus = mapDecisionToStatus(request.getDecision());
        candidatureWorkflowService.transition(candidature, targetStatus, currentUser);

        LocalDateTime now = LocalDateTime.now();
        Decision decision = new Decision();
        decision.setId(UUID.randomUUID());
        decision.setCandidature(candidature);
        decision.setDecision(request.getDecision());
        decision.setCommentaire(request.getCommentaire());
        decision.setResponsable(currentUser);
        decision.setDecidedAt(now);

        candidatureRepository.save(candidature);
        return mapToResponse(decisionRepository.save(decision));
    }

    @Transactional(readOnly = true)
    public DecisionResponse getDecisionByCandidature(UUID candidatureId) {
        Candidature candidature = candidatureRepository.findById(candidatureId)
            .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable"));

        checkReadAccess(candidature);

        Decision decision = decisionRepository.findByCandidature_Id(candidatureId)
            .orElseThrow(() -> new ResourceNotFoundException("Decision introuvable"));

        return mapToResponse(decision);
    }

    private void validateRequest(CreateDecisionRequest request) {
        if (request == null) {
            throw new BadRequestException("Requete obligatoire");
        }

        if (request.getCandidatureId() == null) {
            throw new BadRequestException("candidatureId obligatoire");
        }

        if (request.getDecision() == null) {
            throw new BadRequestException("decision obligatoire");
        }
    }

    private void checkDecisionAccess(User currentUser) {
        if (currentUser.getRole() != Role.RESPONSABLE && currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Seul un responsable ou un admin peut prendre une decision");
        }
    }

    private void checkReadAccess(Candidature candidature) {
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser.getRole() == Role.CANDIDAT
            && !candidature.getCandidat().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Acces refuse a cette decision");
        }
    }

    private StatutCandidature mapDecisionToStatus(TypeDecision decision) {
        return switch (decision) {
            case ACCEPTEE -> StatutCandidature.ACCEPTEE;
            case REFUSEE -> StatutCandidature.REFUSEE;
            case LISTE_ATTENTE -> StatutCandidature.LISTE_ATTENTE;
        };
    }

    private DecisionResponse mapToResponse(Decision decision) {
        User responsable = decision.getResponsable();

        return DecisionResponse.builder()
            .id(decision.getId())
            .candidatureId(decision.getCandidature().getId())
            .decision(decision.getDecision())
            .commentaire(decision.getCommentaire())
            .responsableId(responsable != null ? responsable.getId() : null)
            .responsableNom(responsable != null ? responsable.getNom() : null)
            .decidedAt(decision.getDecidedAt())
            .createdAt(decision.getDecidedAt())
            .build();
    }
}
