package com.exchangeHub.Backend.dto;

import java.util.List;
import java.util.UUID;

import com.exchangeHub.Backend.enums.TypeDocument;

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
public class DocumentValidationResponse {

    private UUID candidatureId;
    private boolean valid;
    private List<TypeDocument> requiredDocuments;
    private List<TypeDocument> uploadedDocuments;
    private List<TypeDocument> missingDocuments;
}
