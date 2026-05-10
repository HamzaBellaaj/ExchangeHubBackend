package com.exchangeHub.Backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.exchangeHub.Backend.entity.Decision;

@Repository
public interface DecisionRepository extends JpaRepository<Decision, UUID> {
    Optional<Decision> findByCandidature_Id(UUID candidatureId);

    boolean existsByCandidature_Id(UUID candidatureId);
}
