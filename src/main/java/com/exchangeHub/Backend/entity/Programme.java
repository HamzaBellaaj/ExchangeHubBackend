package com.exchangeHub.Backend.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.exchangeHub.Backend.enums.StatutProgramme;
import com.exchangeHub.Backend.enums.TypeMobilite;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "programme")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Programme {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "titre")
    private String titre;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "type_mobilite")
    @Enumerated(EnumType.STRING)
    private TypeMobilite typeMobilite;

    @Column(name = "pays")
    private String pays;

    @Column(name = "universite_partenaire")
    private String universitePartenaire;

    @Column(name = "date_debut")
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(name = "date_limite_candidature")
    private LocalDate dateLimiteCandidature;

    @Column(name = "statut")
    @Enumerated(EnumType.STRING)
    private StatutProgramme statut;

    @ManyToOne
    @JoinColumn(name = "coordinateur_id")
    private User coordinateur;

    @ManyToOne
    @JoinColumn(name = "responsable_id")
    private User responsable;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
