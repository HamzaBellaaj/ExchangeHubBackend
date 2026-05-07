package com.exchangeHub.Backend.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.exchangeHub.Backend.enums.DecisionFinale;

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
@Table(name = "decision")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Decision {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @OneToOne
    @JoinColumn(name = "candidature_id", unique = true, nullable = false)
    private Candidature candidature;

    @Column(name = "decision", nullable = false)
    @Enumerated(EnumType.STRING)
    private DecisionFinale decision;

    @ManyToOne
    @JoinColumn(name = "responsable_id")
    private User responsable;

    @Column(name = "commentaire", columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;
}
