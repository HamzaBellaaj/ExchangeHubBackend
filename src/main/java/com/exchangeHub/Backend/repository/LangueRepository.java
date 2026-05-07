package com.exchangeHub.Backend.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.exchangeHub.Backend.entity.Langue;

@Repository
public interface LangueRepository extends JpaRepository<Langue, UUID> {
}
