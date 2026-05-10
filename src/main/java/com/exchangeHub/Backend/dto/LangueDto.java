package com.exchangeHub.Backend.dto;

import com.exchangeHub.Backend.enums.NiveauLangue;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LangueDto {

    private String langue;
    private NiveauLangue niveau;
}
