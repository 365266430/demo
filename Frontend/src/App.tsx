import { BrowserRouter, Link, Navigate, Route, Routes } from "react-router-dom";
import LegalDocumentDetailPage from "./pages/legalDocument/LegalDocumentDetailPage";
import LegalDocumentListPage from "./pages/legalDocument/LegalDocumentListPage";
import LegalDocumentUploadPage from "./pages/legalDocument/LegalDocumentUploadPage";

export default function App() {
  return (
    <BrowserRouter>
      <div className="app">
        <aside className="sidebar">
          <div className="logo">AI 法律文档分析</div>
          <nav>
            <Link to="/legaldocuments/upload">上传文档</Link>
            <Link to="/legaldocuments">分析记录</Link>
          </nav>
        </aside>

        <main className="main">
          <Routes>
            <Route path="/" element={<Navigate to="/legaldocuments/upload" replace />} />
            <Route path="/upload" element={<Navigate to="/legaldocuments/upload" replace />} />
            <Route path="/legaldocuments/upload" element={<LegalDocumentUploadPage />} />
            <Route path="/legaldocuments" element={<LegalDocumentListPage />} />
            <Route path="/legaldocuments/:id" element={<LegalDocumentDetailPage />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}
