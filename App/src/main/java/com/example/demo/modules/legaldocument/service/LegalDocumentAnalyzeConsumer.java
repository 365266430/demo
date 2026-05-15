package com.example.demo.modules.legaldocument.service;

import com.example.demo.modules.legaldocument.constants.LegalDocumentStreamConstants;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LegalDocumentAnalyzeConsumer {

    private final StringRedisTemplate stringRedisTemplate;
    private final LegalDocumentAnalyzeService legalDocumentAnalyzeService;
    private final LegalDocumentAnalyzeProducer legalDocumentAnalyzeProducer;

    /**
     * 初始化 Redis Stream 消费组（应用启动时执行）
     */
    @PostConstruct
    public void initConsumerGroup() {
        try {
            Boolean hasKey = stringRedisTemplate.hasKey(LegalDocumentStreamConstants.LEGAL_DOCUMENT_ANALYZE_STREAM);

            if (Boolean.FALSE.equals(hasKey)) {
                stringRedisTemplate.opsForStream().add(
                        StreamRecords.newRecord()
                                .in(LegalDocumentStreamConstants.LEGAL_DOCUMENT_ANALYZE_STREAM)
                                .ofMap(Map.of("init", "true"))
                );
            }

            stringRedisTemplate.opsForStream().createGroup(
                    LegalDocumentStreamConstants.LEGAL_DOCUMENT_ANALYZE_STREAM,
                    LegalDocumentStreamConstants.LEGAL_DOCUMENT_ANALYZE_GROUP
            );

            log.info("Redis Stream 消费组创建成功：stream={}，group={}",
                    LegalDocumentStreamConstants.LEGAL_DOCUMENT_ANALYZE_STREAM,
                    LegalDocumentStreamConstants.LEGAL_DOCUMENT_ANALYZE_GROUP);
        } catch (DataAccessException e) {
            String message = e.getMessage();

            if (message != null && message.contains("BUSYGROUP")) {
                log.info("Redis Stream 消费组已存在，无需重复创建");
            } else {
                log.warn("Redis Stream 消费组初始化异常：{}", message);
            }
        }
    }

    /**
     * 定时消费 Redis Stream 中的法律文档分析任务
     */
    @Scheduled(fixedDelay = 1000)
    public void consume() {
        try {
            List<MapRecord<String, Object, Object>> records =
                    stringRedisTemplate.opsForStream().read(
                            Consumer.from(
                                    LegalDocumentStreamConstants.LEGAL_DOCUMENT_ANALYZE_GROUP,
                                    LegalDocumentStreamConstants.LEGAL_DOCUMENT_ANALYZE_CONSUMER
                            ),
                            StreamReadOptions.empty()
                                    .count(5)
                                    .block(Duration.ofSeconds(1)),
                            StreamOffset.create(
                                    LegalDocumentStreamConstants.LEGAL_DOCUMENT_ANALYZE_STREAM,
                                    ReadOffset.lastConsumed()
                            )
                    );

            if (records == null || records.isEmpty()) {
                return;
            }

            for (MapRecord<String, Object, Object> record : records) {
                handleRecord(record);
            }
        } catch (Exception e) {
            log.error("消费 Redis Stream 法律文档分析任务异常", e);
        }
    }

    /**
     * 处理单条消息记录，执行分析并在失败时重试
     */
    private void handleRecord(MapRecord<String, Object, Object> record) {
        Object legalDocumentIdObj = record.getValue().get(LegalDocumentStreamConstants.FIELD_LEGAL_DOCUMENT_ID);

        if (legalDocumentIdObj == null) {
            ack(record.getId());
            return;
        }

        Long legalDocumentId = Long.valueOf(legalDocumentIdObj.toString());
        int retryCount = getRetryCount(record);

        try {
            log.info("开始消费法律文档分析任务，legalDocumentId={}，retryCount={}，messageId={}",
                    legalDocumentId, retryCount, record.getId());

            legalDocumentAnalyzeService.analyze(legalDocumentId);
            ack(record.getId());

            log.info("法律文档分析任务消费完成，legalDocumentId={}，retryCount={}，messageId={}",
                    legalDocumentId, retryCount, record.getId());
        } catch (Exception e) {
            log.error("法律文档分析任务消费失败，legalDocumentId={}，retryCount={}，messageId={}",
                    legalDocumentId, retryCount, record.getId(), e);

            if (retryCount < LegalDocumentStreamConstants.MAX_RETRY_COUNT) {
                legalDocumentAnalyzeProducer.sendAnalyzeTask(legalDocumentId, retryCount + 1);
                log.warn("法律文档分析任务已重新投递，legalDocumentId={}，nextRetryCount={}",
                        legalDocumentId, retryCount + 1);
            } else {
                log.error("法律文档分析任务达到最大重试次数，legalDocumentId={}", legalDocumentId);
            }

            ack(record.getId());
        }
    }

    /**
     * 从消息记录中获取重试次数
     */
    private int getRetryCount(MapRecord<String, Object, Object> record) {
        Object retryCountObj = record.getValue().get(LegalDocumentStreamConstants.FIELD_RETRY_COUNT);

        if (retryCountObj == null) {
            return 0;
        }

        try {
            return Integer.parseInt(retryCountObj.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 确认消息已消费
     */
    private void ack(RecordId recordId) {
        stringRedisTemplate.opsForStream().acknowledge(
                LegalDocumentStreamConstants.LEGAL_DOCUMENT_ANALYZE_STREAM,
                LegalDocumentStreamConstants.LEGAL_DOCUMENT_ANALYZE_GROUP,
                recordId
        );
    }
}
