package com.exchangeHub.Backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.exchangeHub.Backend.dto.CandidatureDetailResponse;
import com.exchangeHub.Backend.dto.CandidatureListResponse;
import com.exchangeHub.Backend.dto.CandidatureStatusResponse;
import com.exchangeHub.Backend.dto.CreateCandidatureRequest;
import com.exchangeHub.Backend.dto.DocumentResponse;
import com.exchangeHub.Backend.dto.UpdateCandidatureStatusRequest;
import com.exchangeHub.Backend.entity.Candidature;
import com.exchangeHub.Backend.entity.Document;
import com.exchangeHub.Backend.entity.Programme;
import com.exchangeHub.Backend.entity.User;
import com.exchangeHub.Backend.enums.Role;
import com.exchangeHub.Backend.enums.StatutCandidature;
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

    public Candidature createCandidature(CreateCandidatureRequest request, User candidat) {
        // Vérifier que le candidat n'est pas null
        if (candidat == null) {
            throw new RuntimeException("Candidat null");
        }

        // Vérifier que c'est un candidat
        if (candidat.getRole() != Role.CANDIDAT) {
            throw new RuntimeException("L'utilisateur n'est pas un candidat");
        }

        // Vérifier que la requête n'est pas null
        if (request == null) {
            throw new RuntimeException("Requête null");
        }

        // Récupérer le programme
        Optional<Programme> programmeOptional = programmeRepository.findById(request.getProgrammeId());
        if (!programmeOptional.isPresent()) {
            throw new RuntimeException("Programme introuvable");
        }
        Programme programme = programmeOptional.get();

        // Vérifier qu'il n'existe pas déjà une candidature
        boolean candidatureExiste = candidatureRepository.existsByCandidatIdAndProgrammeId(
            candidat.getId(), request.getProgrammeId());
        if (candidatureExiste) {
            throw new RuntimeException("Une candidature existe déjà pour ce candidat et ce programme");
        }

        // Créer la candidature
        Candidature candidature = new Candidature();
        candidature.setId(UUID.randomUUID());
        candidature.setCandidat(candidat);
        candidature.setProgramme(programme);
        candidature.setStatut(StatutCandidature.SOUMISE);
        candidature.setSubmittedAt(LocalDateTime.now());
        candidature.setCreatedAt(LocalDateTime.now());
        candidature.setUpdatedAt(LocalDateTime.now());

        // Sauvegarder et retourner
        Candidature result = candidatureRepository.save(candidature);
        return result;
    }

    public CandidatureDetailResponse getCandidature(UUID id) {
        // Récupérer la candidature
        Optional<Candidature> candidatureOptional = candidatureRepository.findById(id);
        if (!candidatureOptional.isPresent()) {
            throw new RuntimeException("Candidature introuvable");
        }
        Candidature candidature = candidatureOptional.get();

        // Vérifier l'accès : un candidat ne peut accéder qu'à ses propres candidatures
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser.getRole() == Role.CANDIDAT) {
            if (!candidature.getCandidat().getId().equals(currentUser.getId())) {
                throw new RuntimeException("Accès refusé à cette candidature");
            }
        }

        // Récupérer le programme
        Programme programme = candidature.getProgramme();

        // Récupérer les documents
        List<Document> documents = documentRepository.findByCandidatureId(id);
        List<DocumentResponse> documentResponses = documents.stream()
            .map(doc -> DocumentResponse.builder()
                .documentId(doc.getId())
                .candidatureId(doc.getCandidature().getId())
                .typeDocument(doc.getTypeDocument())
                .fileName(doc.getFileName())
                .storagePath(doc.getStoragePath())
                .fileUrl(doc.getFileUrl())
                .mimeType(doc.getMimeType())
                .size(doc.getSize())
                .uploadedAt(doc.getUploadedAt())
                .build())
            .collect(Collectors.toList());

        // Construire et retourner la réponse
        CandidatureDetailResponse response = CandidatureDetailResponse.builder()
            .id(candidature.getId())
            .statut(candidature.getStatut())
            .submittedAt(candidature.getSubmittedAt())
            .programmeId(programme.getId())
            .programmeTitre(programme.getTitre())
            .programmePays(programme.getPays())
            .documents(documentResponses)
            .build();

        return response;
    }

    public CandidatureStatusResponse updateStatus(UUID candidatureId, UpdateCandidatureStatusRequest request) {
        // Récupérer la candidature par id
        Optional<Candidature> candidatureOptional = candidatureRepository.findById(candidatureId);
        if (!candidatureOptional.isPresent()) {
            throw new RuntimeException("Candidature introuvable");
        }
        Candidature candidature = candidatureOptional.get();

        // Vérifier que la requête n'est pas null
        if (request == null) {
            throw new RuntimeException("Requête null");
        }

        // Vérifier que le statut n'est pas null
        if (request.getStatut() == null) {
            throw new RuntimeException("Statut null");
        }

        // Mettre à jour le statut
        candidature.setStatut(request.getStatut());
        candidature.setUpdatedAt(LocalDateTime.now());

        // Sauvegarder
        Candidature updated = candidatureRepository.save(candidature);

        // Retourner la réponse
        CandidatureStatusResponse response = CandidatureStatusResponse.builder()
            .id(updated.getId())
            .statut(updated.getStatut())
            .updatedAt(updated.getUpdatedAt())
            .build();

        return response;
    }

    public List<CandidatureListResponse> getCandidatures(
        StatutCandidature statut,
        UUID programmeId,
        UUID candidatId) {

        // Récupérer l'utilisateur courant
        User currentUser = currentUserService.getCurrentUser();
        
        // Si l'utilisateur est un candidat, forcer le filtre sur son propre ID
        final UUID effectiveCandidatId;
        if (currentUser.getRole() == Role.CANDIDAT) {
            effectiveCandidatId = currentUser.getId();
        } else {
            effectiveCandidatId = candidatId;
        }

        // Récupérer les candidatures basées sur les filtres
        List<Candidature> candidatures;

        if (statut != null && programmeId != null && effectiveCandidatId != null) {
            // Tous les filtres
            candidatures = candidatureRepository.findByStatut(statut).stream()
                .filter(c -> c.getProgramme().getId().equals(programmeId))
                .filter(c -> c.getCandidat().getId().equals(effectiveCandidatId))
                .collect(Collectors.toList());
        } else if (statut != null && programmeId != null) {
            // Statut et programme
            candidatures = candidatureRepository.findByStatut(statut).stream()
                .filter(c -> c.getProgramme().getId().equals(programmeId))
                .filter(c -> effectiveCandidatId == null || c.getCandidat().getId().equals(effectiveCandidatId))
                .collect(Collectors.toList());
        } else if (statut != null && effectiveCandidatId != null) {
            // Statut et candidat
            candidatures = candidatureRepository.findByStatut(statut).stream()
                .filter(c -> c.getCandidat().getId().equals(effectiveCandidatId))
                .collect(Collectors.toList());
        } else if (programmeId != null && effectiveCandidatId != null) {
            // Programme et candidat
            candidatures = candidatureRepository.findByProgrammeId(programmeId).stream()
                .filter(c -> c.getCandidat().getId().equals(effectiveCandidatId))
                .collect(Collectors.toList());
        } else if (statut != null) {
            // Seulement statut
            candidatures = candidatureRepository.findByStatut(statut);
            if (effectiveCandidatId != null) {
                candidatures = candidatures.stream()
                    .filter(c -> c.getCandidat().getId().equals(effectiveCandidatId))
                    .collect(Collectors.toList());
            }
        } else if (programmeId != null) {
            // Seulement programme
            candidatures = candidatureRepository.findByProgrammeId(programmeId);
            if (effectiveCandidatId != null) {
                candidatures = candidatures.stream()
                    .filter(c -> c.getCandidat().getId().equals(effectiveCandidatId))
                    .collect(Collectors.toList());
            }
        } else if (effectiveCandidatId != null) {
            // Seulement candidat
            candidatures = candidatureRepository.findByCandidatId(effectiveCandidatId);
        } else {
            // Pas de filtre
            candidatures = candidatureRepository.findAll();
        }

        // Mapper vers DTO
        List<CandidatureListResponse> responses = candidatures.stream()
            .map(c -> CandidatureListResponse.builder()
                .id(c.getId())
                .statut(c.getStatut())
                .submittedAt(c.getSubmittedAt())
                .programmeId(c.getProgramme().getId())
                .programmeTitre(c.getProgramme().getTitre())
                .candidatId(c.getCandidat().getId())
                .build())
            .collect(Collectors.toList());

        return responses;
    }
}
