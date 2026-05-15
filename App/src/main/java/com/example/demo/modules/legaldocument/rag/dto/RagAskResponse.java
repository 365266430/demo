package com.example.demo.modules.legaldocument.rag.dto;

import lombok.Data;

import java.util.List;

@Data
public class RagAskResponse {

    private String documentId;

    private String question;

    private String answer;

    private List<RagSourceChunk> sources;
}

