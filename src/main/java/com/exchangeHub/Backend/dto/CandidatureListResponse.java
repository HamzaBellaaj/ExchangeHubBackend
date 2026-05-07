package com.exchangeHub.Backend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.exchangeHub.Backend.enums.StatutCandidature;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidatureListResponse {

    private UUID id;
    private StatutCandidature statut;
    private LocalDateTime submittedAt;
    private UUID programmeId;
    private String programmeTitre;
    private UUID candidatId;
}
