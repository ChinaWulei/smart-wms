package com.example.wms.service;

import com.example.wms.common.BizException;
import com.example.wms.config.AiProperties;
import com.example.wms.domain.AiReport;
import com.example.wms.repository.AiReportRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AiReportService {
    private final DashboardService dashboardService;
    private final AiTextClient aiTextClient;
    private final AiReportRepository reportRepository;
    private final ObjectMapper objectMapper;
    private final AiProperties aiProperties;

    public AiReportService(DashboardService dashboardService, AiTextClient aiTextClient,
                           AiReportRepository reportRepository, ObjectMapper objectMapper,
                           AiProperties aiProperties) {
        this.dashboardService = dashboardService;
        this.aiTextClient = aiTextClient;
        this.reportRepository = reportRepository;
        this.objectMapper = objectMapper;
        this.aiProperties = aiProperties;
    }

    public AiReport generate() {
        String json;
        try {
            json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dashboardService.structuredAiData());
        } catch (JsonProcessingException ex) {
            throw new BizException("结构化数据序列化失败");
        }
        String text;
        try {
            text = aiTextClient.analyze(json);
        } catch (RuntimeException ex) {
            text = "AI分析生成失败，核心库存业务未受影响。失败原因：" + ex.getMessage();
        }
        AiReport report = new AiReport();
        report.setTitle("AI仓储运营报告-" + DateTimeFormatter.ofPattern("yyyyMMddHHmm").format(LocalDateTime.now()));
        report.setStructuredDataJson(json);
        report.setAnalysisText(text);
        return reportRepository.save(report);
    }

    public List<AiReport> list() {
        return reportRepository.findAll();
    }

    public AiReport createFromAgent(String title, String analysisText, String structuredDataJson) {
        AiReport report = new AiReport();
        report.setTitle(title);
        report.setAnalysisText(analysisText);
        report.setStructuredDataJson(structuredDataJson);
        return reportRepository.save(report);
    }

    public byte[] pdf(Long id) {
        AiReport report = reportRepository.findById(id).orElseThrow(() -> new BizException("报告不存在"));
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();
            Font font = pdfFont();
            document.add(new Paragraph(report.getTitle(), font));
            document.add(new Paragraph(" "));
            for (String line : report.getAnalysisText().split("\\R")) {
                document.add(new Paragraph(line, font));
            }
            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new BizException("PDF生成失败：" + ex.getMessage());
        }
    }

    private Font pdfFont() {
        try {
            BaseFont baseFont = BaseFont.createFont(
                    aiProperties.getPdfFont(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            return new Font(baseFont, 11);
        } catch (Exception ex) {
            return new Font(Font.HELVETICA, 11);
        }
    }
}
