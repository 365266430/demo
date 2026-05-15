package com.example.demo.modules.legaldocument.repository;

import com.example.demo.modules.legaldocument.entity.LegalDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LegalDocumentRepository extends JpaRepository<LegalDocumentEntity, Long> {
}

