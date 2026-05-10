package com.exchangeHub.Backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.exchangeHub.Backend.entity.Programme;
import com.exchangeHub.Backend.enums.StatutProgramme;
import com.exchangeHub.Backend.enums.TypeMobilite;

@Repository
public interface ProgrammeRepository extends JpaRepository<Programme, UUID> {
    List<Programme> findByStatut(StatutProgramme statut);

    List<Programme> findByTypeMobilite(TypeMobilite typeMobilite);

    List<Programme> findByPaysIgnoreCase(String pays);
}
