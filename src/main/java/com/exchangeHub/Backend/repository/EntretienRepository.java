package com.exchangeHub.Backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.exchangeHub.Backend.entity.Entretien;

@Repository
public interface EntretienRepository extends JpaRepository<Entretien, UUID> {
    Optional<Entretien> findByCandidature_Id(UUID candidatureId);

    boolean existsByCandidature_Id(UUID candidatureId);
}
