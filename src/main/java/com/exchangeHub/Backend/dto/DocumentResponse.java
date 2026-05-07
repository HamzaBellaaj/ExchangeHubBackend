package com.exchangeHub.Backend.dto;

import java.time.LocalDateTime;
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
public class DocumentResponse {

    private UUID documentId;
    private UUID candidatureId;
    private TypeDocument typeDocument;
    private String fileName;
    private String storagePath;
    private String fileUrl;
    private String mimeType;
    private Long size;
    private LocalDateTime uploadedAt;
}
