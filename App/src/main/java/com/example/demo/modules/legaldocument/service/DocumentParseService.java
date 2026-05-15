package com.example.demo.modules.legaldocument.service;

import com.example.demo.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Slf4j
@Service
public class DocumentParseService {

    private final Tika tika = new Tika();

    /**
     * 解析上传文件，提取文本内容
     */
    public String parse(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            String text = tika.parseToString(inputStream);

            if (text == null || text.isBlank()) {
                throw new BusinessException("无法从文件中提取文本内容，请确认文件不是扫描版 PDF");
            }

            return cleanText(text);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("文档解析失败，文件名：{}", file.getOriginalFilename(), e);
            throw new BusinessException("文档解析失败：" + e.getMessage(), e);
        }
    }

    /**
     * 检测上传文件的 MIME 类型
     */
    public String detectContentType(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return tika.detect(inputStream, file.getOriginalFilename());
        } catch (Exception e) {
            log.error("文件类型检测失败，文件名：{}", file.getOriginalFilename(), e);
            throw new BusinessException("文件类型检测失败：" + e.getMessage(), e);
        }
    }

    /**
     * 清洗提取的文本（去除空字符、规范化空白）
     */
    private String cleanText(String text) {
        return text
                .replace("\u0000", "")
                .replaceAll("[\\t\\x0B\\f\\r]+", " ")
                .replaceAll(" +", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
