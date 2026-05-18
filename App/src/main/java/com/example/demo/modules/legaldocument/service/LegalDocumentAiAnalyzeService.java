package com.example.demo.modules.legaldocument.service;

import com.example.demo.common.exception.BusinessException;
import com.example.demo.modules.legaldocument.dto.LegalDocumentAiResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class LegalDocumentAiAnalyzeService {

    private static final Set<String> ALLOWED_RISK_LEVELS = Set.of("LOW", "MEDIUM", "HIGH");

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public LegalDocumentAiAnalyzeService(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public LegalDocumentAiResult analyze(String legalDocumentText) {
        if (legalDocumentText == null || legalDocumentText.isBlank()) {
            throw new BusinessException("法律文档内容为空，无法进行 AI 分析");
        }

        try {
            String content = chatClient.prompt()
                    .system(systemPrompt())
                    .user(buildPrompt(legalDocumentText))
                    .call()
                    .content();

            return parseResult(content);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("AI 法律文档分析失败：" + e.getMessage(), e);
        }
    }

    public Flux<String> streamAnalyze(String legalDocumentText) {
        if (legalDocumentText == null || legalDocumentText.isBlank()) {
            return Flux.error(new BusinessException("法律文档内容为空，无法进行 AI 分析"));
        }

        return chatClient.prompt()
                .system(systemPrompt())
                .user(buildPrompt(legalDocumentText))
                .stream()
                .content();
    }

    public LegalDocumentAiResult parseResult(String content) {
        try {
            LegalDocumentAiResult result = objectMapper.readValue(cleanJson(content), LegalDocumentAiResult.class);
            normalizeResult(result);
            validateResult(result);
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("AI 法律文档分析结果解析失败：" + e.getMessage(), e);
        }
    }

    private String systemPrompt() {
        return "你是一名资深法律文档审查顾问，擅长分析合同、协议、法律文书中的风险条款、权利义务和修改建议。";
    }

    private String buildPrompt(String documentText) {
        return """
                请分析下面这份法律文档或合同文本。
                请从以下维度进行分析：
                1. 文档整体摘要
                2. 综合安全评分：0 到 100 分，0 表示风险最高，100 表示风险最低，分数越高代表风险越小
                3. 风险等级：LOW、MEDIUM、HIGH
                4. 关键条款提取
                5. 风险条款识别
                6. 双方权利义务分析
                7. 修改建议
                8. 审查时需要重点追问或确认的问题

                请严格返回 JSON，不要返回 Markdown，不要返回解释文字。
                JSON 格式如下：
                {
                  "riskScore": 82,
                  "riskLevel": "MEDIUM",
                  "summary": "这是一份服务合作协议，主要约定了服务内容、付款方式、违约责任和保密义务。",
                  "keyClauses": [
                    "甲方应在验收完成后 10 个工作日内支付服务费用",
                    "乙方需对项目资料承担保密义务"
                  ],
                  "riskClauses": [
                    "违约责任条款较笼统，未明确违约金计算方式",
                    "验收标准不够具体，可能导致争议"
                  ],
                  "obligations": [
                    "甲方需按约支付费用并配合验收",
                    "乙方需按时交付服务成果并承担保密义务"
                  ],
                  "suggestions": [
                    "建议明确验收标准和验收期限",
                    "建议补充违约金计算方式",
                    "建议明确争议解决方式和管辖法院"
                  ],
                  "reviewQuestions": [
                    "合同中是否明确了交付成果的验收标准？",
                    "付款节点是否与交付进度匹配？",
                    "违约责任是否具备可执行性？"
                  ]
                }

                法律文档内容如下：
                %s
                """.formatted(documentText);
    }

    private void normalizeResult(LegalDocumentAiResult result) {
        if (result == null) {
            return;
        }

        if (result.getOverallRiskLevel() == null) {
            result.setOverallRiskLevel(result.getRiskLevel());
        }

        if (result.getRisks() == null) {
            result.setRisks(result.getRiskClauses());
        }

        if (result.getParties() == null && result.getObligations() != null) {
            result.setParties(extractPartiesFromObligations(result.getObligations()));
        }
    }

    private List<String> extractPartiesFromObligations(List<String> obligations) {
        List<String> parties = new ArrayList<>();

        for (String obligation : obligations) {
            if (obligation == null) {
                continue;
            }

            if (obligation.contains("甲方") && !parties.contains("甲方")) {
                parties.add("甲方");
            }

            if (obligation.contains("乙方") && !parties.contains("乙方")) {
                parties.add("乙方");
            }
        }

        return parties;
    }

    private void validateResult(LegalDocumentAiResult result) {
        if (result == null) {
            throw new BusinessException("AI 返回内容为空");
        }

        String riskLevel = result.getOverallRiskLevel();
        if (riskLevel == null || !ALLOWED_RISK_LEVELS.contains(riskLevel)) {
            throw new BusinessException("AI 返回的风险等级不合法：" + riskLevel);
        }
    }

    private String cleanJson(String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException("AI 返回内容为空");
        }

        String text = content.trim();

        if (text.startsWith("```json")) {
            text = text.substring(7);
        }

        if (text.startsWith("```")) {
            text = text.substring(3);
        }

        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }

        return text.trim();
    }
}
