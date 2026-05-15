package com.example.demo.modules.legaldocument.dto;

import com.example.demo.modules.legaldocument.enums.LegalDocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LegalDocumentUploadResponse {

    private Long legalDocumentId;

    private LegalDocumentStatus status;

    private String originalFilename;
}
