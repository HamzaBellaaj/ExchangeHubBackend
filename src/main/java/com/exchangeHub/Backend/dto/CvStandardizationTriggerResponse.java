package com.exchangeHub.Backend.dto;

import java.util.UUID;

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
public class CvStandardizationTriggerResponse {

    private UUID candidatureId;
    private UUID documentId;
    private String status;
}
