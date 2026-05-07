package com.exchangeHub.Backend.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.exchangeHub.Backend.entity.Entretien;

@Repository
public interface EntretienRepository extends JpaRepository<Entretien, UUID> {
}
