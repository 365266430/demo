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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LegalDocumentRagService {

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

    public void tryIngest(Long id) {
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

    private LegalDocumentEntity getDocument(Long id) {
        return legalDocumentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "法律文档不存在"));
    }
}
