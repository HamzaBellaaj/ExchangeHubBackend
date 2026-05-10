package com.exchangeHub.Backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.exchangeHub.Backend.dto.CandidatureArchiveResponse;
import com.exchangeHub.Backend.dto.CandidatureDetailResponse;
import com.exchangeHub.Backend.dto.CandidatureListResponse;
import com.exchangeHub.Backend.dto.CandidatureStatusResponse;
import com.exchangeHub.Backend.dto.CreateCandidatureRequest;
import com.exchangeHub.Backend.dto.DocumentResponse;
import com.exchangeHub.Backend.dto.DocumentValidationResponse;
import com.exchangeHub.Backend.dto.UpdateCandidatureStatusRequest;
import com.exchangeHub.Backend.entity.Candidature;
import com.exchangeHub.Backend.entity.Document;
import com.exchangeHub.Backend.entity.Programme;
import com.exchangeHub.Backend.entity.User;
import com.exchangeHub.Backend.enums.Role;
import com.exchangeHub.Backend.enums.StatutCandidature;
import com.exchangeHub.Backend.exception.BadRequestException;
import com.exchangeHub.Backend.exception.ConflictException;
import com.exchangeHub.Backend.exception.ForbiddenException;
import com.exchangeHub.Backend.exception.ResourceNotFoundException;
import com.exchangeHub.Backend.repository.CandidatureRepository;
import com.exchangeHub.Backend.repository.DocumentRepository;
import com.exchangeHub.Backend.repository.ProgrammeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CandidatureService {

    private final CandidatureRepository candidatureRepository;
    private final ProgrammeRepository programmeRepository;
    private final DocumentRepository documentRepository;
    private final CurrentUserService currentUserService;
    private final CandidatureWorkflowService candidatureWorkflowService;
    private final DocumentValidationService documentValidationService;

    public Candidature createCandidature(CreateCandidatureRequest request, User candidat) {
        if (candidat == null) {
            throw new BadRequestException("Candidat obligatoire");
        }

        if (candidat.getRole() != Role.CANDIDAT) {
            throw new ForbiddenException("Seul un candidat peut creer une candidature");
        }

        if (request == null || request.getProgrammeId() == null) {
            throw new BadRequestException("programmeId obligatoire");
        }

        Programme programme = programmeRepository.findById(request.getProgrammeId())
            .orElseThrow(() -> new ResourceNotFoundException("Programme introuvable"));

        if (candidatureRepository.existsByCandidatIdAndProgrammeId(candidat.getId(), request.getProgrammeId())) {
            throw new ConflictException("Une candidature existe deja pour ce candidat et ce programme");
        }

        LocalDateTime now = LocalDateTime.now();
        Candidature candidature = new Candidature();
        candidature.setId(UUID.randomUUID());
        candidature.setCandidat(candidat);
        candidature.setProgramme(programme);
        candidature.setStatut(StatutCandidature.SOUMISE);
        candidature.setSubmittedAt(now);
        candidature.setCreatedAt(now);
        candidature.setUpdatedAt(now);

        return candidatureRepository.save(candidature);
    }

    @Transactional(readOnly = true)
    public CandidatureDetailResponse getCandidature(UUID id) {
        Candidature candidature = findCandidatureOrThrow(id);
        checkCandidatureAccess(candidature);

        Programme programme = candidature.getProgramme();
        List<DocumentResponse> documentResponses = documentRepository.findByCandidature_Id(id).stream()
            .map(this::mapDocumentToResponse)
            .collect(Collectors.toList());

        return CandidatureDetailResponse.builder()
            .id(candidature.getId())
            .statut(candidature.getStatut())
            .submittedAt(candidature.getSubmittedAt())
            .programmeId(programme.getId())
            .programmeTitre(programme.getTitre())
            .programmePays(programme.getPays())
            .documents(documentResponses)
            .build();
    }

    public CandidatureStatusResponse updateStatus(UUID candidatureId, UpdateCandidatureStatusRequest request) {
        if (request == null || request.getStatut() == null) {
            throw new BadRequestException("Statut obligatoire");
        }

        Candidature candidature = findCandidatureOrThrow(candidatureId);
        User currentUser = currentUserService.getCurrentUser();

        if (request.getStatut() == StatutCandidature.EN_ANALYSE) {
            DocumentValidationResponse validation = documentValidationService.validateRequiredDocuments(candidature);
            if (!validation.isValid()) {
                throw new BadRequestException("Documents obligatoires manquants: " + validation.getMissingDocuments());
            }
        }

        candidatureWorkflowService.transition(candidature, request.getStatut(), currentUser);
        Candidature updated = candidatureRepository.save(candidature);

        return CandidatureStatusResponse.builder()
            .id(updated.getId())
            .statut(updated.getStatut())
            .updatedAt(updated.getUpdatedAt())
            .build();
    }

    @Transactional(readOnly = true)
    public List<CandidatureListResponse> getCandidatures(
        StatutCandidature statut,
        UUID programmeId,
        UUID candidatId,
        boolean includeArchived) {

        User currentUser = currentUserService.getCurrentUser();
        List<Candidature> candidatures;

        if (currentUser.getRole() == Role.CANDIDAT) {
            candidatures = includeArchived
                ? candidatureRepository.findByCandidatId(currentUser.getId())
                : candidatureRepository.findByCandidat_IdAndArchivedAtIsNull(currentUser.getId());
        } else {
            candidatures = includeArchived
                ? candidatureRepository.findAll()
                : candidatureRepository.findByArchivedAtIsNull();
        }

        return candidatures.stream()
            .filter(candidature -> statut == null || candidature.getStatut() == statut)
            .filter(candidature -> programmeId == null || candidature.getProgramme().getId().equals(programmeId))
            .filter(candidature -> currentUser.getRole() == Role.CANDIDAT
                || candidatId == null
                || candidature.getCandidat().getId().equals(candidatId))
            .map(this::mapToListResponse)
            .collect(Collectors.toList());
    }

    public CandidatureArchiveResponse archive(UUID candidatureId) {
        Candidature candidature = findCandidatureOrThrow(candidatureId);
        User currentUser = currentUserService.getCurrentUser();

        if (currentUser.getRole() == Role.CANDIDAT) {
            throw new ForbiddenException("Un candidat ne peut pas archiver une candidature");
        }

        if (candidature.getArchivedAt() != null) {
            throw new ConflictException("Candidature deja archivee");
        }

        candidature.setArchivedAt(LocalDateTime.now());
        candidature.setArchivedBy(currentUser);
        candidature.setStatut(StatutCandidature.ARCHIVEE);
        candidature.setUpdatedAt(LocalDateTime.now());

        Candidature archived = candidatureRepository.save(candidature);
        return CandidatureArchiveResponse.builder()
            .id(archived.getId())
            .statut(archived.getStatut())
            .archivedAt(archived.getArchivedAt())
            .archivedById(currentUser.getId())
            .archivedByNom(currentUser.getNom())
            .build();
    }

    private Candidature findCandidatureOrThrow(UUID id) {
        return candidatureRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable"));
    }

    private void checkCandidatureAccess(Candidature candidature) {
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser.getRole() == Role.CANDIDAT
            && !candidature.getCandidat().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Acces refuse a cette candidature");
        }
    }

    private CandidatureListResponse mapToListResponse(Candidature candidature) {
        return CandidatureListResponse.builder()
            .id(candidature.getId())
            .statut(candidature.getStatut())
            .submittedAt(candidature.getSubmittedAt())
            .programmeId(candidature.getProgramme().getId())
            .programmeTitre(candidature.getProgramme().getTitre())
            .candidatId(candidature.getCandidat().getId())
            .build();
    }

    private DocumentResponse mapDocumentToResponse(Document document) {
        return DocumentResponse.builder()
            .documentId(document.getId())
            .candidatureId(document.getCandidature().getId())
            .typeDocument(document.getTypeDocument())
            .fileName(document.getFileName())
            .storagePath(document.getStoragePath())
            .fileUrl(document.getFileUrl())
            .mimeType(document.getMimeType())
            .size(document.getSize())
            .uploadedAt(document.getUploadedAt())
            .build();
    }
}
