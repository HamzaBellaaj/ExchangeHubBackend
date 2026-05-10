package com.exchangeHub.Backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.exchangeHub.Backend.enums.StatutProgramme;
import com.exchangeHub.Backend.enums.TypeMobilite;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgrammeResponse {

    private UUID id;
    private String titre;
    private String description;
    private TypeMobilite typeMobilite;
    private String pays;
    private String universitePartenaire;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private LocalDate dateLimiteCandidature;
    private StatutProgramme statut;
    private UUID coordinateurId;
    private String coordinateurNom;
    private UUID responsableId;
    private String responsableNom;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
