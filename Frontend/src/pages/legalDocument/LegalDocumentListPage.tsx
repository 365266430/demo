import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { deleteLegalDocument, getLegalDocumentList } from "../../api/legalDocument";
import type { LegalDocumentListItem, LegalDocumentStatus, RiskLevel } from "../../types/legalDocument";

export default function LegalDocumentListPage() {
  const navigate = useNavigate();

  const [list, setList] = useState<LegalDocumentListItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");

  async function loadList() {
    try {
      setLoading(true);
      const data = await getLegalDocumentList();
      setList(data);
    } catch (error: any) {
      setMessage(error.message || "加载失败");
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete(id: number) {
    const confirmed = window.confirm("确定要删除这份法律文档吗？");
    if (!confirmed) return;

    try {
      await deleteLegalDocument(id);
      await loadList();
    } catch (error: any) {
      alert(error.message || "删除失败");
    }
  }

  useEffect(() => {
    loadList();
  }, []);

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>法律文档分析记录</h1>
          <p className="sub-title">查看已上传法律文档的分析状态、风险等级和报告结果。</p>
        </div>

        <button className="primary-button" onClick={() => navigate("/legaldocuments/upload")}>
          上传新文档
        </button>
      </div>

      <div className="card">
        {loading && <div className="message">加载中...</div>}
        {message && <div className="message error">{message}</div>}

        {!loading && list.length === 0 && (
          <div className="empty">暂无法律文档记录，请先上传一份文档。</div>
        )}

        {list.length > 0 && (
          <table className="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>文件名</th>
                <th>文件类型</th>
                <th>文件大小</th>
                <th>状态</th>
                <th>整体风险等级</th>
                <th>更新时间</th>
                <th>操作</th>
              </tr>
            </thead>

            <tbody>
              {list.map((item) => (
                <tr key={item.id}>
                  <td>{item.id}</td>
                  <td className="filename">{item.originalFilename}</td>
                  <td>{formatContentType(item.contentType)}</td>
                  <td>{formatFileSize(item.fileSize)}</td>
                  <td>
                    <StatusBadge status={item.status} />
                  </td>
                  <td>
                    <RiskLevelBadge level={item.overallRiskLevel || undefined} />
                  </td>
                  <td>{formatDate(item.updatedAt)}</td>
                  <td>
                    <button className="link-button" onClick={() => navigate(`/legaldocuments/${item.id}`)}>
                      查看
                    </button>

                    <button className="danger-button" onClick={() => handleDelete(item.id)}>
                      删除
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
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

function RiskLevelBadge({ level }: { level?: RiskLevel }) {
  if (!level) return <span>-</span>;

  const textMap: Record<RiskLevel, string> = {
    LOW: "低风险",
    MEDIUM: "中风险",
    HIGH: "高风险",
  };

  return <span className={`risk-badge risk-${level.toLowerCase()}`}>{textMap[level]}</span>;
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
