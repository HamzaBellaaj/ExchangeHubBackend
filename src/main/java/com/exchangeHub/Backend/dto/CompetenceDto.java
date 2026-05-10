package com.exchangeHub.Backend.dto;

import com.exchangeHub.Backend.enums.NiveauCompetence;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompetenceDto {

    private String nom;
    private NiveauCompetence niveau;
}
