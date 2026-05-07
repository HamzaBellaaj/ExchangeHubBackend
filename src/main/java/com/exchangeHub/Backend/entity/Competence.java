package com.exchangeHub.Backend.entity;

import java.util.UUID;

import com.exchangeHub.Backend.enums.NiveauCompetence;

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
@Table(name = "competence")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Competence {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "cv_profile_id", nullable = false)
    private CvProfile cvProfile;

    @Column(name = "nom", nullable = false)
    private String nom;

    @Column(name = "niveau")
    @Enumerated(EnumType.STRING)
    private NiveauCompetence niveau;
}
