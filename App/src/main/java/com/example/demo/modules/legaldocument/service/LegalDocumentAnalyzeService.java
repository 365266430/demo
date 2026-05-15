package com.example.demo.modules.legaldocument.service;

import com.example.demo.common.exception.BusinessException;
import com.example.demo.modules.legaldocument.dto.LegalDocumentAiResult;
import com.example.demo.modules.legaldocument.entity.LegalDocumentEntity;
import com.example.demo.modules.legaldocument.enums.LegalDocumentStatus;
import com.example.demo.modules.legaldocument.repository.LegalDocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LegalDocumentAnalyzeService {

    private final LegalDocumentRepository legalDocumentRepository;
    private final LegalDocumentAiAnalyzeService legalDocumentAiAnalyzeService;
    private final ObjectMapper objectMapper;

    /**
     * 对指定法律文档执行 AI 分析并更新状态
     */
    public void analyze(Long legalDocumentId) {
        log.info("开始异步分析法律文档，legalDocumentId={}", legalDocumentId);

        try {
            LegalDocumentEntity legalDocument = legalDocumentRepository.findById(legalDocumentId)
                    .orElseThrow(() -> new BusinessException("法律文档不存在，id=" + legalDocumentId));

            legalDocument.setStatus(LegalDocumentStatus.PROCESSING);
            legalDocumentRepository.save(legalDocument);

            LegalDocumentAiResult aiResult = legalDocumentAiAnalyzeService.analyze(legalDocument.getContent());

            legalDocument.setScore(aiResult.getRiskScore());
            legalDocument.setSummary(aiResult.getSummary());
            legalDocument.setAnalysisResult(objectMapper.writeValueAsString(aiResult));
            legalDocument.setStatus(LegalDocumentStatus.COMPLETED);
            legalDocument.setErrorMessage(null);

            legalDocumentRepository.save(legalDocument);

            log.info("法律文档 AI 分析完成，legalDocumentId={}", legalDocumentId);
        } catch (Exception e) {
            log.error("法律文档 AI 分析失败，legalDocumentId={}", legalDocumentId, e);

            legalDocumentRepository.findById(legalDocumentId).ifPresent(legalDocument -> {
                legalDocument.setStatus(LegalDocumentStatus.FAILED);
                legalDocument.setErrorMessage(e.getMessage());
                legalDocumentRepository.save(legalDocument);
            });
            throw new BusinessException("法律文档分析失败：" + e.getMessage(), e);
        }
    }
}
