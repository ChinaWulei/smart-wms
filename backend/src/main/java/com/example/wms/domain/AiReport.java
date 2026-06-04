package com.example.wms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "ai_reports")
public class AiReport extends BaseEntity {
    private String title;
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String structuredDataJson;
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String analysisText;
    private String pdfUrl;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getStructuredDataJson() { return structuredDataJson; }
    public void setStructuredDataJson(String structuredDataJson) { this.structuredDataJson = structuredDataJson; }
    public String getAnalysisText() { return analysisText; }
    public void setAnalysisText(String analysisText) { this.analysisText = analysisText; }
    public String getPdfUrl() { return pdfUrl; }
    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }
}
