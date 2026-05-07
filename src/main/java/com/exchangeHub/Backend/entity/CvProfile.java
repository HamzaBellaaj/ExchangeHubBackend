package com.exchangeHub.Backend.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cv_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CvProfile {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @OneToOne
    @JoinColumn(name = "candidature_id", unique = true)
    private Candidature candidature;

    @Column(name = "nom_complet")
    private String nomComplet;

    @Column(name = "email")
    private String email;

    @Column(name = "telephone")
    private String telephone;

    @Column(name = "titre_profil")
    private String titreProfil;

    @Column(name = "resume", columnDefinition = "TEXT")
    private String resume;

    @Column(name = "annees_experience")
    private Integer anneesExperience;

    @Column(name = "parsed_at")
    private LocalDateTime parsedAt;
}
