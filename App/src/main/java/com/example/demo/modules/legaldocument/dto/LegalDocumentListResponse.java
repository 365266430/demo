package com.example.demo.modules.legaldocument.dto;

import com.example.demo.modules.legaldocument.enums.LegalDocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LegalDocumentListResponse {

    private Long id;

    private String originalFilename;

    private String contentType;

    private Long fileSize;

    private LegalDocumentStatus status;

    private String overallRiskLevel;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
