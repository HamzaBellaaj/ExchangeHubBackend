package com.exchangeHub.Backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.exchangeHub.Backend.dto.CandidatureDetailResponse;
import com.exchangeHub.Backend.dto.CandidatureListResponse;
import com.exchangeHub.Backend.dto.CandidatureStatusResponse;
import com.exchangeHub.Backend.dto.CreateCandidatureRequest;
import com.exchangeHub.Backend.dto.DocumentResponse;
import com.exchangeHub.Backend.dto.UpdateCandidatureStatusRequest;
import com.exchangeHub.Backend.entity.Candidature;
import com.exchangeHub.Backend.entity.User;
import com.exchangeHub.Backend.enums.Role;
import com.exchangeHub.Backend.enums.StatutCandidature;
import com.exchangeHub.Backend.service.CandidatureService;
import com.exchangeHub.Backend.service.CurrentUserService;
import com.exchangeHub.Backend.service.DocumentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/candidatures")
@RequiredArgsConstructor
public class CandidatureController {

    private final CandidatureService candidatureService;
    private final DocumentService documentService;
    private final CurrentUserService currentUserService;

    @PostMapping
    public ResponseEntity<Candidature> createCandidature(
        @RequestBody CreateCandidatureRequest request) {
        // Récupérer l'utilisateur courant
        User currentUser = currentUserService.getCurrentUser();
        
        // Créer la candidature avec l'utilisateur courant
        Candidature result = candidatureService.createCandidature(request, currentUser);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<List<CandidatureListResponse>> getCandidatures(
        @RequestParam(required = false) StatutCandidature statut,
        @RequestParam(required = false) UUID programmeId,
        @RequestParam(required = false) UUID candidatId) {
        
        // Récupérer l'utilisateur courant
        User currentUser = currentUserService.getCurrentUser();
        
        // Si l'utilisateur est un candidat, filtrer par son ID
        UUID finalCandidatId = candidatId;
        if (currentUser.getRole() == Role.CANDIDAT) {
            finalCandidatId = currentUser.getId();
        }
        
        List<CandidatureListResponse> responses = candidatureService.getCandidatures(statut, programmeId, finalCandidatId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CandidatureDetailResponse> getCandidature(@PathVariable UUID id) {
        CandidatureDetailResponse response = candidatureService.getCandidature(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{candidatureId}/documents")
    public ResponseEntity<List<DocumentResponse>> getDocumentsByCandidature(@PathVariable UUID candidatureId) {
        List<DocumentResponse> documents = documentService.getDocumentsByCandidature(candidatureId);
        return ResponseEntity.ok(documents);
    }

    @PatchMapping("/{id}/statut")
    public ResponseEntity<CandidatureStatusResponse> updateStatus(
        @PathVariable UUID id,
        @RequestBody UpdateCandidatureStatusRequest request) {
        CandidatureStatusResponse response = candidatureService.updateStatus(id, request);
        return ResponseEntity.ok(response);
    }
}
