package com.example.demo.modules.legaldocument.service;

import com.example.demo.common.exception.BusinessException;
import com.example.demo.modules.legaldocument.entity.LegalDocumentEntity;
import com.example.demo.modules.legaldocument.repository.LegalDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LegalDocumentDeleteService {

    private final LegalDocumentRepository legalDocumentRepository;

    /**
     * 根据 ID 删除法律文档
     */
    public void delete(Long id) {
        LegalDocumentEntity legalDocument = legalDocumentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("法律文档不存在，id=" + id));

        legalDocumentRepository.delete(legalDocument);
    }
}
