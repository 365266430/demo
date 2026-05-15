package com.example.demo.modules.legaldocument.dto;

import com.example.demo.modules.legaldocument.enums.LegalDocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LegalDocumentDetailResponse {

    private Long id;

    private String originalFilename;

    private String contentType;

    private Long fileSize;

    private LegalDocumentStatus status;

    private Integer score;

    private String summary;

    private LegalDocumentAiResult analysisResult;

    private String errorMessage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
