package com.exchangeHub.Backend.dto;

import com.exchangeHub.Backend.enums.StatutCandidature;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCandidatureStatusRequest {

    private StatutCandidature statut;
}
