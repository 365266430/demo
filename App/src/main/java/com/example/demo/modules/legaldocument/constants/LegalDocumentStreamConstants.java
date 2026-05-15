package com.example.demo.modules.legaldocument.constants;

public class LegalDocumentStreamConstants {

    public static final String LEGAL_DOCUMENT_ANALYZE_STREAM = "legal-document:analyze:stream";

    public static final String LEGAL_DOCUMENT_ANALYZE_GROUP = "legal-document:analyze:group";

    public static final String LEGAL_DOCUMENT_ANALYZE_CONSUMER = "legal-document:analyze:consumer";

    public static final String FIELD_LEGAL_DOCUMENT_ID = "legalDocumentId";

    public static final String FIELD_RETRY_COUNT = "retryCount";

    public static final int MAX_RETRY_COUNT = 3;

    private LegalDocumentStreamConstants() {
    }
}
