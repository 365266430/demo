package com.example.demo.modules.legaldocument.service;

import com.example.demo.common.exception.BusinessException;
import com.example.demo.modules.legaldocument.dto.LegalDocumentUploadResponse;
import com.example.demo.modules.legaldocument.entity.LegalDocumentEntity;
import com.example.demo.modules.legaldocument.enums.LegalDocumentStatus;
import com.example.demo.modules.legaldocument.rag.LegalDocumentRagService;
import com.example.demo.modules.legaldocument.repository.LegalDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class LegalDocumentUploadService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private final LegalDocumentAnalyzeProducer legalDocumentAnalyzeProducer;
    private final LegalDocumentRepository legalDocumentRepository;
    private final DocumentParseService documentParseService;
    private final LegalDocumentRagService legalDocumentRagService;

    /**
     * 上传法律文档并默认提交异步分析任务。
     *
     * @param file 上传的法律文档文件
     * @return 上传后的文档基础信息
     */
    public LegalDocumentUploadResponse upload(MultipartFile file) {
        return upload(file, true);
    }

    /**
     * 上传法律文档，保存解析后的文档内容，并按需提交异步分析任务。
     *
     * @param file 上传的法律文档文件
     * @param enqueueAnalyzeTask 是否提交异步分析任务
     * @return 上传后的文档基础信息
     */
    public LegalDocumentUploadResponse upload(MultipartFile file, boolean enqueueAnalyzeTask) {
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String contentType = documentParseService.detectContentType(file);
        long fileSize = file.getSize();
        String content = documentParseService.parse(file);

        LegalDocumentEntity legalDocument = new LegalDocumentEntity();
        legalDocument.setOriginalFilename(originalFilename);
        legalDocument.setContentType(contentType);
        legalDocument.setFileSize(fileSize);
        legalDocument.setContent(content);
        legalDocument.setStatus(LegalDocumentStatus.PENDING);

        LegalDocumentEntity saved = legalDocumentRepository.save(legalDocument);
        legalDocumentRagService.tryIngest(saved.getId());
        if (enqueueAnalyzeTask) {
            legalDocumentAnalyzeProducer.sendAnalyzeTask(saved.getId());
        }

        return new LegalDocumentUploadResponse(
                saved.getId(),
                saved.getStatus(),
                saved.getOriginalFilename()
        );
    }

    /**
     * 校验上传文件是否存在、是否超出大小限制以及文件名是否合法。
     *
     * @param file 待校验的上传文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("文件大小不能超过 10MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessException("文件名不能为空");
        }
    }
}
