package com.exchangeHub.Backend.dto;

import java.util.UUID;

import com.exchangeHub.Backend.enums.TypeDecision;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateDecisionRequest {

    private UUID candidatureId;
    private TypeDecision decision;
    private String commentaire;
}
