package com.example.demo.modules.legaldocument.rag.dto;

import lombok.Data;

@Data
public class RagAskRequest {

    private String documentId;

    private String question;

    private Integer topK = 5;
}

