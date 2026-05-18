package com.example.demo.modules.legaldocument.dto;

import com.example.demo.modules.legaldocument.enums.LegalDocumentStatus;
import com.example.demo.modules.legaldocument.enums.RagIndexStatus;
import com.example.demo.modules.legaldocument.rag.dto.RagQaRecordResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

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

    private RagIndexStatus ragStatus;

    private Integer ragChunkCount;

    private String ragErrorMessage;

    private LocalDateTime ragIndexedAt;

    private List<RagQaRecordResponse> qaRecords;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
