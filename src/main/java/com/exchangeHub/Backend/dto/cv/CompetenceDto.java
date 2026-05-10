package com.exchangeHub.Backend.dto.cv;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompetenceDto {
    private String nom;
    private String type;
    private String niveau;
}