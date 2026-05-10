package com.exchangeHub.Backend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.exchangeHub.Backend.enums.TypeDecision;

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
public class DecisionResponse {

    private UUID id;
    private UUID candidatureId;
    private TypeDecision decision;
    private String commentaire;
    private UUID responsableId;
    private String responsableNom;
    private LocalDateTime decidedAt;
    private LocalDateTime createdAt;
}
