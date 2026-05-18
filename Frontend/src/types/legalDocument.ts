export type LegalDocumentStatus = "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED";

export type AnalyzeStatus = LegalDocumentStatus;

export type RagIndexStatus = "NOT_INDEXED" | "INDEXING" | "INDEXED" | "FAILED";

export type RiskLevel = "LOW" | "MEDIUM" | "HIGH";

export interface LegalDocumentUploadResponse {
  legalDocumentId: number;
  status: AnalyzeStatus;
  originalFilename: string;
}

export interface LegalDocumentListItem {
  id: number;
  originalFilename: string;
  contentType: string;
  fileSize: number;
  status: AnalyzeStatus;
  ragStatus?: RagIndexStatus | null;
  overallRiskLevel?: RiskLevel | null;
  createdAt: string;
  updatedAt: string;
}

export interface RagSourceChunk {
  documentId: string;
  chunkId: string;
  chunkIndex: number;
  content: string;
  score?: number | null;
}

export interface RagAskResponse {
  documentId: string;
  question: string;
  answer: string;
  sources: RagSourceChunk[];
}

export interface RagQaRecord {
  id: number;
  legalDocumentId: number;
  question: string;
  answer: string;
  sources: RagSourceChunk[];
  createdAt: string;
}

export interface LegalAnalysisResult {
  documentType: string;
  riskScore?: number | null;
  riskLevel?: RiskLevel | null;
  summary: string;
  parties: string[];
  keyClauses: string[];
  risks: string[];
  obligations?: string[];
  suggestions: string[];
  reviewQuestions?: string[];
  overallRiskLevel: RiskLevel;
}

export interface LegalDocumentDetail {
  id: number;
  originalFilename: string;
  contentType: string;
  fileSize: number;
  status: AnalyzeStatus;
  ragStatus?: RagIndexStatus | null;
  ragChunkCount?: number | null;
  ragErrorMessage?: string | null;
  ragIndexedAt?: string | null;
  qaRecords?: RagQaRecord[];
  score?: number | null;
  summary?: string | null;
  analysisResult?: LegalAnalysisResult | null;
  errorMessage?: string | null;
  createdAt: string;
  updatedAt: string;
}
