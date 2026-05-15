package com.example.demo.modules.legaldocument.rag;

import com.example.demo.common.exception.BusinessException;
import com.example.demo.modules.legaldocument.entity.LegalDocumentEntity;
import com.example.demo.modules.legaldocument.rag.dto.RagAskRequest;
import com.example.demo.modules.legaldocument.rag.dto.RagAskResponse;
import com.example.demo.modules.legaldocument.rag.dto.RagIngestRequest;
import com.example.demo.modules.legaldocument.rag.dto.RagIngestResponse;
import com.example.demo.modules.legaldocument.repository.LegalDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LegalDocumentRagService {

    private final LegalDocumentRepository legalDocumentRepository;
    private final RagHttpClient ragHttpClient;

    public RagIngestResponse ingest(Long id) {
        LegalDocumentEntity document = getDocument(id);
        if (document.getContent() == null || document.getContent().isBlank()) {
            throw new BusinessException("文档内容为空，无法建立 RAG 索引");
        }

        RagIngestRequest request = new RagIngestRequest(
                String.valueOf(document.getId()),
                document.getOriginalFilename(),
                document.getContent()
        );
        return ragHttpClient.ingest(request);
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
        return ragHttpClient.ask(request);
    }

    private LegalDocumentEntity getDocument(Long id) {
        return legalDocumentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "法律文档不存在"));
    }
}

