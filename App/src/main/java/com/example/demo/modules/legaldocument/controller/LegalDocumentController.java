package com.example.demo.modules.legaldocument.controller;

import com.example.demo.common.result.Result;
import com.example.demo.modules.legaldocument.dto.LegalDocumentDetailResponse;
import com.example.demo.modules.legaldocument.dto.LegalDocumentListResponse;
import com.example.demo.modules.legaldocument.dto.LegalDocumentUploadResponse;
import com.example.demo.modules.legaldocument.entity.LegalDocumentEntity;
import com.example.demo.modules.legaldocument.enums.LegalDocumentStatus;
import com.example.demo.modules.legaldocument.repository.LegalDocumentRepository;
import com.example.demo.modules.legaldocument.service.LegalDocumentDeleteService;
import com.example.demo.modules.legaldocument.service.LegalDocumentQueryService;
import com.example.demo.modules.legaldocument.service.LegalDocumentReanalyzeService;
import com.example.demo.modules.legaldocument.service.LegalDocumentStreamAnalyzeService;
import com.example.demo.modules.legaldocument.service.LegalDocumentUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/legal-documents")
@RequiredArgsConstructor
public class LegalDocumentController {

    private final LegalDocumentRepository legalDocumentRepository;
    private final LegalDocumentUploadService legalDocumentUploadService;
    private final LegalDocumentQueryService legalDocumentQueryService;
    private final LegalDocumentDeleteService legalDocumentDeleteService;
    private final LegalDocumentReanalyzeService legalDocumentReanalyzeService;
    private final LegalDocumentStreamAnalyzeService legalDocumentStreamAnalyzeService;

    /**
     * 查询法律文档列表。
     *
     * @return 法律文档列表响应
     */
    @GetMapping
    public Result<List<LegalDocumentListResponse>> list() {
        return Result.success(legalDocumentQueryService.list());
    }

    /**
     * 查询指定法律文档的详情。
     *
     * @param id 法律文档 ID
     * @return 法律文档详情响应
     */
    @GetMapping("/{id}")
    public Result<LegalDocumentDetailResponse> detail(@PathVariable Long id) {
        return Result.success(legalDocumentQueryService.detail(id));
    }

    /**
     * 创建一条测试法律文档数据。
     *
     * @return 已创建的测试法律文档
     */
    @PostMapping("/test")
    public Result<LegalDocumentEntity> createTestLegalDocument() {
        LegalDocumentEntity legalDocument = new LegalDocumentEntity();
        legalDocument.setOriginalFilename("test-legal-document.txt");
        legalDocument.setContentType("text/plain");
        legalDocument.setFileSize(1024L);
        legalDocument.setContent("这是一份测试法律文档内容。");
        legalDocument.setStatus(LegalDocumentStatus.PENDING);

        LegalDocumentEntity saved = legalDocumentRepository.save(legalDocument);
        return Result.success("测试法律文档创建成功", saved);
    }

    /**
     * 上传法律文档文件，并根据请求参数决定是否立即执行非流式分析。
     *
     * @param file 上传的法律文档文件
     * @param stream 是否使用流式分析
     * @return 上传后的法律文档信息
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<LegalDocumentUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "stream", defaultValue = "false") boolean stream) {
        LegalDocumentUploadResponse response = legalDocumentUploadService.upload(file, !stream);
        return Result.success("上传成功，等待分析", response);
    }

    /**
     * 通过 SSE 流式返回指定法律文档的分析过程和结果。
     *
     * @param id 法律文档 ID
     * @param reanalyze 是否重新分析
     * @return SSE 事件发送器
     */
    @GetMapping(value = "/{id}/analysis-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAnalyze(
            @PathVariable Long id,
            @RequestParam(name = "reanalyze", defaultValue = "false") boolean reanalyze) {
        return legalDocumentStreamAnalyzeService.streamAnalyze(id, reanalyze);
    }

    /**
     * 删除指定法律文档。
     *
     * @param id 法律文档 ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        legalDocumentDeleteService.delete(id);
        return Result.success("删除成功", null);
    }

    /**
     * 提交指定法律文档的重新分析任务。
     *
     * @param id 法律文档 ID
     * @return 重新分析任务提交结果
     */
    @PostMapping("/{id}/reanalyze")
    public Result<LegalDocumentUploadResponse> reanalyze(@PathVariable Long id) {
        LegalDocumentUploadResponse response = legalDocumentReanalyzeService.reanalyze(id);
        return Result.success("重新分析任务已提交", response);
    }
}
