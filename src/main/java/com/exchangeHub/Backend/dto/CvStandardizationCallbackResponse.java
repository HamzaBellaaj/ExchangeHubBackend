package com.exchangeHub.Backend.dto;

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
public class CvStandardizationCallbackResponse {

    private UUID candidatureId;
    private UUID documentId;
    private UUID cvProfileId;
    private StatutCandidature statut;
}
