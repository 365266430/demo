package com.example.demo.modules.legaldocument.rag;

import com.example.demo.common.result.Result;
import com.example.demo.modules.legaldocument.rag.dto.RagAskRequest;
import com.example.demo.modules.legaldocument.rag.dto.RagAskResponse;
import com.example.demo.modules.legaldocument.rag.dto.RagIngestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/legal-documents/{id}/rag")
@RequiredArgsConstructor
public class LegalDocumentRagController {

    private final LegalDocumentRagService legalDocumentRagService;

    @PostMapping("/ingest")
    public Result<RagIngestResponse> ingest(@PathVariable Long id) {
        return Result.success("RAG 索引建立成功", legalDocumentRagService.ingest(id));
    }

    @PostMapping("/ask")
    public Result<RagAskResponse> ask(@PathVariable Long id, @RequestBody RagAskRequest request) {
        return Result.success(legalDocumentRagService.ask(id, request));
    }
}

