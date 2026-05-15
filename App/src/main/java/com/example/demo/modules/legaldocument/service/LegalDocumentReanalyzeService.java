package com.example.demo.modules.legaldocument.service;

import com.example.demo.common.exception.BusinessException;
import com.example.demo.modules.legaldocument.dto.LegalDocumentUploadResponse;
import com.example.demo.modules.legaldocument.entity.LegalDocumentEntity;
import com.example.demo.modules.legaldocument.enums.LegalDocumentStatus;
import com.example.demo.modules.legaldocument.repository.LegalDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LegalDocumentReanalyzeService {

    private final LegalDocumentRepository legalDocumentRepository;
    private final LegalDocumentAnalyzeProducer legalDocumentAnalyzeProducer;

    /**
     * 重新提交法律文档分析任务
     */
    public LegalDocumentUploadResponse reanalyze(Long id) {
        LegalDocumentEntity legalDocument = legalDocumentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("法律文档不存在，id=" + id));

        if (legalDocument.getContent() == null || legalDocument.getContent().isBlank()) {
            throw new BusinessException("法律文档内容为空，无法重新分析");
        }

        if (legalDocument.getStatus() == LegalDocumentStatus.PROCESSING) {
            throw new BusinessException("法律文档正在分析中，请稍后再试");
        }

        legalDocument.setStatus(LegalDocumentStatus.PENDING);
        legalDocument.setScore(null);
        legalDocument.setSummary(null);
        legalDocument.setAnalysisResult(null);
        legalDocument.setErrorMessage(null);

        LegalDocumentEntity saved = legalDocumentRepository.save(legalDocument);
        legalDocumentAnalyzeProducer.sendAnalyzeTask(saved.getId());

        return new LegalDocumentUploadResponse(
                saved.getId(),
                saved.getStatus(),
                saved.getOriginalFilename()
        );
    }
}
