package com.exchangeHub.Backend.dto.cv;

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
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String description;
}
