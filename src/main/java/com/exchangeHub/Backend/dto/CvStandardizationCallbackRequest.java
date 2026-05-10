package com.exchangeHub.Backend.dto;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CvStandardizationCallbackRequest {

    private UUID candidatureId;
    private UUID documentId;
    private String fullName;
    private String email;
    private String phone;
    private String summary;
    private List<ExperienceDto> experiences;
    private List<FormationDto> formations;
    private List<CompetenceDto> competences;
    private List<LangueDto> langues;
    private AnalyseCvDto analyse;
}
