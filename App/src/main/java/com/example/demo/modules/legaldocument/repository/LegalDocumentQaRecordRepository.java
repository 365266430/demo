package com.example.demo.modules.legaldocument.repository;

import com.example.demo.modules.legaldocument.entity.LegalDocumentQaRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LegalDocumentQaRecordRepository extends JpaRepository<LegalDocumentQaRecordEntity, Long> {

    List<LegalDocumentQaRecordEntity> findByLegalDocumentIdOrderByCreatedAtAsc(Long legalDocumentId);
}
