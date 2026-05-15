package com.example.demo.modules.legaldocument.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagIngestRequest {

    private String documentId;

    private String filename;

    private String content;
}

