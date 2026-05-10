package com.exchangeHub.Backend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.exchangeHub.Backend.entity.Candidature;
import com.exchangeHub.Backend.entity.Document;
import com.exchangeHub.Backend.enums.TypeDocument;
import com.exchangeHub.Backend.exception.BadRequestException;
import com.exchangeHub.Backend.exception.ResourceNotFoundException;
import com.exchangeHub.Backend.repository.CandidatureRepository;
import com.exchangeHub.Backend.repository.DocumentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class N8nCvService {

    private final CandidatureRepository candidatureRepository;
    private final DocumentRepository documentRepository;
    private final SupabaseStorageService supabaseStorageService;
    private final RestTemplate restTemplate;

    @Value("${n8n.base-url}")
    private String n8nBaseUrl;

    @Value("${n8n.cv-webhook-path}")
    private String webhookPath;

    @Value("${n8n.callback-url}")
    private String callbackUrl;

    public String envoyerCvVersN8n(UUID candidatureId) {
        if (candidatureId == null) {
            throw new BadRequestException("candidatureId obligatoire");
        }

        // Chercher candidature
        Candidature candidature = candidatureRepository.findById(candidatureId)
            .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable"));

        // Chercher document CV
        List<Document> documents = documentRepository.findByCandidature_IdAndTypeDocument(candidatureId, TypeDocument.CV);
        if (documents.isEmpty()) {
            throw new ResourceNotFoundException("Aucun document CV trouve");
        }

        Document documentCv = documents.get(0);

        // Generer signed URL
        String signedUrl = supabaseStorageService.generateSignedUrl(documentCv.getStoragePath(), 600);

        // Telecharger le CV
        byte[] fileBytes = restTemplate.getForObject(signedUrl, byte[].class);
        if (fileBytes == null || fileBytes.length == 0) {
            throw new BadRequestException("CV vide ou inaccessible");
        }

        // Preparer le fichier
        ByteArrayResource fileResource = new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return documentCv.getFileName();
            }
        };

        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(getMediaType(documentCv));
        HttpEntity<ByteArrayResource> filePart = new HttpEntity<>(fileResource, fileHeaders);

        // Construire multipart n8n
        MultiValueMap<String, Object> payload = new LinkedMultiValueMap<>();
        payload.add("candidatureId", candidature.getId().toString());
        payload.add("documentId", documentCv.getId().toString());
        payload.add("fileName", documentCv.getFileName());
        payload.add("mimeType", documentCv.getMimeType());
        payload.add("storagePath", documentCv.getStoragePath());
        payload.add("callbackUrl", callbackUrl);
        payload.add("file", filePart);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(payload, headers);

        // Envoyer vers n8n
        String url = n8nBaseUrl + webhookPath;
        restTemplate.postForObject(url, request, String.class);

        return "Standardisation CV envoyee vers n8n";
    }

    private MediaType getMediaType(Document document) {
        if (document.getMimeType() == null || document.getMimeType().isBlank()) {
            return MediaType.APPLICATION_PDF;
        }

        return MediaType.parseMediaType(document.getMimeType());
    }
}
