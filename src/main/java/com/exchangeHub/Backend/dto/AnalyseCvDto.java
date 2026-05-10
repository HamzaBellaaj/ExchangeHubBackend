package com.exchangeHub.Backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnalyseCvDto {

    private Integer scoreGlobal;
    private Integer scoreExperience;
    private Integer scoreFormation;
    private Integer scoreLangues;
    private Integer scoreCompetences;
    private String pointsForts;
    private String pointsFaibles;
    private String recommandation;
}
