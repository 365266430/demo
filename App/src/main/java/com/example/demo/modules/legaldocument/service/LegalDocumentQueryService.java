package com.example.demo.modules.legaldocument.service;

import com.example.demo.common.exception.BusinessException;
import com.example.demo.modules.legaldocument.dto.LegalDocumentAiResult;
import com.example.demo.modules.legaldocument.dto.LegalDocumentDetailResponse;
import com.example.demo.modules.legaldocument.dto.LegalDocumentListResponse;
import com.example.demo.modules.legaldocument.entity.LegalDocumentEntity;
import com.example.demo.modules.legaldocument.entity.LegalDocumentQaRecordEntity;
import com.example.demo.modules.legaldocument.rag.dto.RagQaRecordResponse;
import com.example.demo.modules.legaldocument.rag.dto.RagSourceChunk;
import com.example.demo.modules.legaldocument.repository.LegalDocumentQaRecordRepository;
import com.example.demo.modules.legaldocument.repository.LegalDocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LegalDocumentQueryService {

    private final LegalDocumentRepository legalDocumentRepository;
    private final LegalDocumentQaRecordRepository qaRecordRepository;
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
                legalDocument.getRagStatus(),
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
                legalDocument.getRagStatus(),
                legalDocument.getRagChunkCount(),
                legalDocument.getRagErrorMessage(),
                legalDocument.getRagIndexedAt(),
                qaRecordRepository.findByLegalDocumentIdOrderByCreatedAtAsc(legalDocument.getId())
                        .stream()
                        .map(this::toQaRecordResponse)
                        .toList(),
                legalDocument.getCreatedAt(),
                legalDocument.getUpdatedAt()
        );
    }

    private RagQaRecordResponse toQaRecordResponse(LegalDocumentQaRecordEntity record) {
        return new RagQaRecordResponse(
                record.getId(),
                record.getLegalDocumentId(),
                record.getQuestion(),
                record.getAnswer(),
                parseSources(record.getSourcesJson()),
                record.getCreatedAt()
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

    private List<RagSourceChunk> parseSources(String sourcesJson) {
        if (sourcesJson == null || sourcesJson.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(sourcesJson, new TypeReference<List<RagSourceChunk>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
