package com.exchangeHub.Backend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.exchangeHub.Backend.enums.ModeEntretien;
import com.exchangeHub.Backend.enums.StatutEntretien;

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
public class EntretienResponse {

    private UUID id;
    private UUID candidatureId;
    private LocalDateTime dateHeure;
    private ModeEntretien mode;
    private String lienVisio;
    private String lieu;
    private StatutEntretien statut;
    private UUID planifieParId;
    private String planifieParNom;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
