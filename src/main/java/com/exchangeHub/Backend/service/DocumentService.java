package com.exchangeHub.Backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.exchangeHub.Backend.dto.DocumentResponse;
import com.exchangeHub.Backend.dto.SignedUrlResponse;
import com.exchangeHub.Backend.dto.UploadDocumentResponse;
import com.exchangeHub.Backend.entity.Candidature;
import com.exchangeHub.Backend.entity.Document;
import com.exchangeHub.Backend.entity.User;
import com.exchangeHub.Backend.enums.Role;
import com.exchangeHub.Backend.enums.TypeDocument;
import com.exchangeHub.Backend.exception.BadRequestException;
import com.exchangeHub.Backend.exception.ForbiddenException;
import com.exchangeHub.Backend.exception.ResourceNotFoundException;
import com.exchangeHub.Backend.repository.CandidatureRepository;
import com.exchangeHub.Backend.repository.DocumentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final CandidatureRepository candidatureRepository;
    private final SupabaseStorageService supabaseStorageService;
    private final CurrentUserService currentUserService;
    private final N8nCvStandardizationService n8nCvStandardizationService;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.storage.bucket}")
    private String bucket;

    public UploadDocumentResponse uploadDocument(
        UUID candidatureId,
        TypeDocument typeDocument,
        MultipartFile file) {

        if (candidatureId == null) {
            throw new BadRequestException("candidatureId obligatoire");
        }

        if (typeDocument == null) {
            throw new BadRequestException("typeDocument obligatoire");
        }

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Fichier obligatoire");
        }

        Candidature candidature = candidatureRepository.findById(candidatureId)
            .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable"));

        checkUploadAccess(candidature);

        String originalFilename = sanitizeFilename(file.getOriginalFilename());
        String storagePath = "candidatures/" + candidatureId + "/" + UUID.randomUUID() + "_" + originalFilename;

        logger.info("Uploading document to Supabase - candidatureId={}, typeDocument={}, storagePath={}",
            candidatureId, typeDocument, storagePath);
        supabaseStorageService.uploadFile(file, storagePath);

        Document document = new Document();
        document.setId(UUID.randomUUID());
        document.setCandidature(candidature);
        document.setTypeDocument(typeDocument);
        document.setFileName(originalFilename);
        document.setFileUrl(supabaseUrl + "/storage/v1/object/" + bucket + "/" + storagePath);
        document.setStoragePath(storagePath);
        document.setMimeType(file.getContentType());
        document.setSize(file.getSize());
        document.setUploadedAt(LocalDateTime.now());

        Document savedDocument = documentRepository.save(document);

        // Verifier type document
        if (savedDocument.getTypeDocument() == TypeDocument.CV) {
            try {
                // Lancer analyse CV automatique
                n8nCvStandardizationService.triggerStandardizationAuto(candidatureId, savedDocument);
            } catch (Exception e) {
                // Ne pas bloquer upload
                logger.warn("Erreur n8n auto: {}", e.getMessage());
            }
        }

        return mapToUploadResponse(savedDocument);
    }

    @Transactional(readOnly = true)
    public SignedUrlResponse generateSignedUrl(UUID documentId) {
        // Charger le document
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new ResourceNotFoundException("Document introuvable"));

        User currentUser = currentUserService.getCurrentUser();
        Role role = currentUser.getRole();

        // Verifier le role
        switch (role) {
            case CANDIDAT:
                // Candidat seulement ses documents
                if (!document.getCandidature().getCandidat().getId().equals(currentUser.getId())) {
                    throw new ForbiddenException("Acces refuse a ce document");
                }
                break;
            case COORDINATEUR:
            case RESPONSABLE:
            case ADMIN:
                // Staff autorise
                break;
            default:
                throw new ForbiddenException("Role non autorise");
        }

        int expiresInSeconds = 600;

        // Generer signed URL
        String signedUrl = supabaseStorageService.generateSignedUrl(document.getStoragePath(), expiresInSeconds);

        return SignedUrlResponse.builder()
            .documentId(document.getId())
            .signedUrl(signedUrl)
            .expiresIn(expiresInSeconds)
            .build();
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsByCandidature(UUID candidatureId) {
        if (candidatureId == null) {
            throw new BadRequestException("candidatureId obligatoire");
        }

        Candidature candidature = candidatureRepository.findById(candidatureId)
            .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable"));

        checkReadAccess(candidature, "Acces refuse aux documents de cette candidature");

        return documentRepository.findByCandidature_Id(candidatureId).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    private void checkUploadAccess(Candidature candidature) {
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser.getRole() == Role.CANDIDAT
            && !candidature.getCandidat().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Un candidat ne peut uploader que sur sa propre candidature");
        }

        if (currentUser.getRole() == Role.RESPONSABLE) {
            throw new ForbiddenException("Un responsable ne peut pas uploader de documents");
        }
    }

    private void checkReadAccess(Candidature candidature, String message) {
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser.getRole() == Role.CANDIDAT
            && !candidature.getCandidat().getId().equals(currentUser.getId())) {
            throw new ForbiddenException(message);
        }
    }

    private String sanitizeFilename(String originalFilename) {
        String filename = originalFilename;
        if (filename == null || filename.isBlank()) {
            filename = "document";
        }

        filename = filename.replaceAll("\\s+", "_");
        filename = filename.replaceAll("[^a-zA-Z0-9._-]", "");

        return filename.isBlank() ? "document" : filename;
    }

    private UploadDocumentResponse mapToUploadResponse(Document document) {
        return UploadDocumentResponse.builder()
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

    private DocumentResponse mapToResponse(Document document) {
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
