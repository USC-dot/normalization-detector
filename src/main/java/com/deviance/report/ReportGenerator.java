package com.deviance.report;

import com.deviance.model.Anomaly;
import com.deviance.model.DevianceReport;

import java.io.*;
import java.nio.file.*;

/**
 * Generates a human-readable HTML report from the DevianceReport.
 */
public class ReportGenerator {

    public void generateHtml(DevianceReport report, String outputPath) throws IOException {
        StringBuilder html = new StringBuilder();

        html.append("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Normalization of Deviance Report</title>
                <style>
                    body { font-family: 'IBM Plex Mono', monospace; background: #0a0a0a; color: #e0e0e0; padding: 40px; }
                    h1 { color: #ffffff; font-size: 24px; border-bottom: 1px solid #333; padding-bottom: 16px; }
                    .summary { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin: 24px 0; }
                    .card { background: #111; border: 1px solid #222; padding: 20px; border-radius: 4px; }
                    .card .num { font-size: 36px; font-weight: bold; }
                    .card .label { font-size: 11px; color: #666; margin-top: 4px; letter-spacing: 0.1em; }
                    .critical .num { color: #e05252; }
                    .moderate .num { color: #d4891a; }
                    .low .num { color: #4caf7d; }
                    .anomaly { background: #111; border: 1px solid #222; margin: 12px 0; padding: 20px; border-radius: 4px; }
                    .anomaly-header { display: flex; justify-content: space-between; margin-bottom: 12px; }
                    .pattern { font-size: 14px; font-weight: bold; color: #fff; }
                    .badge { padding: 3px 10px; border-radius: 2px; font-size: 10px; font-weight: bold; }
                    .badge.CRITICAL { background: rgba(224,82,82,0.2); color: #e05252; border: 1px solid rgba(224,82,82,0.3); }
                    .badge.MODERATE { background: rgba(212,137,26,0.2); color: #d4891a; border: 1px solid rgba(212,137,26,0.3); }
                    .badge.LOW { background: rgba(76,175,125,0.2); color: #4caf7d; border: 1px solid rgba(76,175,125,0.3); }
                    .meta { font-size: 11px; color: #666; margin: 8px 0; }
                    .evidence { background: #0d0d0d; padding: 10px; margin: 8px 0; font-size: 11px; color: #888; border-left: 2px solid #333; }
                    .recommendation { margin-top: 12px; font-size: 12px; color: #aaa; padding: 10px; background: #0d0d0d; border-left: 3px solid #444; }
                    .score { font-size: 11px; color: #555; }
                    section { margin: 32px 0; }
                    h2 { font-size: 13px; letter-spacing: 0.1em; color: #666; text-transform: uppercase; margin-bottom: 16px; }
                </style>
            </head>
            <body>
            """);

        html.append("<h1>🔮 Normalization of Deviance Report</h1>");
        html.append("<p style='color:#555;font-size:12px;'>Generated: ").append(report.getGeneratedAt()).append("</p>");

        // Summary cards
        html.append("<div class='summary'>");
        html.append(card("TOTAL ANOMALIES", String.valueOf(report.getTotalCount()), ""));
        html.append(card("CRITICAL", String.valueOf(report.getCritical().size()), "critical"));
        html.append(card("MODERATE", String.valueOf(report.getModerate().size()), "moderate"));
        html.append(card("LOW", String.valueOf(report.getLow().size()), "low"));
        html.append("</div>");

        // Anomalies by section
        appendSection(html, "🚨 CRITICAL — Immediate Action Required", report.getCritical());
        appendSection(html, "⚠️ MODERATE — Action Recommended", report.getModerate());
        appendSection(html, "📋 LOW — Monitor", report.getLow());

        html.append("</body></html>");

        Files.writeString(Path.of(outputPath), html.toString());
        System.out.println("Report generated: " + outputPath);
    }

    private String card(String label, String value, String cssClass) {
        return String.format(
                "<div class='card %s'><div class='num'>%s</div><div class='label'>%s</div></div>",
                cssClass, value, label);
    }

    private void appendSection(StringBuilder html, String title, java.util.List<Anomaly> anomalies) {
        if (anomalies.isEmpty()) return;
        html.append("<section><h2>").append(title).append("</h2>");
        for (Anomaly a : anomalies) {
            html.append("<div class='anomaly'>");
            html.append("<div class='anomaly-header'>");
            html.append("<span class='pattern'>").append(escapeHtml(a.getPattern())).append("</span>");
            html.append("<span class='badge ").append(a.getRiskLevel()).append("'>").append(a.getRiskLevel()).append("</span>");
            html.append("</div>");
            html.append("<div class='meta'>Source: ").append(a.getSource())
                    .append(" | Occurrences: ").append(a.getOccurrenceCount())
                    .append(" | Days unresolved: ").append(a.getDaysSinceLastAction())
                    .append(" | Score: <span class='score'>").append(a.getDevianceScore()).append("</span></div>");

            if (!a.getEvidence().isEmpty()) {
                html.append("<div class='evidence'>");
                a.getEvidence().forEach(e -> html.append(escapeHtml(e)).append("<br>"));
                html.append("</div>");
            }

            if (a.getRecommendation() != null) {
                html.append("<div class='recommendation'>").append(escapeHtml(a.getRecommendation())).append("</div>");
            }
            html.append("</div>");
        }
        html.append("</section>");
    }

    private String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}