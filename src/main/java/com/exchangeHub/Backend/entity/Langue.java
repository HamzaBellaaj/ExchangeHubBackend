package com.exchangeHub.Backend.entity;

import java.util.UUID;

import com.exchangeHub.Backend.enums.NiveauLangue;

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
@Table(name = "langue")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Langue {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "cv_profile_id", nullable = false)
    private CvProfile cvProfile;

    @Column(name = "langue", nullable = false)
    private String langue;

    @Column(name = "niveau", nullable = false)
    @Enumerated(EnumType.STRING)
    private NiveauLangue niveau;
}
