package com.exchangeHub.Backend.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.exchangeHub.Backend.enums.StatutProgramme;
import com.exchangeHub.Backend.enums.TypeMobilite;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProgrammeRequest {

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
    private UUID responsableId;
}
