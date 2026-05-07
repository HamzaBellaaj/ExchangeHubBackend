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
@Table(name = "analyse_cv")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyseCv {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @OneToOne
    @JoinColumn(name = "cv_profile_id", unique = true, nullable = false)
    private CvProfile cvProfile;

    @Column(name = "score_global")
    private Integer scoreGlobal;

    @Column(name = "score_experience")
    private Integer scoreExperience;

    @Column(name = "score_formation")
    private Integer scoreFormation;

    @Column(name = "score_langues")
    private Integer scoreLangues;

    @Column(name = "score_competences")
    private Integer scoreCompetences;

    @Column(name = "points_forts", columnDefinition = "TEXT")
    private String pointsForts;

    @Column(name = "points_faibles", columnDefinition = "TEXT")
    private String pointsFaibles;

    @Column(name = "recommandation", columnDefinition = "TEXT")
    private String recommandation;

    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;
}
