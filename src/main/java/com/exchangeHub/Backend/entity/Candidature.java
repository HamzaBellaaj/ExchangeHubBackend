package com.exchangeHub.Backend.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.exchangeHub.Backend.enums.StatutCandidature;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "candidature",
    uniqueConstraints = @UniqueConstraint(columnNames = {"candidat_id", "programme_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Candidature {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "candidat_id")
    private User candidat;

    @ManyToOne
    @JoinColumn(name = "programme_id")
    private Programme programme;

    @Column(name = "statut")
    @Enumerated(EnumType.STRING)
    private StatutCandidature statut;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @ManyToOne
    @JoinColumn(name = "archived_by")
    private User archivedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
