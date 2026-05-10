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
public class ExperienceDto {
    private String titre;
    private String organisation;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String description;
}
