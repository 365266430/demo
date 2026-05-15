package com.example.demo.modules.legaldocument.rag.dto;

import lombok.Data;

@Data
public class RagSourceChunk {

    private String documentId;

    private String chunkId;

    private Integer chunkIndex;

    private String content;

    private Double score;
}

