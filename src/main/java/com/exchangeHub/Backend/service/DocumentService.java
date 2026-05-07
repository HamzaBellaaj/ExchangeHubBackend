package com.exchangeHub.Backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.exchangeHub.Backend.dto.DocumentResponse;
import com.exchangeHub.Backend.dto.UploadDocumentResponse;
import com.exchangeHub.Backend.dto.SignedUrlResponse;
import com.exchangeHub.Backend.entity.Candidature;
import com.exchangeHub.Backend.entity.Document;
import com.exchangeHub.Backend.entity.User;
import com.exchangeHub.Backend.enums.Role;
import com.exchangeHub.Backend.enums.TypeDocument;
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

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.storage.bucket}")
    private String bucket;

    public UploadDocumentResponse uploadDocument(
        UUID candidatureId,
        TypeDocument typeDocument,
        MultipartFile file) {

        logger.info("========== START DOCUMENT UPLOAD SERVICE ==========");
        logger.info("Start upload document - candidatureId: {}, typeDocument: {}",
            candidatureId, typeDocument);

        // 1. Vérifier candidatureId non null
        if (candidatureId == null) {
            throw new RuntimeException("candidatureId est null");
        }

        // 2. Vérifier typeDocument non null
        if (typeDocument == null) {
            throw new RuntimeException("typeDocument est null");
        }

        // 3. Vérifier file non null et non vide
        if (file == null) {
            throw new RuntimeException("Le fichier est null");
        }
        if (file.isEmpty()) {
            throw new RuntimeException("Le fichier est vide");
        }

        // 4. Récupérer la candidature
        logger.info("Fetching candidature...");
        Optional<Candidature> candidatureOptional = candidatureRepository.findById(candidatureId);
        if (!candidatureOptional.isPresent()) {
            throw new RuntimeException("Candidature introuvable");
        }
        Candidature candidature = candidatureOptional.get();
        logger.info("Candidature found");

        // 5. Vérifier l'accès (contrôle de sécurité métier)
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser.getRole() == Role.CANDIDAT) {
            // Un candidat ne peut uploader que sur sa propre candidature
            if (!candidature.getCandidat().getId().equals(currentUser.getId())) {
                throw new RuntimeException("Un candidat ne peut uploader que sur sa propre candidature");
            }
        } else if (currentUser.getRole() == Role.RESPONSABLE) {
            // Un responsable ne peut pas uploader de documents
            throw new RuntimeException("Un responsable ne peut pas uploader de documents");
        }
        // COORDINATEUR et ADMIN peuvent uploader

        // 6. Nettoyer le nom original du fichier
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            originalFilename = "document";
        }
        // Remplacer les espaces par des underscores
        originalFilename = originalFilename.replaceAll("\\s+", "_");
        // Éviter les caractères problématiques
        originalFilename = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "");
        logger.info("Cleaned filename: {}", originalFilename);

        // 7. Générer un chemin unique
        String storagePath = "candidatures/" + candidatureId + "/" + UUID.randomUUID() + "_" + originalFilename;
        logger.info("Generated storage path: {}", storagePath);

        // 8. Appeler uploadFile à Supabase AVANT de sauvegarder en DB
        logger.info("Uploading to Supabase Storage...");
        supabaseStorageService.uploadFile(file, storagePath);
        logger.info("Supabase upload done");

        // 8. Construire fileUrl
        String fileUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + storagePath;
        logger.info("File URL: {}", fileUrl);

        // 9. Créer un Document
        Document document = new Document();
        document.setId(UUID.randomUUID());
        document.setCandidature(candidature);
        document.setTypeDocument(typeDocument);
        document.setFileName(originalFilename);
        document.setFileUrl(fileUrl);
        document.setStoragePath(storagePath);
        document.setMimeType(file.getContentType());
        document.setSize(file.getSize());
        document.setUploadedAt(LocalDateTime.now());

        // 10. Sauvegarder avec documentRepository.save(document)
        logger.info("Saving document to database...");
        Document savedDocument = documentRepository.save(document);
        logger.info("Document saved - id: {}", savedDocument.getId());

        // 11. Retourner UploadDocumentResponse
        UploadDocumentResponse response = UploadDocumentResponse.builder()
            .documentId(savedDocument.getId())
            .candidatureId(savedDocument.getCandidature().getId())
            .typeDocument(savedDocument.getTypeDocument())
            .fileName(savedDocument.getFileName())
            .storagePath(savedDocument.getStoragePath())
            .fileUrl(savedDocument.getFileUrl())
            .mimeType(savedDocument.getMimeType())
            .size(savedDocument.getSize())
            .uploadedAt(savedDocument.getUploadedAt())
            .build();

        logger.info("========== DOCUMENT UPLOAD SERVICE COMPLETED ==========");
        return response;
    }

    public SignedUrlResponse generateSignedUrl(UUID documentId) {
        logger.info("========== GENERATING SIGNED URL ==========");
        logger.info("DocumentId: {}", documentId);

        // Chercher le Document
        Optional<Document> documentOptional = documentRepository.findById(documentId);
        if (!documentOptional.isPresent()) {
            throw new RuntimeException("Document introuvable");
        }
        Document document = documentOptional.get();
        logger.info("Document found - storagePath: {}", document.getStoragePath());

        // Vérifier l'accès : un candidat ne peut accéder qu'à ses propres documents
        User currentUser = currentUserService.getCurrentUser();
        Candidature candidature = document.getCandidature();
        if (currentUser.getRole() == Role.CANDIDAT) {
            if (!candidature.getCandidat().getId().equals(currentUser.getId())) {
                throw new RuntimeException("Accès refusé à ce document");
            }
        }

        // Générer l'URL signée avec expiration de 5 minutes
        int expiresInSeconds = 300;
        logger.info("Calling Supabase to generate signed URL...");
        String signedUrl = supabaseStorageService.createSignedUrl(document.getStoragePath(), expiresInSeconds);

        logger.info("========== SIGNED URL GENERATED SUCCESSFULLY ==========");

        // Retourner la réponse
        SignedUrlResponse response = SignedUrlResponse.builder()
            .documentId(document.getId())
            .signedUrl(signedUrl)
            .expiresIn(expiresInSeconds)
            .build();

        return response;
    }

    public List<DocumentResponse> getDocumentsByCandidature(UUID candidatureId) {
        logger.info("========== LIST DOCUMENTS BY CANDIDATURE ==========");
        logger.info("CandidatureId: {}", candidatureId);

        // Vérifier que candidatureId n'est pas null
        if (candidatureId == null) {
            throw new RuntimeException("candidatureId est null");
        }

        // Vérifier que la candidature existe
        Optional<Candidature> candidatureOptional = candidatureRepository.findById(candidatureId);
        if (!candidatureOptional.isPresent()) {
            logger.warn("Candidature not found - candidatureId: {}", candidatureId);
            throw new RuntimeException("Candidature introuvable");
        }
        Candidature candidature = candidatureOptional.get();

        // Vérifier l'accès : un candidat ne peut accéder qu'à ses propres documents
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser.getRole() == Role.CANDIDAT) {
            if (!candidature.getCandidat().getId().equals(currentUser.getId())) {
                throw new RuntimeException("Accès refusé aux documents de cette candidature");
            }
        }

        // Récupérer les documents
        logger.info("Fetching documents for candidature...");
        List<Document> documents = documentRepository.findByCandidatureId(candidatureId);
        logger.info("Found {} documents", documents.size());

        // Mapper vers DocumentResponse
        List<DocumentResponse> responses = documents.stream()
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

        logger.info("========== DOCUMENTS LISTED SUCCESSFULLY ==========");
        return responses;
    }
}
