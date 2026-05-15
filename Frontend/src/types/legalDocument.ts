export type LegalDocumentStatus = "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED";

export type AnalyzeStatus = LegalDocumentStatus;

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
  overallRiskLevel?: RiskLevel | null;
  createdAt: string;
  updatedAt: string;
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
  score?: number | null;
  summary?: string | null;
  analysisResult?: LegalAnalysisResult | null;
  errorMessage?: string | null;
  createdAt: string;
  updatedAt: string;
}
