package com.example.demo.modules.legaldocument.rag;

import com.example.demo.common.exception.BusinessException;
import com.example.demo.modules.legaldocument.entity.LegalDocumentEntity;
import com.example.demo.modules.legaldocument.entity.LegalDocumentQaRecordEntity;
import com.example.demo.modules.legaldocument.enums.RagIndexStatus;
import com.example.demo.modules.legaldocument.rag.dto.RagAskRequest;
import com.example.demo.modules.legaldocument.rag.dto.RagAskResponse;
import com.example.demo.modules.legaldocument.rag.dto.RagIngestRequest;
import com.example.demo.modules.legaldocument.rag.dto.RagIngestResponse;
import com.example.demo.modules.legaldocument.repository.LegalDocumentQaRecordRepository;
import com.example.demo.modules.legaldocument.repository.LegalDocumentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class LegalDocumentRagService {

    private static final long QA_STREAM_TIMEOUT_MILLIS = 5 * 60 * 1000L;
    private static final int QA_STREAM_CHUNK_SIZE = 24;

    private final LegalDocumentRepository legalDocumentRepository;
    private final LegalDocumentQaRecordRepository qaRecordRepository;
    private final RagHttpClient ragHttpClient;
    private final ObjectMapper objectMapper;

    public RagIngestResponse ingest(Long id) {
        LegalDocumentEntity document = getDocument(id);
        if (document.getContent() == null || document.getContent().isBlank()) {
            throw new BusinessException("文档内容为空，无法建立 RAG 索引");
        }

        document.setRagStatus(RagIndexStatus.INDEXING);
        document.setRagErrorMessage(null);
        legalDocumentRepository.save(document);

        RagIngestRequest request = new RagIngestRequest(
                String.valueOf(document.getId()),
                document.getOriginalFilename(),
                document.getContent()
        );
        try {
            RagIngestResponse response = ragHttpClient.ingest(request);
            document.setRagStatus(RagIndexStatus.INDEXED);
            document.setRagChunkCount(response == null ? 0 : response.getChunkCount());
            document.setRagErrorMessage(null);
            document.setRagIndexedAt(LocalDateTime.now());
            legalDocumentRepository.save(document);
            return response;
        } catch (RuntimeException e) {
            document.setRagStatus(RagIndexStatus.FAILED);
            document.setRagErrorMessage(e.getMessage());
            legalDocumentRepository.save(document);
            throw e;
        }
    }

    public RagAskResponse ask(Long id, RagAskRequest request) {
        getDocument(id);
        if (request == null || request.getQuestion() == null || request.getQuestion().isBlank()) {
            throw new BusinessException(400, "问题不能为空");
        }

        request.setDocumentId(String.valueOf(id));
        if (request.getTopK() == null || request.getTopK() <= 0) {
            request.setTopK(5);
        }
        RagAskResponse response = ragHttpClient.ask(request);
        saveQaRecord(id, response);
        return response;
    }

    public SseEmitter streamAsk(Long id, RagAskRequest request) {
        SseEmitter emitter = new SseEmitter(QA_STREAM_TIMEOUT_MILLIS);

        CompletableFuture.runAsync(() -> {
            try {
                RagAskResponse response = ask(id, request);
                streamAnswer(emitter, response == null ? "" : response.getAnswer());
                sendEvent(emitter, "done", "");
                emitter.complete();
            } catch (Exception e) {
                try {
                    sendEvent(emitter, "qa-error", e.getMessage());
                    emitter.complete();
                } catch (Exception ignored) {
                    emitter.completeWithError(e);
                }
            }
        });

        return emitter;
    }

    @Async
    public void ingestAsync(Long id) {
        try {
            ingest(id);
        } catch (RuntimeException ignored) {
        }
    }

    private void saveQaRecord(Long legalDocumentId, RagAskResponse response) {
        if (response == null) {
            return;
        }

        LegalDocumentQaRecordEntity record = new LegalDocumentQaRecordEntity();
        record.setLegalDocumentId(legalDocumentId);
        record.setQuestion(response.getQuestion());
        record.setAnswer(response.getAnswer());
        try {
            record.setSourcesJson(objectMapper.writeValueAsString(response.getSources()));
        } catch (JsonProcessingException e) {
            record.setSourcesJson("[]");
        }
        qaRecordRepository.save(record);
    }

    public void deleteQaRecord(Long legalDocumentId, Long recordId) {
        getDocument(legalDocumentId);
        LegalDocumentQaRecordEntity record = qaRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException(404, "问答记录不存在"));

        if (!legalDocumentId.equals(record.getLegalDocumentId())) {
            throw new BusinessException(404, "问答记录不存在");
        }

        qaRecordRepository.delete(record);
    }

    private void streamAnswer(SseEmitter emitter, String answer) {
        String content = answer == null ? "" : answer;
        if (content.isBlank()) {
            sendEvent(emitter, "chunk", "");
            return;
        }

        for (int start = 0; start < content.length(); start += QA_STREAM_CHUNK_SIZE) {
            int end = Math.min(start + QA_STREAM_CHUNK_SIZE, content.length());
            sendEvent(emitter, "chunk", content.substring(start, end));
        }
    }

    private void sendEvent(SseEmitter emitter, String name, String data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data == null ? "" : data));
        } catch (IOException e) {
            throw new BusinessException("SSE 消息发送失败：" + e.getMessage(), e);
        }
    }

    private LegalDocumentEntity getDocument(Long id) {
        return legalDocumentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "法律文档不存在"));
    }
}
