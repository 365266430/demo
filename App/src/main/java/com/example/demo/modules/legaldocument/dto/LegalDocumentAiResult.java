package com.example.demo.modules.legaldocument.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LegalDocumentAiResult {

    private String documentType;

    private String summary;

    private List<String> parties;

    private List<String> keyClauses;

    private List<String> risks;

    private List<String> suggestions;

    private String overallRiskLevel;

    private Integer riskScore;

    private String riskLevel;

    private List<String> riskClauses;

    private List<String> obligations;

    private List<String> reviewQuestions;
}
