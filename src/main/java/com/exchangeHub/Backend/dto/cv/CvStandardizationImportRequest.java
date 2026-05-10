package com.exchangeHub.Backend.dto.cv;

import java.time.LocalDate;
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
public class CvStandardizationImportRequest {
    private UUID candidatureId;
    private UUID documentId;
    private CvProfileDto cvProfile;
    private List<FormationDto> formations;
    private List<ExperienceDto> experiences;
    private List<CompetenceDto> competences;
    private List<LangueDto> langues;
    private AnalyseCvDto analyse;
}
