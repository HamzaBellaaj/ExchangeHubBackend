package com.exchangeHub.Backend.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.exchangeHub.Backend.dto.CreateEntretienRequest;
import com.exchangeHub.Backend.dto.EntretienResponse;
import com.exchangeHub.Backend.dto.UpdateEntretienStatusRequest;
import com.exchangeHub.Backend.entity.Candidature;
import com.exchangeHub.Backend.entity.Entretien;
import com.exchangeHub.Backend.entity.User;
import com.exchangeHub.Backend.enums.ModeEntretien;
import com.exchangeHub.Backend.enums.Role;
import com.exchangeHub.Backend.enums.StatutCandidature;
import com.exchangeHub.Backend.enums.StatutEntretien;
import com.exchangeHub.Backend.exception.BadRequestException;
import com.exchangeHub.Backend.exception.ConflictException;
import com.exchangeHub.Backend.exception.ForbiddenException;
import com.exchangeHub.Backend.exception.ResourceNotFoundException;
import com.exchangeHub.Backend.repository.CandidatureRepository;
import com.exchangeHub.Backend.repository.EntretienRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class EntretienService {

    private final EntretienRepository entretienRepository;
    private final CandidatureRepository candidatureRepository;
    private final CurrentUserService currentUserService;
    private final CandidatureWorkflowService candidatureWorkflowService;

    public EntretienResponse createEntretien(CreateEntretienRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        checkPlanningAccess(currentUser);
        validateCreateRequest(request);

        Candidature candidature = candidatureRepository.findById(request.getCandidatureId())
            .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable"));

        if (entretienRepository.existsByCandidature_Id(candidature.getId())) {
            throw new ConflictException("Cette candidature a deja un entretien");
        }

        candidatureWorkflowService.transition(candidature, StatutCandidature.ENTRETIEN_PLANIFIE, currentUser);

        LocalDateTime now = LocalDateTime.now();
        Entretien entretien = new Entretien();
        entretien.setId(UUID.randomUUID());
        entretien.setCandidature(candidature);
        entretien.setDateEntretien(request.getDateHeure());
        entretien.setMode(request.getMode());
        entretien.setLienVisio(request.getLienVisio());
        entretien.setLieu(request.getLieu());
        entretien.setStatut(StatutEntretien.PLANIFIE);
        entretien.setPlanifiePar(currentUser);
        entretien.setCreatedAt(now);
        entretien.setUpdatedAt(now);

        candidatureRepository.save(candidature);
        return mapToResponse(entretienRepository.save(entretien));
    }

    @Transactional(readOnly = true)
    public EntretienResponse getEntretien(UUID id) {
        Entretien entretien = entretienRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Entretien introuvable"));

        checkReadAccess(entretien.getCandidature());
        return mapToResponse(entretien);
    }

    @Transactional(readOnly = true)
    public EntretienResponse getEntretienByCandidature(UUID candidatureId) {
        Candidature candidature = candidatureRepository.findById(candidatureId)
            .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable"));

        checkReadAccess(candidature);

        Entretien entretien = entretienRepository.findByCandidature_Id(candidatureId)
            .orElseThrow(() -> new ResourceNotFoundException("Entretien introuvable"));

        return mapToResponse(entretien);
    }

    public EntretienResponse updateStatus(UUID id, UpdateEntretienStatusRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        checkPlanningAccess(currentUser);

        if (request == null || request.getStatut() == null) {
            throw new BadRequestException("Statut entretien obligatoire");
        }

        Entretien entretien = entretienRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Entretien introuvable"));

        if (request.getStatut() == StatutEntretien.TERMINE
            && entretien.getCandidature().getStatut() != StatutCandidature.ENTRETIEN_TERMINE) {
            candidatureWorkflowService.transition(
                entretien.getCandidature(),
                StatutCandidature.ENTRETIEN_TERMINE,
                currentUser);
            candidatureRepository.save(entretien.getCandidature());
        }

        entretien.setStatut(request.getStatut());
        entretien.setUpdatedAt(LocalDateTime.now());

        return mapToResponse(entretienRepository.save(entretien));
    }

    private void validateCreateRequest(CreateEntretienRequest request) {
        if (request == null) {
            throw new BadRequestException("Requete obligatoire");
        }

        if (request.getCandidatureId() == null) {
            throw new BadRequestException("candidatureId obligatoire");
        }

        if (request.getDateHeure() == null) {
            throw new BadRequestException("dateHeure obligatoire");
        }

        if (request.getMode() == null) {
            throw new BadRequestException("Mode entretien obligatoire");
        }

        if (request.getMode() == ModeEntretien.VISIO && isBlank(request.getLienVisio())) {
            throw new BadRequestException("lienVisio obligatoire pour un entretien VISIO");
        }

        if (request.getMode() == ModeEntretien.PRESENTIEL && isBlank(request.getLieu())) {
            throw new BadRequestException("lieu obligatoire pour un entretien PRESENTIEL");
        }

        if (request.getMode() != ModeEntretien.VISIO && request.getMode() != ModeEntretien.PRESENTIEL) {
            throw new BadRequestException("Mode entretien non supporte");
        }
    }

    private void checkPlanningAccess(User user) {
        if (user.getRole() != Role.COORDINATEUR && user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Seul un coordinateur ou un admin peut planifier ou modifier un entretien");
        }
    }

    private void checkReadAccess(Candidature candidature) {
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser.getRole() == Role.CANDIDAT
            && !candidature.getCandidat().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Acces refuse a cet entretien");
        }
    }

    private EntretienResponse mapToResponse(Entretien entretien) {
        User planifiePar = entretien.getPlanifiePar();

        return EntretienResponse.builder()
            .id(entretien.getId())
            .candidatureId(entretien.getCandidature().getId())
            .dateHeure(entretien.getDateEntretien())
            .mode(entretien.getMode())
            .lienVisio(entretien.getLienVisio())
            .lieu(entretien.getLieu())
            .statut(entretien.getStatut())
            .planifieParId(planifiePar != null ? planifiePar.getId() : null)
            .planifieParNom(planifiePar != null ? planifiePar.getNom() : null)
            .createdAt(entretien.getCreatedAt())
            .updatedAt(entretien.getUpdatedAt())
            .build();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
