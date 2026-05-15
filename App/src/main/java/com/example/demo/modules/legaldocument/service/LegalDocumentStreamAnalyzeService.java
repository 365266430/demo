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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class LegalDocumentStreamAnalyzeService {

    private static final long SSE_TIMEOUT_MILLIS = 10 * 60 * 1000L;

    private final LegalDocumentRepository legalDocumentRepository;
    private final LegalDocumentAiAnalyzeService legalDocumentAiAnalyzeService;
    private final ObjectMapper objectMapper;

    /**
     * 创建 SSE 连接并异步启动法律文档流式分析。
     *
     * @param legalDocumentId 法律文档 ID
     * @param reanalyze 是否强制重新分析
     * @return 用于向客户端推送分析事件的 SSE 发送器
     */
    public SseEmitter streamAnalyze(Long legalDocumentId, boolean reanalyze) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);

        CompletableFuture.runAsync(() -> doStreamAnalyze(legalDocumentId, reanalyze, emitter));

        return emitter;
    }

    /**
     * 执行流式分析主流程，负责读取文档、更新状态并向客户端推送分片内容。
     *
     * @param legalDocumentId 法律文档 ID
     * @param reanalyze 是否强制重新分析
     * @param emitter SSE 事件发送器
     */
    private void doStreamAnalyze(Long legalDocumentId, boolean reanalyze, SseEmitter emitter) {
        StringBuilder fullContent = new StringBuilder();

        try {
            LegalDocumentEntity legalDocument = legalDocumentRepository.findById(legalDocumentId)
                    .orElseThrow(() -> new BusinessException("法律文档不存在，id=" + legalDocumentId));

            if (!reanalyze && legalDocument.getStatus() == LegalDocumentStatus.COMPLETED
                    && legalDocument.getAnalysisResult() != null && !legalDocument.getAnalysisResult().isBlank()) {
                sendEvent(emitter, "done", legalDocument.getAnalysisResult());
                emitter.complete();
                return;
            }

            if (legalDocument.getContent() == null || legalDocument.getContent().isBlank()) {
                throw new BusinessException("法律文档内容为空，无法进行 AI 分析");
            }

            legalDocument.setStatus(LegalDocumentStatus.PROCESSING);
            legalDocument.setScore(null);
            legalDocument.setSummary(null);
            legalDocument.setAnalysisResult(null);
            legalDocument.setErrorMessage(null);
            legalDocumentRepository.save(legalDocument);

            sendEvent(emitter, "status", "PROCESSING");

            legalDocumentAiAnalyzeService.streamAnalyze(legalDocument.getContent())
                    .doOnNext(chunk -> {
                        fullContent.append(chunk);
                        sendEvent(emitter, "chunk", chunk);
                    })
                    .doOnComplete(() -> handleComplete(legalDocumentId, emitter, fullContent.toString()))
                    .blockLast();
        } catch (Exception e) {
            handleError(legalDocumentId, emitter, e);
        }
    }

    /**
     * 处理流式分析完成后的结果解析、持久化和完成事件推送。
     *
     * @param legalDocumentId 法律文档 ID
     * @param emitter SSE 事件发送器
     * @param rawResult AI 返回的原始分析结果
     */
    private void handleComplete(Long legalDocumentId, SseEmitter emitter, String rawResult) {
        try {
            LegalDocumentAiResult aiResult = legalDocumentAiAnalyzeService.parseResult(rawResult);
            String analysisResult = objectMapper.writeValueAsString(aiResult);

            LegalDocumentEntity legalDocument = legalDocumentRepository.findById(legalDocumentId)
                    .orElseThrow(() -> new BusinessException("法律文档不存在，id=" + legalDocumentId));

            legalDocument.setScore(aiResult.getRiskScore());
            legalDocument.setSummary(aiResult.getSummary());
            legalDocument.setAnalysisResult(analysisResult);
            legalDocument.setStatus(LegalDocumentStatus.COMPLETED);
            legalDocument.setErrorMessage(null);
            legalDocumentRepository.save(legalDocument);

            sendEvent(emitter, "done", analysisResult);
            emitter.complete();
        } catch (Exception e) {
            handleError(legalDocumentId, emitter, e);
        }
    }

    /**
     * 处理流式分析异常，记录错误、更新文档状态并向客户端推送错误事件。
     *
     * @param legalDocumentId 法律文档 ID
     * @param emitter SSE 事件发送器
     * @param error 分析过程中抛出的异常
     */
    private void handleError(Long legalDocumentId, SseEmitter emitter, Throwable error) {
        log.error("法律文档流式分析失败，legalDocumentId={}", legalDocumentId, error);

        legalDocumentRepository.findById(legalDocumentId).ifPresent(legalDocument -> {
            legalDocument.setStatus(LegalDocumentStatus.FAILED);
            legalDocument.setErrorMessage(error.getMessage());
            legalDocumentRepository.save(legalDocument);
        });

        try {
            sendEvent(emitter, "analysis-error", error.getMessage());
            emitter.complete();
        } catch (Exception ignored) {
            emitter.completeWithError(error);
        }
    }

    /**
     * 向客户端发送指定名称和内容的 SSE 事件。
     *
     * @param emitter SSE 事件发送器
     * @param name 事件名称
     * @param data 事件数据
     */
    private void sendEvent(SseEmitter emitter, String name, String data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data == null ? "" : data));
        } catch (IOException e) {
            throw new BusinessException("SSE 消息发送失败：" + e.getMessage(), e);
        }
    }
}
