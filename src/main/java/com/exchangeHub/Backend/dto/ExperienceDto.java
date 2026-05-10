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
public class ExperienceDto {

    private String poste;
    private String organisation;
    private String pays;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String description;
    private Boolean current;
}
