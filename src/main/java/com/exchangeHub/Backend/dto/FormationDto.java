package com.exchangeHub.Backend.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FormationDto {

    private String diplome;
    private String etablissement;
    private String pays;
    private String domaine;
    private LocalDate dateDebut;
    private LocalDate dateFin;
}
