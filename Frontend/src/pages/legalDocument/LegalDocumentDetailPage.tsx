import { useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { askLegalDocument, getLegalDocumentDetail, ingestLegalDocument } from "../../api/legalDocument";
import type { LegalDocumentDetail, LegalDocumentStatus, RagIndexStatus, RiskLevel } from "../../types/legalDocument";

export default function LegalDocumentDetailPage() {
  const navigate = useNavigate();
  const params = useParams();
  const id = Number(params.id);

  const eventSourceRef = useRef<EventSource | null>(null);

  const [detail, setDetail] = useState<LegalDocumentDetail | null>(null);
  const [loading, setLoading] = useState(false);
  const [streaming, setStreaming] = useState(false);
  const [reanalyzing, setReanalyzing] = useState(false);
  const [streamText, setStreamText] = useState("");
  const [message, setMessage] = useState("");
  const [question, setQuestion] = useState("");
  const [asking, setAsking] = useState(false);
  const [indexing, setIndexing] = useState(false);

  async function loadDetail() {
    if (!id) return;

    try {
      const data = await getLegalDocumentDetail(id);
      setDetail(data);
    } catch (error: any) {
      setMessage(error.message || "加载详情失败");
    }
  }

  function closeStream() {
    eventSourceRef.current?.close();
    eventSourceRef.current = null;
  }

  function startStream(reanalyze = false) {
    if (!id) return;

    closeStream();
    setStreaming(true);
    setReanalyzing(reanalyze);
    setMessage("");
    setStreamText("");

    const query = reanalyze ? "?reanalyze=true" : "";
    const eventSource = new EventSource(`/api/legal-documents/${id}/analysis-stream${query}`);
    eventSourceRef.current = eventSource;

    eventSource.addEventListener("chunk", (event) => {
      setStreamText((previous) => previous + event.data);
    });

    eventSource.addEventListener("done", async () => {
      closeStream();
      setStreaming(false);
      setReanalyzing(false);
      await loadDetail();
    });

    eventSource.addEventListener("analysis-error", (event) => {
      closeStream();
      setStreaming(false);
      setReanalyzing(false);
      setMessage((event as MessageEvent).data || "流式分析失败");
      loadDetail();
    });

    eventSource.onerror = () => {
      closeStream();
      setStreaming(false);
      setReanalyzing(false);
      setMessage("流式连接已断开");
      loadDetail();
    };
  }

  async function handleReanalyze() {
    startStream(true);
  }

  async function handleBuildRagIndex() {
    if (!id) return;

    try {
      setIndexing(true);
      setMessage("");
      await ingestLegalDocument(id);
      await loadDetail();
    } catch (error: any) {
      setMessage(error.message || "建立 RAG 索引失败");
      await loadDetail();
    } finally {
      setIndexing(false);
    }
  }

  async function handleAsk() {
    const trimmed = question.trim();
    if (!trimmed) {
      setMessage("请输入要咨询的问题");
      return;
    }

    try {
      setAsking(true);
      setMessage("");
      await askLegalDocument(id, trimmed);
      setQuestion("");
      await loadDetail();
    } catch (error: any) {
      setMessage(error.message || "RAG 问答失败");
    } finally {
      setAsking(false);
    }
  }

  useEffect(() => {
    async function init() {
      setLoading(true);
      await loadDetail();
      setLoading(false);
    }

    init();

    return () => {
      closeStream();
    };
  }, [id]);

  useEffect(() => {
    if (!detail || streaming || eventSourceRef.current) return;
    if (detail.status === "PENDING" || detail.status === "PROCESSING") {
      startStream(false);
    }
  }, [detail?.status, streaming, id]);

  const result = detail?.analysisResult;
  const riskScore = result?.riskScore ?? detail?.score ?? null;

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>法律文档分析详情</h1>
          <p className="sub-title">查看 AI 对法律文档的风险评分、关键条款、风险条款和修改建议。</p>
        </div>

        <div className="actions">
          <button className="secondary-button" onClick={() => navigate("/legaldocuments")}>
            返回列表
          </button>

          <button className="primary-button" onClick={handleReanalyze} disabled={streaming || reanalyzing}>
            {reanalyzing ? "提交中..." : "重新分析"}
          </button>
        </div>
      </div>

      {loading && <div className="card message">加载中...</div>}
      {message && <div className="card message error">{message}</div>}

      {detail && (
        <>
          <div className="card">
            <div className="detail-grid">
              <Info label="文件名" value={detail.originalFilename} />
              <Info label="文件类型" value={formatContentType(detail.contentType)} />
              <Info label="文件大小" value={formatFileSize(detail.fileSize)} />
              <Info label="分析状态" value={<StatusBadge status={streaming ? "PROCESSING" : detail.status} />} />
              <Info label="RAG 索引" value={<RagStatusBadge status={indexing ? "INDEXING" : detail.ragStatus} />} />
              <Info label="索引切片" value={detail.ragChunkCount ?? "-"} />
              <Info label="风险评分" value={riskScore ?? "-"} />
              <Info label="风险等级" value={<RiskLevelBadge level={result?.overallRiskLevel} />} />
              <Info label="更新时间" value={formatDate(detail.updatedAt)} />
            </div>
          </div>

          <div className="card rag-card">
            <div className="rag-header">
              <div>
                <h2>文档问答</h2>
                <p className="sub-title">基于当前文档索引检索相关片段，再生成回答。</p>
              </div>
              {detail.ragStatus !== "INDEXED" && (
                <button className="secondary-button" onClick={handleBuildRagIndex} disabled={indexing}>
                  {indexing ? "索引中..." : "重新建立索引"}
                </button>
              )}
            </div>

            {detail.ragStatus === "FAILED" && (
              <div className="message error">{detail.ragErrorMessage || "RAG 索引建立失败"}</div>
            )}

            <div className="qa-composer">
              <textarea
                value={question}
                onChange={(event) => setQuestion(event.target.value)}
                placeholder={detail.ragStatus === "INDEXED" ? "输入你想基于这份文档咨询的问题" : "索引完成后即可提问"}
                disabled={detail.ragStatus !== "INDEXED" || asking}
              />
              <button className="primary-button" onClick={handleAsk} disabled={detail.ragStatus !== "INDEXED" || asking}>
                {asking ? "回答中..." : "发送问题"}
              </button>
            </div>

            {!detail.qaRecords || detail.qaRecords.length === 0 ? (
              <p className="muted">暂无问答记录</p>
            ) : (
              <div className="qa-list">
                {detail.qaRecords.map((record) => (
                  <div className="qa-item" key={record.id}>
                    <div className="qa-question">问：{record.question}</div>
                    <div className="qa-answer">{record.answer}</div>
                    {record.sources?.length > 0 && (
                      <details className="qa-sources">
                        <summary>查看引用片段</summary>
                        {record.sources.map((source) => (
                          <div className="source-item" key={source.chunkId}>
                            <div className="source-title">片段 {source.chunkIndex}</div>
                            <p>{source.content}</p>
                          </div>
                        ))}
                      </details>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>

          {streaming && (
            <div className="card streaming-card">
              <div className="analyzing">
                <div className="spinner" />
                <div>
                  <h3>AI 正在流式分析</h3>
                  <p>模型输出会实时显示，完成后自动保存为结构化分析结果。</p>
                </div>
              </div>

              <pre className="stream-output">{streamText || "正在等待模型返回内容..."}</pre>
            </div>
          )}

          {detail.status === "FAILED" && !streaming && (
            <div className="card failed-card">
              <h3>分析失败</h3>
              <p>{detail.errorMessage || "未知错误"}</p>
              <button className="primary-button" onClick={handleReanalyze}>
                重新分析
              </button>
            </div>
          )}

          {detail.status === "COMPLETED" && result && !streaming && (
            <div className="report">
              <div className="card score-card">
                <div>
                  <h2>风险概览</h2>
                  <p>{result.summary || detail.summary || "暂无内容"}</p>
                  <p className="risk-level">风险等级：{formatRiskLevel(result.overallRiskLevel)}</p>
                </div>

                <div className={`score-circle ${getRiskLevelClass(result.overallRiskLevel)}`}>{riskScore ?? "-"}</div>
              </div>

              <ReportSection title="关键条款" items={result.keyClauses} />
              <ReportSection title="风险条款" items={result.risks} />
              <ReportSection title="权利义务分析" items={result.obligations} />
              <ReportSection title="修改建议" items={result.suggestions} />
              <ReportSection title="审查问题" items={result.reviewQuestions} />
            </div>
          )}
        </>
      )}
    </div>
  );
}

function Info({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="info-item">
      <div className="info-label">{label}</div>
      <div className="info-value">{value}</div>
    </div>
  );
}

function ReportSection({ title, items }: { title: string; items?: string[] }) {
  return (
    <div className="card report-section">
      <h2>{title}</h2>

      {!items || items.length === 0 ? (
        <p className="muted">暂无内容</p>
      ) : (
        <ul>
          {items.map((item, index) => (
            <li key={index}>{item}</li>
          ))}
        </ul>
      )}
    </div>
  );
}

function StatusBadge({ status }: { status: LegalDocumentStatus }) {
  const textMap: Record<LegalDocumentStatus, string> = {
    PENDING: "等待分析",
    PROCESSING: "分析中",
    COMPLETED: "已完成",
    FAILED: "失败",
  };

  return <span className={`status status-${status.toLowerCase()}`}>{textMap[status]}</span>;
}

function RagStatusBadge({ status }: { status?: RagIndexStatus | null }) {
  const value = status || "NOT_INDEXED";
  const textMap: Record<RagIndexStatus, string> = {
    NOT_INDEXED: "未索引",
    INDEXING: "索引中",
    INDEXED: "已索引",
    FAILED: "失败",
  };

  return <span className={`status rag-${value.toLowerCase().replace("_", "-")}`}>{textMap[value]}</span>;
}

function RiskLevelBadge({ level }: { level?: RiskLevel }) {
  if (!level) return <span>-</span>;

  return <span className={`risk-badge risk-${level.toLowerCase()}`}>{formatRiskLevel(level)}</span>;
}

function formatFileSize(size: number) {
  if (!size) return "-";
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(2)} KB`;
  return `${(size / 1024 / 1024).toFixed(2)} MB`;
}

function formatDate(value: string) {
  if (!value) return "-";
  return value.replace("T", " ").slice(0, 19);
}

function formatContentType(contentType?: string) {
  if (!contentType) return "-";

  const lower = contentType.toLowerCase();
  if (lower.includes("pdf")) return "PDF 文档";
  if (lower.includes("wordprocessingml") || lower.includes("docx")) return "Word 文档";
  if (lower.includes("msword") || lower.includes("doc")) return "Word 文档";
  if (lower.includes("text")) return "文本文件";

  return contentType;
}

function formatRiskLevel(level?: RiskLevel) {
  if (!level) return "-";

  const map: Record<RiskLevel, string> = {
    LOW: "低风险",
    MEDIUM: "中风险",
    HIGH: "高风险",
  };

  return map[level];
}

function getRiskLevelClass(level?: RiskLevel) {
  if (level === "LOW") return "score-low";
  if (level === "MEDIUM") return "score-medium";
  if (level === "HIGH") return "score-high";
  return "";
}
