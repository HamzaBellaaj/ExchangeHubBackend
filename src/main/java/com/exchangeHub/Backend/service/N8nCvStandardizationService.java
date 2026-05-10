package com.exchangeHub.Backend.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.exchangeHub.Backend.dto.AnalyseCvDto;
import com.exchangeHub.Backend.dto.CompetenceDto;
import com.exchangeHub.Backend.dto.CvStandardizationCallbackRequest;
import com.exchangeHub.Backend.dto.CvStandardizationCallbackResponse;
import com.exchangeHub.Backend.dto.CvStandardizationTriggerResponse;
import com.exchangeHub.Backend.dto.DocumentValidationResponse;
import com.exchangeHub.Backend.dto.ExperienceDto;
import com.exchangeHub.Backend.dto.FormationDto;
import com.exchangeHub.Backend.dto.LangueDto;
import com.exchangeHub.Backend.entity.AnalyseCv;
import com.exchangeHub.Backend.entity.Candidature;
import com.exchangeHub.Backend.entity.Competence;
import com.exchangeHub.Backend.entity.CvProfile;
import com.exchangeHub.Backend.entity.Document;
import com.exchangeHub.Backend.entity.Experience;
import com.exchangeHub.Backend.entity.Formation;
import com.exchangeHub.Backend.entity.Langue;
import com.exchangeHub.Backend.entity.User;
import com.exchangeHub.Backend.enums.Role;
import com.exchangeHub.Backend.enums.StatutCandidature;
import com.exchangeHub.Backend.enums.TypeDocument;
import com.exchangeHub.Backend.exception.BadRequestException;
import com.exchangeHub.Backend.exception.ForbiddenException;
import com.exchangeHub.Backend.exception.ResourceNotFoundException;
import com.exchangeHub.Backend.repository.AnalyseCvRepository;
import com.exchangeHub.Backend.repository.CandidatureRepository;
import com.exchangeHub.Backend.repository.CompetenceRepository;
import com.exchangeHub.Backend.repository.CvProfileRepository;
import com.exchangeHub.Backend.repository.DocumentRepository;
import com.exchangeHub.Backend.repository.ExperienceRepository;
import com.exchangeHub.Backend.repository.FormationRepository;
import com.exchangeHub.Backend.repository.LangueRepository;

@Service
@Transactional
public class N8nCvStandardizationService {

    private static final Logger logger = LoggerFactory.getLogger(N8nCvStandardizationService.class);
    private static final int SIGNED_URL_EXPIRES_IN_SECONDS = 600;

    private final CandidatureRepository candidatureRepository;
    private final DocumentRepository documentRepository;
    private final SupabaseStorageService supabaseStorageService;
    private final CvProfileRepository cvProfileRepository;
    private final ExperienceRepository experienceRepository;
    private final FormationRepository formationRepository;
    private final CompetenceRepository competenceRepository;
    private final LangueRepository langueRepository;
    private final AnalyseCvRepository analyseCvRepository;
    private final CurrentUserService currentUserService;
    private final CandidatureWorkflowService candidatureWorkflowService;
    private final DocumentValidationService documentValidationService;
    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String webhookPath;
    private final String callbackSecret;
    private final String serverPort;

    public N8nCvStandardizationService(
        CandidatureRepository candidatureRepository,
        DocumentRepository documentRepository,
        SupabaseStorageService supabaseStorageService,
        CvProfileRepository cvProfileRepository,
        ExperienceRepository experienceRepository,
        FormationRepository formationRepository,
        CompetenceRepository competenceRepository,
        LangueRepository langueRepository,
        AnalyseCvRepository analyseCvRepository,
        CurrentUserService currentUserService,
        CandidatureWorkflowService candidatureWorkflowService,
        DocumentValidationService documentValidationService,
        RestTemplateBuilder restTemplateBuilder,
        @Value("${n8n.base-url}") String baseUrl,
        @Value("${n8n.cv-webhook-path}") String webhookPath,
        @Value("${n8n.timeout.connect:10000}") int connectTimeout,
        @Value("${n8n.timeout.read:60000}") int readTimeout,
        @Value("${n8n.callback-secret:change-me}") String callbackSecret,
        @Value("${server.port:8081}") String serverPort) {

        this.candidatureRepository = candidatureRepository;
        this.documentRepository = documentRepository;
        this.supabaseStorageService = supabaseStorageService;
        this.cvProfileRepository = cvProfileRepository;
        this.experienceRepository = experienceRepository;
        this.formationRepository = formationRepository;
        this.competenceRepository = competenceRepository;
        this.langueRepository = langueRepository;
        this.analyseCvRepository = analyseCvRepository;
        this.currentUserService = currentUserService;
        this.candidatureWorkflowService = candidatureWorkflowService;
        this.documentValidationService = documentValidationService;
        this.baseUrl = baseUrl;
        this.webhookPath = webhookPath;
        this.callbackSecret = callbackSecret;
        this.serverPort = serverPort;
        this.restTemplate = restTemplateBuilder
            .requestFactory(() -> {
                SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
                requestFactory.setConnectTimeout(connectTimeout);
                requestFactory.setReadTimeout(readTimeout);
                return requestFactory;
            })
            .build();
    }

    public void triggerStandardizationAuto(UUID candidatureId, Document cvDocument) {
        String signedUrl = supabaseStorageService.createSignedUrl(
            cvDocument.getStoragePath(),
            SIGNED_URL_EXPIRES_IN_SECONDS);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("candidatureId", candidatureId.toString());
        payload.put("documentId", cvDocument.getId().toString());
        payload.put("fileName", cvDocument.getFileName());
        payload.put("mimeType", cvDocument.getMimeType());
        payload.put("storagePath", cvDocument.getStoragePath());
        payload.put("signedUrl", signedUrl);
        payload.put("callbackUrl", "http://localhost:" + serverPort + "/n8n/cv-standardization/callback");

        Candidature candidature = cvDocument.getCandidature();
        if (candidature.getStatut() == StatutCandidature.SOUMISE) {
            candidatureWorkflowService.transitionSystem(candidature, StatutCandidature.EN_ANALYSE);
            candidatureRepository.save(candidature);
        }

        String webhookUrl = buildWebhookUrl();
        try {
            logger.info("Auto-calling n8n CV standardization webhook - candidatureId={}, documentId={}",
                candidatureId, cvDocument.getId());
            ResponseEntity<String> response = restTemplate.postForEntity(webhookUrl, payload, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.error("n8n a retourne le statut {}", response.getStatusCode());
            }
        } catch (RestClientException exception) {
            logger.error("Erreur appel webhook n8n: {}", exception.getMessage());
        }
    }

    public CvStandardizationTriggerResponse triggerStandardization(UUID candidatureId) {
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser.getRole() != Role.COORDINATEUR
            && currentUser.getRole() != Role.RESPONSABLE
            && currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Role non autorise pour standardiser un CV");
        }

        Candidature candidature = candidatureRepository.findById(candidatureId)
            .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable"));

        DocumentValidationResponse validation = documentValidationService.validateRequiredDocuments(candidature);
        if (!validation.isValid()) {
            throw new BadRequestException("Documents obligatoires manquants: " + validation.getMissingDocuments());
        }

        Document cvDocument = documentRepository.findByCandidature_IdAndTypeDocument(candidatureId, TypeDocument.CV)
            .stream()
            .findFirst()
            .orElseThrow(() -> new BadRequestException("CV obligatoire pour lancer la standardisation"));

        String signedUrl = supabaseStorageService.createSignedUrl(
            cvDocument.getStoragePath(),
            SIGNED_URL_EXPIRES_IN_SECONDS);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("candidatureId", candidature.getId().toString());
        payload.put("documentId", cvDocument.getId().toString());
        payload.put("fileName", cvDocument.getFileName());
        payload.put("mimeType", cvDocument.getMimeType());
        payload.put("storagePath", cvDocument.getStoragePath());
        payload.put("signedUrl", signedUrl);
        payload.put("callbackUrl", "http://localhost:" + serverPort + "/n8n/cv-standardization/callback");

        if (candidature.getStatut() == StatutCandidature.SOUMISE) {
            candidatureWorkflowService.transitionSystem(candidature, StatutCandidature.EN_ANALYSE);
            candidatureRepository.save(candidature);
        }

        String webhookUrl = buildWebhookUrl();
        try {
            logger.info("Calling n8n CV standardization webhook - candidatureId={}, documentId={}",
                candidature.getId(), cvDocument.getId());
            ResponseEntity<String> response = restTemplate.postForEntity(webhookUrl, payload, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("n8n a retourne le statut " + response.getStatusCode());
            }
        } catch (RestClientException exception) {
            throw new IllegalStateException("Erreur appel webhook n8n: " + exception.getMessage(), exception);
        }

        return CvStandardizationTriggerResponse.builder()
            .candidatureId(candidature.getId())
            .documentId(cvDocument.getId())
            .status("SENT")
            .build();
    }

    public CvStandardizationCallbackResponse handleCallback(
        CvStandardizationCallbackRequest request,
        String secretHeader) {

        verifyCallbackSecret(secretHeader);
        validateCallbackRequest(request);

        Candidature candidature = candidatureRepository.findById(request.getCandidatureId())
            .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable"));

        Document document = documentRepository.findById(request.getDocumentId())
            .orElseThrow(() -> new ResourceNotFoundException("Document introuvable"));

        if (!document.getCandidature().getId().equals(candidature.getId())) {
            throw new BadRequestException("Le document ne correspond pas a la candidature");
        }

        if (document.getTypeDocument() != TypeDocument.CV) {
            throw new BadRequestException("Le document callback n'est pas un CV");
        }

        CvProfile cvProfile = upsertCvProfile(request, candidature);
        replaceExperiences(cvProfile, request.getExperiences());
        replaceFormations(cvProfile, request.getFormations());
        replaceCompetences(cvProfile, request.getCompetences());
        replaceLangues(cvProfile, request.getLangues());
        upsertAnalyse(cvProfile, request.getAnalyse());

        markCandidatureAnalysee(candidature);
        candidatureRepository.save(candidature);

        return CvStandardizationCallbackResponse.builder()
            .candidatureId(candidature.getId())
            .documentId(document.getId())
            .cvProfileId(cvProfile.getId())
            .statut(candidature.getStatut())
            .build();
    }

    private void verifyCallbackSecret(String secretHeader) {
        if (secretHeader == null || secretHeader.isBlank()) {
            logger.warn("n8n callback received without X-N8N-SECRET header");
            return;
        }

        if (!secretHeader.equals(callbackSecret)) {
            throw new ForbiddenException("Secret callback n8n invalide");
        }
    }

    private void validateCallbackRequest(CvStandardizationCallbackRequest request) {
        if (request == null) {
            throw new BadRequestException("Payload callback obligatoire");
        }

        if (request.getCandidatureId() == null) {
            throw new BadRequestException("candidatureId obligatoire");
        }

        if (request.getDocumentId() == null) {
            throw new BadRequestException("documentId obligatoire");
        }
    }

    private CvProfile upsertCvProfile(CvStandardizationCallbackRequest request, Candidature candidature) {
        CvProfile cvProfile = cvProfileRepository.findByCandidature_Id(candidature.getId())
            .orElseGet(() -> {
                CvProfile created = new CvProfile();
                created.setId(UUID.randomUUID());
                created.setCandidature(candidature);
                return created;
            });

        cvProfile.setNomComplet(request.getFullName());
        cvProfile.setEmail(request.getEmail());
        cvProfile.setTelephone(request.getPhone());
        cvProfile.setResume(request.getSummary());
        cvProfile.setParsedAt(LocalDateTime.now());

        return cvProfileRepository.save(cvProfile);
    }

    private void replaceExperiences(CvProfile cvProfile, List<ExperienceDto> experiences) {
        experienceRepository.deleteByCvProfile_Id(cvProfile.getId());

        if (experiences == null) {
            return;
        }

        experiences.stream()
            .map(dto -> Experience.builder()
                .id(UUID.randomUUID())
                .cvProfile(cvProfile)
                .poste(dto.getPoste())
                .organisation(dto.getOrganisation())
                .pays(dto.getPays())
                .dateDebut(dto.getDateDebut())
                .dateFin(dto.getDateFin())
                .description(dto.getDescription())
                .current(dto.getCurrent())
                .build())
            .forEach(experienceRepository::save);
    }

    private void replaceFormations(CvProfile cvProfile, List<FormationDto> formations) {
        formationRepository.deleteByCvProfile_Id(cvProfile.getId());

        if (formations == null) {
            return;
        }

        formations.stream()
            .map(dto -> Formation.builder()
                .id(UUID.randomUUID())
                .cvProfile(cvProfile)
                .diplome(dto.getDiplome())
                .etablissement(dto.getEtablissement())
                .pays(dto.getPays())
                .domaine(dto.getDomaine())
                .dateDebut(dto.getDateDebut())
                .dateFin(dto.getDateFin())
                .build())
            .forEach(formationRepository::save);
    }

    private void replaceCompetences(CvProfile cvProfile, List<CompetenceDto> competences) {
        competenceRepository.deleteByCvProfile_Id(cvProfile.getId());

        if (competences == null) {
            return;
        }

        competences.stream()
            .filter(dto -> dto.getNom() != null && !dto.getNom().isBlank())
            .map(dto -> Competence.builder()
                .id(UUID.randomUUID())
                .cvProfile(cvProfile)
                .nom(dto.getNom())
                .niveau(dto.getNiveau())
                .build())
            .forEach(competenceRepository::save);
    }

    private void replaceLangues(CvProfile cvProfile, List<LangueDto> langues) {
        langueRepository.deleteByCvProfile_Id(cvProfile.getId());

        if (langues == null) {
            return;
        }

        langues.stream()
            .filter(dto -> dto.getLangue() != null && !dto.getLangue().isBlank() && dto.getNiveau() != null)
            .map(dto -> Langue.builder()
                .id(UUID.randomUUID())
                .cvProfile(cvProfile)
                .langue(dto.getLangue())
                .niveau(dto.getNiveau())
                .build())
            .forEach(langueRepository::save);
    }

    private void upsertAnalyse(CvProfile cvProfile, AnalyseCvDto dto) {
        if (dto == null) {
            return;
        }

        validateScore(dto.getScoreGlobal(), "scoreGlobal");
        validateScore(dto.getScoreExperience(), "scoreExperience");
        validateScore(dto.getScoreFormation(), "scoreFormation");
        validateScore(dto.getScoreLangues(), "scoreLangues");
        validateScore(dto.getScoreCompetences(), "scoreCompetences");

        AnalyseCv analyseCv = analyseCvRepository.findByCvProfile_Id(cvProfile.getId())
            .orElseGet(() -> AnalyseCv.builder()
                .id(UUID.randomUUID())
                .cvProfile(cvProfile)
                .build());

        analyseCv.setScoreGlobal(dto.getScoreGlobal());
        analyseCv.setScoreExperience(dto.getScoreExperience());
        analyseCv.setScoreFormation(dto.getScoreFormation());
        analyseCv.setScoreLangues(dto.getScoreLangues());
        analyseCv.setScoreCompetences(dto.getScoreCompetences());
        analyseCv.setPointsForts(dto.getPointsForts());
        analyseCv.setPointsFaibles(dto.getPointsFaibles());
        analyseCv.setRecommandation(dto.getRecommandation());
        analyseCv.setAnalyzedAt(LocalDateTime.now());

        analyseCvRepository.save(analyseCv);
    }

    private void markCandidatureAnalysee(Candidature candidature) {
        if (candidature.getStatut() == StatutCandidature.SOUMISE) {
            candidatureWorkflowService.transitionSystem(candidature, StatutCandidature.EN_ANALYSE);
        }

        if (candidature.getStatut() == StatutCandidature.EN_ANALYSE) {
            candidatureWorkflowService.transitionSystem(candidature, StatutCandidature.ANALYSEE);
            return;
        }

        if (candidature.getStatut() == StatutCandidature.ANALYSEE) {
            return;
        }

        throw new BadRequestException("Statut candidature incompatible avec le callback CV: "
            + candidature.getStatut());
    }

    private void validateScore(Integer score, String fieldName) {
        if (score != null && (score < 0 || score > 100)) {
            throw new BadRequestException(fieldName + " doit etre entre 0 et 100");
        }
    }

    private String buildWebhookUrl() {
        String normalizedBaseUrl = baseUrl.endsWith("/")
            ? baseUrl.substring(0, baseUrl.length() - 1)
            : baseUrl;
        String normalizedPath = webhookPath.startsWith("/") ? webhookPath : "/" + webhookPath;

        if (normalizedPath.startsWith("/workflow/")) {
            throw new BadRequestException("Le webhook n8n ne doit pas utiliser /workflow");
        }

        return normalizedBaseUrl + normalizedPath;
    }
}
