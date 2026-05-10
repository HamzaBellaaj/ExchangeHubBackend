package com.exchangeHub.Backend.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.exchangeHub.Backend.entity.Formation;

@Repository
public interface FormationRepository extends JpaRepository<Formation, UUID> {
    void deleteByCvProfile_Id(UUID cvProfileId);
}
