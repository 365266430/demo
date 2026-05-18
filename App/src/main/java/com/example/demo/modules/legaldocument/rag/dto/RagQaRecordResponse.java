package com.example.demo.modules.legaldocument.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagQaRecordResponse {

    private Long id;

    private Long legalDocumentId;

    private String question;

    private String answer;

    private List<RagSourceChunk> sources;

    private LocalDateTime createdAt;
}
