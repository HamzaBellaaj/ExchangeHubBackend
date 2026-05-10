package com.exchangeHub.Backend.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.exchangeHub.Backend.entity.Experience;

@Repository
public interface ExperienceRepository extends JpaRepository<Experience, UUID> {
    void deleteByCvProfile_Id(UUID cvProfileId);
}
