package com.example.demo.modules.legaldocument.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "legal_document_qa_record")
public class LegalDocumentQaRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "legal_document_id", nullable = false)
    private Long legalDocumentId;

    @Lob
    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    private String question;

    @Lob
    @Column(name = "answer", columnDefinition = "LONGTEXT")
    private String answer;

    @Lob
    @Column(name = "sources_json", columnDefinition = "LONGTEXT")
    private String sourcesJson;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
