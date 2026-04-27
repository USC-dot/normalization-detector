package com.deviance;

import com.deviance.engine.PatternEngine;
import com.deviance.model.Anomaly;
import com.deviance.model.DevianceReport;
import com.deviance.report.ReportGenerator;

/**
 * Entry point for the Normalization of Deviance Detector.
 */
public class Main {

    public static void main(String[] args) throws Exception {

        // Paths — change these to point at your real data
        String repoPath    = "data/sample_commits";
        String logPath     = "data/sample_logs/server.log";
        String bugCsvPath  = "data/sample_bugs/bugs.csv";
        String outputPath  = "output/report.html";

        System.out.println("=".repeat(60));
        System.out.println("  NORMALIZATION OF DEVIANCE DETECTOR");
        System.out.println("=".repeat(60));

        // Run the full analysis
        PatternEngine engine = new PatternEngine();
        DevianceReport report = engine.analyze(repoPath, logPath, bugCsvPath);

        // Print summary to console
        System.out.println("\n📊 DEVIANCE SUMMARY");
        System.out.println("-".repeat(40));
        System.out.println("Total anomalies detected : " + report.getTotalCount());
        System.out.println("🚨 Critical               : " + report.getCritical().size());
        System.out.println("⚠️  Moderate               : " + report.getModerate().size());
        System.out.println("📋 Low                    : " + report.getLow().size());
        System.out.println();

        // Print top 5 highest scoring anomalies
        System.out.println("🔥 TOP ANOMALIES BY DEVIANCE SCORE:");
        System.out.println("-".repeat(40));
        report.getAllAnomalies().stream().limit(5).forEach(a ->
                System.out.printf("  [%.1f] %s | %s%n",
                        a.getDevianceScore(),
                        a.getRiskLevel(),
                        a.getPattern().substring(0, Math.min(60, a.getPattern().length())))
        );

        // Generate HTML report
        System.out.println("\n📄 Generating HTML report...");
        ReportGenerator generator = new ReportGenerator();
        generator.generateHtml(report, outputPath);

        System.out.println("\n✅ Done. Open output/report.html in your browser.");
        System.out.println("=".repeat(60));
    }
}