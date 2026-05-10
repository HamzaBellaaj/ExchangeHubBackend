package com.exchangeHub.Backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.exchangeHub.Backend.dto.cv.CvStandardizationImportRequest;
import com.exchangeHub.Backend.service.CvStandardizationImportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/cv-standardization")
@RequiredArgsConstructor
public class CvStandardizationImportController {

    private final CvStandardizationImportService importService;

    @Value("${n8n.callback-secret}")
    private String callbackSecret;

    @PostMapping("/import")
    public ResponseEntity<String> importStandardizedCv(
        @RequestBody CvStandardizationImportRequest request,
        @RequestHeader(value = "X-N8N-SECRET", required = false) String secret) {

        // Verifier secret n8n
        if (secret != null && !secret.equals(callbackSecret)) {
            return ResponseEntity.status(403).body("Secret n8n invalide");
        }

        importService.importer(request);
        return ResponseEntity.ok("CV standardise importe avec succes");
    }
}
