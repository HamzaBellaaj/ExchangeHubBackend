package com.exchangeHub.Backend.dto.cv;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnalyseCvDto {
    private Double academicScore;
    private Double languageScore;
    private Double mobilityReadinessScore;
    private Double globalScore;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> keywords;
    private String recommendation;
}