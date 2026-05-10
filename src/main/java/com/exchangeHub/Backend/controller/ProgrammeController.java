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

import com.exchangeHub.Backend.dto.CreateProgrammeRequest;
import com.exchangeHub.Backend.dto.ProgrammeResponse;
import com.exchangeHub.Backend.dto.UpdateProgrammeRequest;
import com.exchangeHub.Backend.enums.StatutProgramme;
import com.exchangeHub.Backend.enums.TypeMobilite;
import com.exchangeHub.Backend.service.ProgrammeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/programmes")
@RequiredArgsConstructor
public class ProgrammeController {

    private final ProgrammeService programmeService;

    @GetMapping
    public ResponseEntity<List<ProgrammeResponse>> getProgrammes(
        @RequestParam(required = false) StatutProgramme statut,
        @RequestParam(required = false) TypeMobilite typeMobilite,
        @RequestParam(required = false) String pays) {

        List<ProgrammeResponse> responses = programmeService.getProgrammes(statut, typeMobilite, pays);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProgrammeResponse> getProgramme(@PathVariable UUID id) {
        ProgrammeResponse response = programmeService.getProgramme(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ProgrammeResponse> createProgramme(@RequestBody CreateProgrammeRequest request) {
        ProgrammeResponse response = programmeService.createProgramme(request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProgrammeResponse> updateProgramme(
        @PathVariable UUID id,
        @RequestBody UpdateProgrammeRequest request) {

        ProgrammeResponse response = programmeService.updateProgramme(id, request);
        return ResponseEntity.ok(response);
    }
}
