package com.example.demo.common.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    /**
     * 健康检查接口，用于确认后端服务是否正常运行。
     *
     * @return 服务运行状态说明
     */
    @GetMapping("/api/health")
    public String health() {
        return "legal-document-analysis backend is running";
    }
}
