import request from "./request";
import type {
  LegalDocumentDetail,
  LegalDocumentListItem,
  LegalDocumentUploadResponse,
} from "../types/legalDocument";

const API_PREFIX = "/legal-documents";

export function uploadLegalDocument(file: File) {
  const formData = new FormData();
  formData.append("file", file);

  return request.post<any, LegalDocumentUploadResponse>(`${API_PREFIX}/upload?stream=true`, formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });
}

export function getLegalDocumentList() {
  return request.get<any, LegalDocumentListItem[]>(API_PREFIX);
}

export function getLegalDocumentDetail(id: number) {
  return request.get<any, LegalDocumentDetail>(`${API_PREFIX}/${id}`);
}

export function deleteLegalDocument(id: number) {
  return request.delete<any, null>(`${API_PREFIX}/${id}`);
}

export function reanalyzeLegalDocument(id: number) {
  return request.post<any, LegalDocumentUploadResponse>(`${API_PREFIX}/${id}/reanalyze`);
}
