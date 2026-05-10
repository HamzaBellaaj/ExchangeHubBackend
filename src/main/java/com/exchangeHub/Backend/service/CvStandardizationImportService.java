package com.exchangeHub.Backend.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.exchangeHub.Backend.dto.cv.AnalyseCvDto;
import com.exchangeHub.Backend.dto.cv.CompetenceDto;
import com.exchangeHub.Backend.dto.cv.CvStandardizationImportRequest;
import com.exchangeHub.Backend.dto.cv.ExperienceDto;
import com.exchangeHub.Backend.dto.cv.FormationDto;
import com.exchangeHub.Backend.dto.cv.LangueDto;
import com.exchangeHub.Backend.entity.AnalyseCv;
import com.exchangeHub.Backend.entity.Candidature;
import com.exchangeHub.Backend.entity.Competence;
import com.exchangeHub.Backend.entity.CvProfile;
import com.exchangeHub.Backend.entity.Document;
import com.exchangeHub.Backend.entity.Experience;
import com.exchangeHub.Backend.entity.Formation;
import com.exchangeHub.Backend.entity.Langue;
import com.exchangeHub.Backend.enums.NiveauCompetence;
import com.exchangeHub.Backend.enums.NiveauLangue;
import com.exchangeHub.Backend.enums.StatutCandidature;
import com.exchangeHub.Backend.exception.BadRequestException;
import com.exchangeHub.Backend.exception.ResourceNotFoundException;
import com.exchangeHub.Backend.repository.AnalyseCvRepository;
import com.exchangeHub.Backend.repository.CandidatureRepository;
import com.exchangeHub.Backend.repository.CompetenceRepository;
import com.exchangeHub.Backend.repository.CvProfileRepository;
import com.exchangeHub.Backend.repository.DocumentRepository;
import com.exchangeHub.Backend.repository.ExperienceRepository;
import com.exchangeHub.Backend.repository.FormationRepository;
import com.exchangeHub.Backend.repository.LangueRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CvStandardizationImportService {

    private final CandidatureRepository candidatureRepository;
    private final DocumentRepository documentRepository;
    private final CvProfileRepository cvProfileRepository;
    private final FormationRepository formationRepository;
    private final ExperienceRepository experienceRepository;
    private final CompetenceRepository competenceRepository;
    private final LangueRepository langueRepository;
    private final AnalyseCvRepository analyseCvRepository;

    @Transactional
    public void importer(CvStandardizationImportRequest request) {
        // Verifier requete n8n
        if (request == null) {
            throw new BadRequestException("Requete n8n obligatoire");
        }

        if (request.getCandidatureId() == null) {
            throw new BadRequestException("candidatureId obligatoire");
        }

        if (request.getDocumentId() == null) {
            throw new BadRequestException("documentId obligatoire");
        }

        // Charger candidature
        Candidature candidature = candidatureRepository.findById(request.getCandidatureId())
            .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable"));

        // Charger document
        Document document = documentRepository.findById(request.getDocumentId())
            .orElseThrow(() -> new ResourceNotFoundException("Document introuvable"));

        if (!document.getCandidature().getId().equals(candidature.getId())) {
            throw new BadRequestException("Document hors candidature");
        }

        CvProfile profile = findOrCreateProfile(candidature);

        // Sauvegarder profil CV
        saveProfile(profile, request);

        // Supprimer anciennes donnees
        deleteOldCvData(profile);

        // Inserer nouvelles donnees
        saveFormations(profile, request);
        saveExperiences(profile, request);
        saveCompetences(profile, request);
        saveLangues(profile, request);
        saveAnalyse(profile, request.getAnalyse());

        // Sauvegarder statut analyse
        candidature.setStatut(StatutCandidature.ANALYSEE);
        candidatureRepository.save(candidature);
    }

    private CvProfile findOrCreateProfile(Candidature candidature) {
        CvProfile profile = cvProfileRepository.findByCandidature_Id(candidature.getId())
            .orElse(new CvProfile());

        if (profile.getId() == null) {
            profile.setId(UUID.randomUUID());
            profile.setCandidature(candidature);
        }

        profile.setParsedAt(LocalDateTime.now());
        return profile;
    }

    private void saveProfile(CvProfile profile, CvStandardizationImportRequest request) {
        if (request.getCvProfile() != null) {
            profile.setNomComplet(request.getCvProfile().getFullName());
            profile.setEmail(request.getCvProfile().getEmail());
            profile.setTelephone(request.getCvProfile().getPhone());
            profile.setTitreProfil(request.getCvProfile().getProfileTitle());
            profile.setResume(request.getCvProfile().getSummary());
        }

        cvProfileRepository.save(profile);
    }

    private void deleteOldCvData(CvProfile profile) {
        formationRepository.deleteByCvProfile_Id(profile.getId());
        experienceRepository.deleteByCvProfile_Id(profile.getId());
        competenceRepository.deleteByCvProfile_Id(profile.getId());
        langueRepository.deleteByCvProfile_Id(profile.getId());

        analyseCvRepository.findByCvProfile_Id(profile.getId()).ifPresent(analyse -> {
            analyseCvRepository.delete(analyse);
            analyseCvRepository.flush();
        });
    }

    private void saveFormations(CvProfile profile, CvStandardizationImportRequest request) {
        if (request.getFormations() == null) {
            return;
        }

        for (FormationDto dto : request.getFormations()) {
            Formation formation = new Formation();
            formation.setId(UUID.randomUUID());
            formation.setCvProfile(profile);
            formation.setDiplome(dto.getDiplome());
            formation.setEtablissement(dto.getEtablissement());
            formation.setDateDebut(dto.getDateDebut());
            formation.setDateFin(dto.getDateFin());
            formationRepository.save(formation);
        }
    }

    private void saveExperiences(CvProfile profile, CvStandardizationImportRequest request) {
        if (request.getExperiences() == null) {
            return;
        }

        for (ExperienceDto dto : request.getExperiences()) {
            Experience experience = new Experience();
            experience.setId(UUID.randomUUID());
            experience.setCvProfile(profile);
            experience.setPoste(dto.getTitre());
            experience.setOrganisation(dto.getOrganisation());
            experience.setDateDebut(dto.getDateDebut());
            experience.setDateFin(dto.getDateFin());
            experience.setDescription(dto.getDescription());
            experience.setCurrent(dto.getDateFin() == null);
            experienceRepository.save(experience);
        }
    }

    private void saveCompetences(CvProfile profile, CvStandardizationImportRequest request) {
        if (request.getCompetences() == null) {
            return;
        }

        for (CompetenceDto dto : request.getCompetences()) {
            Competence competence = new Competence();
            competence.setId(UUID.randomUUID());
            competence.setCvProfile(profile);
            competence.setNom(dto.getNom());
            competence.setNiveau(parseNiveauCompetence(dto.getNiveau()));
            competenceRepository.save(competence);
        }
    }

    private void saveLangues(CvProfile profile, CvStandardizationImportRequest request) {
        if (request.getLangues() == null) {
            return;
        }

        for (LangueDto dto : request.getLangues()) {
            Langue langue = new Langue();
            langue.setId(UUID.randomUUID());
            langue.setCvProfile(profile);
            langue.setLangue(dto.getNom());
            langue.setNiveau(parseNiveauLangue(dto.getNiveau()));
            langueRepository.save(langue);
        }
    }

    private void saveAnalyse(CvProfile profile, AnalyseCvDto dto) {
        if (dto == null) {
            return;
        }

        AnalyseCv analyse = new AnalyseCv();
        analyse.setId(UUID.randomUUID());
        analyse.setCvProfile(profile);
        analyse.setScoreGlobal(toInteger(dto.getGlobalScore()));
        analyse.setScoreFormation(toInteger(dto.getAcademicScore()));
        analyse.setScoreLangues(toInteger(dto.getLanguageScore()));
        analyse.setScoreCompetences(toInteger(dto.getMobilityReadinessScore()));
        analyse.setPointsForts(joinList(dto.getStrengths()));
        analyse.setPointsFaibles(joinList(dto.getWeaknesses()));
        analyse.setRecommandation(dto.getRecommendation());
        analyse.setAnalyzedAt(LocalDateTime.now());

        analyseCvRepository.save(analyse);
    }

    private Integer toInteger(Double value) {
        if (value == null) {
            return null;
        }

        return value.intValue();
    }

    private String joinList(java.util.List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        return String.join(", ", values);
    }

    private NiveauCompetence parseNiveauCompetence(String niveau) {
        if (niveau == null || niveau.isBlank()) {
            return NiveauCompetence.INTERMEDIAIRE;
        }

        try {
            return NiveauCompetence.valueOf(niveau.toUpperCase());
        } catch (Exception e) {
            return NiveauCompetence.INTERMEDIAIRE;
        }
    }

    private NiveauLangue parseNiveauLangue(String niveau) {
        if (niveau == null || niveau.isBlank()) {
            return NiveauLangue.B1;
        }

        try {
            return NiveauLangue.valueOf(niveau.toUpperCase());
        } catch (Exception e) {
            return NiveauLangue.B1;
        }
    }
}
