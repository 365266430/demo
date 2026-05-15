package com.example.demo.modules.legaldocument.rag;

import com.example.demo.common.exception.BusinessException;
import com.example.demo.modules.legaldocument.rag.dto.RagAskRequest;
import com.example.demo.modules.legaldocument.rag.dto.RagAskResponse;
import com.example.demo.modules.legaldocument.rag.dto.RagIngestRequest;
import com.example.demo.modules.legaldocument.rag.dto.RagIngestResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class RagHttpClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public RagHttpClient(@Value("${rag.service.base-url:http://localhost:8000}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(120000);
        this.restTemplate = new RestTemplate(factory);
        this.baseUrl = trimTrailingSlash(baseUrl);
    }

    public RagIngestResponse ingest(RagIngestRequest request) {
        try {
            return restTemplate.postForObject(baseUrl + "/api/rag/ingest", request, RagIngestResponse.class);
        } catch (RestClientException e) {
            throw new BusinessException("RAG 服务入库调用失败", e);
        }
    }

    public RagAskResponse ask(RagAskRequest request) {
        try {
            return restTemplate.postForObject(baseUrl + "/api/rag/ask", request, RagAskResponse.class);
        } catch (RestClientException e) {
            throw new BusinessException("RAG 服务问答调用失败", e);
        }
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:8000";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}

