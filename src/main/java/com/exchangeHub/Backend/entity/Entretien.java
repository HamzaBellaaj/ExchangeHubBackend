package com.exchangeHub.Backend.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.exchangeHub.Backend.enums.ModeEntretien;
import com.exchangeHub.Backend.enums.StatutEntretien;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "entretien")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Entretien {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @OneToOne
    @JoinColumn(name = "candidature_id", unique = true, nullable = false)
    private Candidature candidature;

    @Column(name = "date_entretien")
    private LocalDateTime dateEntretien;

    @Column(name = "mode")
    @Enumerated(EnumType.STRING)
    private ModeEntretien mode;

    @Column(name = "lien_visio", columnDefinition = "TEXT")
    private String lienVisio;

    @Column(name = "lieu")
    private String lieu;

    @Column(name = "statut")
    @Enumerated(EnumType.STRING)
    private StatutEntretien statut;

    @ManyToOne
    @JoinColumn(name = "planifie_par_id")
    private User planifiePar;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
