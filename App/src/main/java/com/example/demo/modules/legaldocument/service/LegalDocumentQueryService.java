package com.example.demo.modules.legaldocument.service;

import com.example.demo.common.exception.BusinessException;
import com.example.demo.modules.legaldocument.dto.LegalDocumentAiResult;
import com.example.demo.modules.legaldocument.dto.LegalDocumentDetailResponse;
import com.example.demo.modules.legaldocument.dto.LegalDocumentListResponse;
import com.example.demo.modules.legaldocument.entity.LegalDocumentEntity;
import com.example.demo.modules.legaldocument.repository.LegalDocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LegalDocumentQueryService {

    private final LegalDocumentRepository legalDocumentRepository;
    private final ObjectMapper objectMapper;

    /**
     * 查询所有法律文档列表
     */
    public List<LegalDocumentListResponse> list() {
        return legalDocumentRepository.findAll()
                .stream()
                .map(this::toListResponse)
                .toList();
    }

    /**
     * 根据 ID 查询法律文档详情
     */
    public LegalDocumentDetailResponse detail(Long id) {
        LegalDocumentEntity legalDocument = legalDocumentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("法律文档不存在，id=" + id));

        return toDetailResponse(legalDocument);
    }

    /**
     * 实体转列表响应 DTO
     */
    private LegalDocumentListResponse toListResponse(LegalDocumentEntity legalDocument) {
        LegalDocumentAiResult analysisResult = parseAnalysisResult(legalDocument.getAnalysisResult());

        return new LegalDocumentListResponse(
                legalDocument.getId(),
                legalDocument.getOriginalFilename(),
                legalDocument.getContentType(),
                legalDocument.getFileSize(),
                legalDocument.getStatus(),
                analysisResult == null ? null : analysisResult.getOverallRiskLevel(),
                legalDocument.getCreatedAt(),
                legalDocument.getUpdatedAt()
        );
    }

    /**
     * 实体转详情响应 DTO
     */
    private LegalDocumentDetailResponse toDetailResponse(LegalDocumentEntity legalDocument) {
        return new LegalDocumentDetailResponse(
                legalDocument.getId(),
                legalDocument.getOriginalFilename(),
                legalDocument.getContentType(),
                legalDocument.getFileSize(),
                legalDocument.getStatus(),
                legalDocument.getScore(),
                legalDocument.getSummary(),
                parseAnalysisResult(legalDocument.getAnalysisResult()),
                legalDocument.getErrorMessage(),
                legalDocument.getCreatedAt(),
                legalDocument.getUpdatedAt()
        );
    }

    /**
     * 将 JSON 字符串解析为 AI 分析结果对象
     */
    private LegalDocumentAiResult parseAnalysisResult(String analysisResult) {
        if (analysisResult == null || analysisResult.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(analysisResult, LegalDocumentAiResult.class);
        } catch (Exception e) {
            return null;
        }
    }
}
