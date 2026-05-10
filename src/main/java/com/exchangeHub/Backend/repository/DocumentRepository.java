package com.exchangeHub.Backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.exchangeHub.Backend.entity.Document;
import com.exchangeHub.Backend.enums.TypeDocument;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findByCandidature_Id(UUID candidatureId);

    List<Document> findByCandidature_IdAndTypeDocument(UUID candidatureId, TypeDocument typeDocument);
}
