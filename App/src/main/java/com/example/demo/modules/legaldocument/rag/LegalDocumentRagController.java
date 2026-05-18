package com.example.demo.modules.legaldocument.rag;

import com.example.demo.common.result.Result;
import com.example.demo.modules.legaldocument.rag.dto.RagAskRequest;
import com.example.demo.modules.legaldocument.rag.dto.RagAskResponse;
import com.example.demo.modules.legaldocument.rag.dto.RagIngestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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

    @GetMapping(value = "/ask-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter askStream(
            @PathVariable Long id,
            @RequestParam String question,
            @RequestParam(defaultValue = "5") Integer topK
    ) {
        RagAskRequest request = new RagAskRequest();
        request.setQuestion(question);
        request.setTopK(topK);
        return legalDocumentRagService.streamAsk(id, request);
    }

    @DeleteMapping("/qa-records/{recordId}")
    public Result<Void> deleteQaRecord(@PathVariable Long id, @PathVariable Long recordId) {
        legalDocumentRagService.deleteQaRecord(id, recordId);
        return Result.success("问答记录已删除", null);
    }
}
