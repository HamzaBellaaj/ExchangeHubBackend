package com.exchangeHub.Backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.exchangeHub.Backend.dto.CreateProgrammeRequest;
import com.exchangeHub.Backend.dto.ProgrammeResponse;
import com.exchangeHub.Backend.dto.UpdateProgrammeRequest;
import com.exchangeHub.Backend.entity.Programme;
import com.exchangeHub.Backend.entity.User;
import com.exchangeHub.Backend.enums.Role;
import com.exchangeHub.Backend.enums.StatutProgramme;
import com.exchangeHub.Backend.enums.TypeMobilite;
import com.exchangeHub.Backend.exception.BadRequestException;
import com.exchangeHub.Backend.exception.ForbiddenException;
import com.exchangeHub.Backend.exception.ResourceNotFoundException;
import com.exchangeHub.Backend.repository.ProgrammeRepository;
import com.exchangeHub.Backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ProgrammeService {

    private final ProgrammeRepository programmeRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<ProgrammeResponse> getProgrammes(
        StatutProgramme statut,
        TypeMobilite typeMobilite,
        String pays) {

        String paysFiltre = normalize(pays);
        List<Programme> programmes = programmeRepository.findAll();

        return programmes.stream()
            .filter(programme -> statut == null || programme.getStatut() == statut)
            .filter(programme -> typeMobilite == null || programme.getTypeMobilite() == typeMobilite)
            .filter(programme -> paysFiltre == null || equalsIgnoreCase(programme.getPays(), paysFiltre))
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProgrammeResponse getProgramme(UUID id) {
        Programme programme = programmeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Programme introuvable"));

        return mapToResponse(programme);
    }

    public ProgrammeResponse createProgramme(CreateProgrammeRequest request) {
        checkWriteAccess();
        validateCreateRequest(request);

        User coordinateur = request.getCoordinateurId() == null
            ? null
            : getUserWithRole(request.getCoordinateurId(), Role.COORDINATEUR, "Coordinateur introuvable",
                "L'utilisateur n'est pas un coordinateur");

        User responsable = request.getResponsableId() == null
            ? null
            : getUserWithRole(request.getResponsableId(), Role.RESPONSABLE, "Responsable introuvable",
                "L'utilisateur n'est pas un responsable");

        LocalDateTime now = LocalDateTime.now();
        Programme programme = new Programme();
        programme.setId(UUID.randomUUID());
        programme.setTitre(request.getTitre().trim());
        programme.setDescription(request.getDescription());
        programme.setTypeMobilite(request.getTypeMobilite());
        programme.setPays(request.getPays().trim());
        programme.setUniversitePartenaire(request.getUniversitePartenaire());
        programme.setDateDebut(request.getDateDebut());
        programme.setDateFin(request.getDateFin());
        programme.setDateLimiteCandidature(request.getDateLimiteCandidature());
        programme.setStatut(request.getStatut() != null ? request.getStatut() : StatutProgramme.BROUILLON);
        programme.setCoordinateur(coordinateur);
        programme.setResponsable(responsable);
        programme.setCreatedAt(now);
        programme.setUpdatedAt(now);

        return mapToResponse(programmeRepository.save(programme));
    }

    public ProgrammeResponse updateProgramme(UUID id, UpdateProgrammeRequest request) {
        checkWriteAccess();

        if (request == null) {
            throw new BadRequestException("Requete obligatoire");
        }

        Programme programme = programmeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Programme introuvable"));

        if (request.getTitre() != null) {
            if (request.getTitre().trim().isEmpty()) {
                throw new BadRequestException("Titre obligatoire");
            }
            programme.setTitre(request.getTitre().trim());
        }

        if (request.getDescription() != null) {
            programme.setDescription(request.getDescription());
        }

        if (request.getTypeMobilite() != null) {
            programme.setTypeMobilite(request.getTypeMobilite());
        }

        if (request.getPays() != null) {
            if (request.getPays().trim().isEmpty()) {
                throw new BadRequestException("Pays obligatoire");
            }
            programme.setPays(request.getPays().trim());
        }

        if (request.getUniversitePartenaire() != null) {
            programme.setUniversitePartenaire(request.getUniversitePartenaire());
        }

        if (request.getDateDebut() != null) {
            programme.setDateDebut(request.getDateDebut());
        }

        if (request.getDateFin() != null) {
            programme.setDateFin(request.getDateFin());
        }

        if (request.getDateLimiteCandidature() != null) {
            programme.setDateLimiteCandidature(request.getDateLimiteCandidature());
        }

        validateDates(programme.getDateDebut(), programme.getDateFin(), programme.getDateLimiteCandidature());

        if (request.getStatut() != null) {
            programme.setStatut(request.getStatut());
        }

        if (request.getCoordinateurId() != null) {
            programme.setCoordinateur(getUserWithRole(
                request.getCoordinateurId(),
                Role.COORDINATEUR,
                "Coordinateur introuvable",
                "L'utilisateur n'est pas un coordinateur"));
        }

        if (request.getResponsableId() != null) {
            programme.setResponsable(getUserWithRole(
                request.getResponsableId(),
                Role.RESPONSABLE,
                "Responsable introuvable",
                "L'utilisateur n'est pas un responsable"));
        }

        programme.setUpdatedAt(LocalDateTime.now());
        return mapToResponse(programmeRepository.save(programme));
    }

    private void checkWriteAccess() {
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser.getRole() != Role.ADMIN && currentUser.getRole() != Role.RESPONSABLE) {
            throw new ForbiddenException("Acces refuse");
        }
    }

    private void validateCreateRequest(CreateProgrammeRequest request) {
        if (request == null) {
            throw new BadRequestException("Requete obligatoire");
        }

        if (request.getTitre() == null || request.getTitre().trim().isEmpty()) {
            throw new BadRequestException("Titre obligatoire");
        }

        if (request.getTypeMobilite() == null) {
            throw new BadRequestException("Type mobilite obligatoire");
        }

        if (request.getPays() == null || request.getPays().trim().isEmpty()) {
            throw new BadRequestException("Pays obligatoire");
        }

        validateDates(request.getDateDebut(), request.getDateFin(), request.getDateLimiteCandidature());
    }

    private void validateDates(LocalDate dateDebut, LocalDate dateFin, LocalDate dateLimiteCandidature) {
        if (dateDebut != null && dateFin != null && dateFin.isBefore(dateDebut)) {
            throw new BadRequestException("Dates programme incoherentes");
        }

        if (dateDebut != null
            && dateLimiteCandidature != null
            && !dateLimiteCandidature.isBefore(dateDebut)) {
            throw new BadRequestException("La date limite de candidature doit etre avant la date de debut");
        }
    }

    private User getUserWithRole(UUID userId, Role expectedRole, String notFoundMessage, String roleMessage) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException(notFoundMessage));

        if (user.getRole() != expectedRole) {
            throw new BadRequestException(roleMessage);
        }

        return user;
    }

    private ProgrammeResponse mapToResponse(Programme programme) {
        User coordinateur = programme.getCoordinateur();
        User responsable = programme.getResponsable();

        return ProgrammeResponse.builder()
            .id(programme.getId())
            .titre(programme.getTitre())
            .description(programme.getDescription())
            .typeMobilite(programme.getTypeMobilite())
            .pays(programme.getPays())
            .universitePartenaire(programme.getUniversitePartenaire())
            .dateDebut(programme.getDateDebut())
            .dateFin(programme.getDateFin())
            .dateLimiteCandidature(programme.getDateLimiteCandidature())
            .statut(programme.getStatut())
            .coordinateurId(coordinateur != null ? coordinateur.getId() : null)
            .coordinateurNom(coordinateur != null ? coordinateur.getNom() : null)
            .responsableId(responsable != null ? responsable.getId() : null)
            .responsableNom(responsable != null ? responsable.getNom() : null)
            .createdAt(programme.getCreatedAt())
            .updatedAt(programme.getUpdatedAt())
            .build();
    }

    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }

    private boolean equalsIgnoreCase(String first, String second) {
        return first != null && second != null && first.equalsIgnoreCase(second);
    }
}
