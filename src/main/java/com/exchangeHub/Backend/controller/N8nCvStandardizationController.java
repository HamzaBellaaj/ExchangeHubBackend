package com.exchangeHub.Backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.exchangeHub.Backend.dto.CvStandardizationCallbackRequest;
import com.exchangeHub.Backend.dto.CvStandardizationCallbackResponse;
import com.exchangeHub.Backend.service.N8nCvStandardizationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/n8n/cv-standardization")
@RequiredArgsConstructor
public class N8nCvStandardizationController {

    private final N8nCvStandardizationService n8nCvStandardizationService;

    @PostMapping("/callback")
    public ResponseEntity<CvStandardizationCallbackResponse> callback(
        @RequestBody CvStandardizationCallbackRequest request,
        @RequestHeader(value = "X-N8N-SECRET", required = false) String secret) {

        CvStandardizationCallbackResponse response = n8nCvStandardizationService.handleCallback(request, secret);
        return ResponseEntity.ok(response);
    }
}
