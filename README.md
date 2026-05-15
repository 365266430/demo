# AI 法律文档分析系统

本项目是一个基于 **Spring Boot + React + MySQL + Redis Stream + Spring AI** 的法律文档分析应用。

当前核心模块是法律文档分析，支持文档上传、Apache Tika 文本解析、MySQL 持久化、Redis Stream 异步分析、SSE 真流式分析、失败重试、列表查询、详情查看、删除和重新分析。

所有源码文件建议统一使用 **UTF-8 无 BOM** 编码。

---

## 项目结构

```text
legal-document-analysis
├── App        # Spring Boot 后端
└── Frontend   # React + TypeScript + Vite 前端
```

后端法律文档模块包路径：

```text
com.example.demo.modules.legaldocument
```

前端法律文档页面目录：

```text
Frontend/src/pages/legalDocument
```

---

## 当前功能

```text
用户上传法律文档
  ->
后端接收文件并校验
  ->
Apache Tika 提取文本
  ->
保存法律文档记录到 MySQL
  ->
前端详情页通过 SSE 接收 AI 流式输出
  ->
AI 返回完整 JSON 后，后端解析并保存结构化分析结果
  ->
前端刷新并展示风险评分、风险等级、关键条款、风险条款、权利义务、修改建议和审查问题
```

保留的非流式异步能力：

```text
上传或重新分析
  ->
写入 Redis Stream
  ->
Consumer 异步消费
  ->
调用 AI
  ->
保存完整 JSON 到 MySQL
```

---

## 技术栈

- Java 17
- Spring Boot 3.5.13
- Spring AI 1.1.6
- Spring MVC
- Spring Data JPA
- Spring Data Redis
- MySQL
- Redis Stream
- Apache Tika
- React 19
- TypeScript
- Vite
- Axios
- Server-Sent Events

---

## 数据库

法律文档实体表名：

```text
legal_document
```

主要字段：

- `id`
- `original_filename`
- `content_type`
- `file_size`
- `content`
- `status`
- `score`
- `summary`
- `analysis_result`
- `error_message`
- `created_at`
- `updated_at`

状态枚举：

```text
PENDING
PROCESSING
COMPLETED
FAILED
```

---

## Redis Stream

Redis Stream 用于保留非流式异步分析能力。

```text
stream key: legal-document:analyze:stream
group:      legal-document:analyze:group
consumer:   legal-document:analyze:consumer
消息字段:   legalDocumentId, retryCount
```

最大失败重试次数：

```text
3
```

---

## AI 输出格式

当前 Prompt 要求 AI 严格返回 JSON，不返回 Markdown 或解释文字。

主要字段：

```json
{
  "riskScore": 65,
  "riskLevel": "MEDIUM",
  "summary": "文档整体摘要",
  "keyClauses": ["关键条款"],
  "riskClauses": ["风险条款"],
  "obligations": ["权利义务分析"],
  "suggestions": ["修改建议"],
  "reviewQuestions": ["审查问题"]
}
```

风险等级只允许：

```text
LOW
MEDIUM
HIGH
```

后端会兼容并规范化部分字段：

- `riskLevel` -> `overallRiskLevel`
- `riskClauses` -> `risks`
- 从 `obligations` 中尝试提取 `parties`

---

## 后端接口

基础路径：

```text
/api/legal-documents
```

接口列表：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/legal-documents` | 查询法律文档列表 |
| `GET` | `/api/legal-documents/{id}` | 查询法律文档详情 |
| `POST` | `/api/legal-documents/upload` | 上传文档，默认走 Redis Stream 异步分析 |
| `POST` | `/api/legal-documents/upload?stream=true` | 上传文档，前端详情页通过 SSE 流式分析 |
| `GET` | `/api/legal-documents/{id}/analysis-stream` | SSE 真流式分析 |
| `GET` | `/api/legal-documents/{id}/analysis-stream?reanalyze=true` | SSE 重新流式分析 |
| `POST` | `/api/legal-documents/{id}/reanalyze` | Redis Stream 重新分析 |
| `DELETE` | `/api/legal-documents/{id}` | 删除文档 |
| `POST` | `/api/legal-documents/test` | 创建测试文档 |

---

## SSE 流式事件

流式分析接口：

```text
GET /api/legal-documents/{id}/analysis-stream
```

事件类型：

| event | data | 说明 |
| --- | --- | --- |
| `status` | `PROCESSING` | 分析开始 |
| `chunk` | 文本片段 | AI 实时输出片段 |
| `done` | 完整 JSON | 分析完成，后端已保存结果 |
| `analysis-error` | 错误信息 | 分析失败 |

说明：

- 前端使用 `EventSource` 接收流式输出。
- 后端会在流结束后解析完整 JSON 并保存到 MySQL。
- 如果文档已经完成且没有要求重新分析，SSE 会直接返回 `done`。

---

## 配置

后端配置文件：

```text
App/src/main/resources/application.yml
```

默认端口：

```text
server.port=8082
```

默认数据库：

```text
jdbc:mysql://localhost:3306/legal_document_analysis
```

默认 Redis：

```text
localhost:26380
```

AI 配置位于：

```yaml
spring:
  ai:
    openai:
      api-key: your-api-key
      base-url: https://api.deepseek.com
      chat:
        options:
          model: deepseek-chat
          temperature: 0.2
```

---

## 启动

### 1. 启动依赖

请先确认以下服务可用：

- MySQL
- Redis
- DeepSeek/OpenAI-compatible API Key

### 2. 启动后端

```bash
cd App
mvn spring-boot:run
```

后端地址：

```text
http://localhost:8082
```

### 3. 启动前端

```bash
cd Frontend
npm install
npm run dev
```

前端默认地址：

```text
http://localhost:5173
```

Vite 已配置 `/api` 代理到：

```text
http://localhost:8082
```

---

## 测试与构建

后端测试：

```bash
cd App
mvn test
```

前端构建：

```bash
cd Frontend
npm run build
```

---

## 前端路由

```text
/legaldocuments
/legaldocuments/upload
/legaldocuments/:id
```

页面：

- 上传文档
- 分析记录
- 分析详情

详情页在 `PENDING` 或 `PROCESSING` 状态下会自动建立 SSE 连接，实时展示 AI 输出。

---

## 注意事项

- 当前项目没有新增 RAG、PDF 导出、语音、权限等功能。
- SSE 流式分析和 Redis Stream 异步分析并存。
- 前端默认上传使用 `stream=true`，因此由详情页 SSE 执行真实流式分析。
- 如果直接调用不带 `stream=true` 的上传接口，则会走 Redis Stream 异步分析。
- 源码中文请使用 UTF-8 无 BOM 编码。
