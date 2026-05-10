package com.exchangeHub.Backend.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.exchangeHub.Backend.dto.CreateEntretienRequest;
import com.exchangeHub.Backend.dto.EntretienResponse;
import com.exchangeHub.Backend.dto.UpdateEntretienStatusRequest;
import com.exchangeHub.Backend.service.EntretienService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class EntretienController {

    private final EntretienService entretienService;

    @PostMapping("/entretiens")
    public ResponseEntity<EntretienResponse> createEntretien(@RequestBody CreateEntretienRequest request) {
        EntretienResponse response = entretienService.createEntretien(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/entretiens/{id}")
    public ResponseEntity<EntretienResponse> getEntretien(@PathVariable UUID id) {
        EntretienResponse response = entretienService.getEntretien(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/candidatures/{candidatureId}/entretien")
    public ResponseEntity<EntretienResponse> getEntretienByCandidature(@PathVariable UUID candidatureId) {
        EntretienResponse response = entretienService.getEntretienByCandidature(candidatureId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/entretiens/{id}/statut")
    public ResponseEntity<EntretienResponse> updateStatus(
        @PathVariable UUID id,
        @RequestBody UpdateEntretienStatusRequest request) {
        EntretienResponse response = entretienService.updateStatus(id, request);
        return ResponseEntity.ok(response);
    }
}
