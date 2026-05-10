package com.exchangeHub.Backend.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.exchangeHub.Backend.dto.DocumentValidationResponse;
import com.exchangeHub.Backend.entity.Candidature;
import com.exchangeHub.Backend.entity.Document;
import com.exchangeHub.Backend.entity.User;
import com.exchangeHub.Backend.enums.Role;
import com.exchangeHub.Backend.enums.TypeDocument;
import com.exchangeHub.Backend.enums.TypeMobilite;
import com.exchangeHub.Backend.exception.ForbiddenException;
import com.exchangeHub.Backend.exception.ResourceNotFoundException;
import com.exchangeHub.Backend.repository.CandidatureRepository;
import com.exchangeHub.Backend.repository.DocumentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentValidationService {

    private final CandidatureRepository candidatureRepository;
    private final DocumentRepository documentRepository;
    private final CurrentUserService currentUserService;

    public DocumentValidationResponse validateRequiredDocuments(UUID candidatureId) {
        Candidature candidature = candidatureRepository.findById(candidatureId)
            .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable"));

        checkCandidatureAccess(candidature);
        return validateRequiredDocuments(candidature);
    }

    public DocumentValidationResponse validateRequiredDocuments(Candidature candidature) {
        List<TypeDocument> requiredDocuments = requiredDocumentsFor(candidature);
        List<TypeDocument> uploadedDocuments = documentRepository.findByCandidature_Id(candidature.getId()).stream()
            .map(Document::getTypeDocument)
            .distinct()
            .collect(Collectors.toList());

        Set<TypeDocument> uploadedSet = new LinkedHashSet<>(uploadedDocuments);
        List<TypeDocument> missingDocuments = requiredDocuments.stream()
            .filter(required -> !uploadedSet.contains(required))
            .collect(Collectors.toList());

        return DocumentValidationResponse.builder()
            .candidatureId(candidature.getId())
            .valid(missingDocuments.isEmpty())
            .requiredDocuments(requiredDocuments)
            .uploadedDocuments(uploadedDocuments)
            .missingDocuments(missingDocuments)
            .build();
    }

    private List<TypeDocument> requiredDocumentsFor(Candidature candidature) {
        List<TypeDocument> required = new ArrayList<>();
        required.add(TypeDocument.CV);

        if (candidature.getProgramme() != null
            && candidature.getProgramme().getTypeMobilite() == TypeMobilite.ETUDES) {
            required.add(TypeDocument.RELEVE_NOTES);
        }

        return required;
    }

    private void checkCandidatureAccess(Candidature candidature) {
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser.getRole() == Role.CANDIDAT
            && !candidature.getCandidat().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Acces refuse a cette candidature");
        }
    }
}
