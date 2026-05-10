package com.exchangeHub.Backend.dto;

import com.exchangeHub.Backend.enums.StatutEntretien;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEntretienStatusRequest {

    private StatutEntretien statut;
}
