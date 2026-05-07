package com.exchangeHub.Backend.controller;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.exchangeHub.Backend.dto.SignedUrlResponse;
import com.exchangeHub.Backend.dto.UploadDocumentResponse;
import com.exchangeHub.Backend.enums.TypeDocument;
import com.exchangeHub.Backend.service.DocumentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);
    private final DocumentService documentService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadDocumentResponse> uploadDocument(
        @RequestParam("candidatureId") UUID candidatureId,
        @RequestParam("typeDocument") TypeDocument typeDocument,
        @RequestParam("file") MultipartFile file) {

        logger.info("Received upload request - candidatureId: {}, typeDocument: {}, fileName: {}",
            candidatureId, typeDocument, file.getOriginalFilename());

        UploadDocumentResponse response = documentService.uploadDocument(candidatureId, typeDocument, file);
        
        logger.info("Upload completed successfully - documentId: {}", response.getDocumentId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{documentId}/signed-url")
    public ResponseEntity<SignedUrlResponse> getSignedUrl(@PathVariable UUID documentId) {
        logger.info("Received signed URL request - documentId: {}", documentId);

        SignedUrlResponse response = documentService.generateSignedUrl(documentId);

        logger.info("Signed URL generated successfully - documentId: {}", documentId);
        return ResponseEntity.ok(response);
    }
}
