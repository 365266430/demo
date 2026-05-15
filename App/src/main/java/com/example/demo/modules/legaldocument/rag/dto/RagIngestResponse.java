package com.example.demo.modules.legaldocument.rag.dto;

import lombok.Data;

@Data
public class RagIngestResponse {

    private String documentId;

    private Integer chunkCount;
}

