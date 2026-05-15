package com.example.demo.modules.legaldocument.service;

import com.example.demo.modules.legaldocument.constants.LegalDocumentStreamConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LegalDocumentAnalyzeProducer {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 发送分析任务到 Redis Stream（初始重试次数为 0）
     */
    public void sendAnalyzeTask(Long legalDocumentId) {
        sendAnalyzeTask(legalDocumentId, 0);
    }

    /**
     * 发送分析任务到 Redis Stream（含重试次数）
     */
    public void sendAnalyzeTask(Long legalDocumentId, int retryCount) {
        MapRecord<String, String, String> record = MapRecord.create(
                LegalDocumentStreamConstants.LEGAL_DOCUMENT_ANALYZE_STREAM,
                Map.of(
                        LegalDocumentStreamConstants.FIELD_LEGAL_DOCUMENT_ID, String.valueOf(legalDocumentId),
                        LegalDocumentStreamConstants.FIELD_RETRY_COUNT, String.valueOf(retryCount)
                )
        );

        var messageId = stringRedisTemplate.opsForStream().add(record);

        log.info("法律文档分析任务已写入 Redis Stream，legalDocumentId={}，retryCount={}，messageId={}",
                legalDocumentId, retryCount, messageId);
    }
}
