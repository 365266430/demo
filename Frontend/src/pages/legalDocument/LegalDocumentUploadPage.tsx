import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { uploadLegalDocument } from "../../api/legalDocument";

export default function LegalDocumentUploadPage() {
  const navigate = useNavigate();

  const [file, setFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");

  async function handleUpload() {
    if (!file) {
      setMessage("请先选择一个法律文档文件");
      return;
    }

    try {
      setLoading(true);
      setMessage("");

      const result = await uploadLegalDocument(file);

      setMessage(`上传成功，法律文档 ID：${result.legalDocumentId}`);
      navigate(`/legaldocuments/${result.legalDocumentId}`);
    } catch (error: any) {
      setMessage(error.message || "上传失败");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="page">
      <div className="card upload-card">
        <h1>AI 法律文档分析系统</h1>
        <p className="sub-title">
          上传 PDF、DOC、DOCX、TXT 合同或法律文书，系统会自动提取文本并异步生成法律文档分析结果。
        </p>

        <div className="upload-box">
          <input
            type="file"
            accept=".pdf,.doc,.docx,.txt"
            onChange={(event) => {
              const selected = event.target.files?.[0];
              setFile(selected || null);
              setMessage("");
            }}
          />

          {file && (
            <div className="file-info">
              <div>文件名：{file.name}</div>
              <div>文件大小：{formatFileSize(file.size)}</div>
            </div>
          )}

          <button className="primary-button" onClick={handleUpload} disabled={loading}>
            {loading ? "上传中..." : "上传并开始法律文档分析"}
          </button>

          {message && <div className="message">{message}</div>}
        </div>
      </div>
    </div>
  );
}

function formatFileSize(size: number) {
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(2)} KB`;
  return `${(size / 1024 / 1024).toFixed(2)} MB`;
}
