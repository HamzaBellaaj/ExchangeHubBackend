package com.exchangeHub.Backend.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.exchangeHub.Backend.dto.CreateDecisionRequest;
import com.exchangeHub.Backend.dto.DecisionResponse;
import com.exchangeHub.Backend.service.DecisionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class DecisionController {

    private final DecisionService decisionService;

    @PostMapping("/decisions")
    public ResponseEntity<DecisionResponse> createDecision(@RequestBody CreateDecisionRequest request) {
        DecisionResponse response = decisionService.createDecision(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/candidatures/{candidatureId}/decision")
    public ResponseEntity<DecisionResponse> getDecisionByCandidature(@PathVariable UUID candidatureId) {
        DecisionResponse response = decisionService.getDecisionByCandidature(candidatureId);
        return ResponseEntity.ok(response);
    }
}
