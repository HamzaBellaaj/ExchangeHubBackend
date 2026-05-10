package com.exchangeHub.Backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.exchangeHub.Backend.entity.Candidature;
import com.exchangeHub.Backend.enums.StatutCandidature;

@Repository
public interface CandidatureRepository extends JpaRepository<Candidature, UUID> {
    boolean existsByCandidatIdAndProgrammeId(UUID candidatId, UUID programmeId);

    List<Candidature> findByStatut(StatutCandidature statut);

    List<Candidature> findByProgrammeId(UUID programmeId);

    List<Candidature> findByCandidatId(UUID candidatId);

    List<Candidature> findByArchivedAtIsNull();

    List<Candidature> findByCandidat_IdAndArchivedAtIsNull(UUID candidatId);
}
