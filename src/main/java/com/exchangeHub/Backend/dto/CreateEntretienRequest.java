package com.exchangeHub.Backend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.exchangeHub.Backend.enums.ModeEntretien;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateEntretienRequest {

    private UUID candidatureId;
    private LocalDateTime dateHeure;
    private ModeEntretien mode;
    private String lienVisio;
    private String lieu;
}
