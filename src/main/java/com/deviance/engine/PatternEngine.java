package com.deviance.engine;

import com.deviance.model.Anomaly;
import com.deviance.model.DevianceReport;
import com.deviance.scanner.BugTrackerScanner;
import com.deviance.scanner.GitScanner;
import com.deviance.scanner.LogScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Orchestrates the full detection pipeline:
 * 1. Run all scanners
 * 2. Score each anomaly
 * 3. Classify each anomaly
 * 4. Build the report
 */
public class PatternEngine {

    private static final Logger logger = LoggerFactory.getLogger(PatternEngine.class);

    private final DevianceScorer scorer = new DevianceScorer();
    private final RiskClassifier classifier = new RiskClassifier();

    public DevianceReport analyze(String repoPath, String logPath, String bugCsvPath) {
        DevianceReport report = new DevianceReport(repoPath);

        // Step 1: Run all scanners
        logger.info("Running Log Scanner...");
        LogScanner logScanner = new LogScanner(logPath);
        List<Anomaly> logAnomalies = logScanner.scan();

        logger.info("Running Git Scanner...");
        GitScanner gitScanner = new GitScanner(repoPath);
        List<Anomaly> gitAnomalies = gitScanner.scan();

        logger.info("Running Bug Tracker Scanner...");
        BugTrackerScanner bugScanner = new BugTrackerScanner(bugCsvPath);
        List<Anomaly> bugAnomalies = bugScanner.scan();

        // Step 2: Score and classify all anomalies
        for (Anomaly anomaly : logAnomalies) {
            scorer.score(anomaly);
            classifier.classify(anomaly);
            report.addAnomaly(anomaly);
        }

        for (Anomaly anomaly : gitAnomalies) {
            scorer.score(anomaly);
            classifier.classify(anomaly);
            report.addAnomaly(anomaly);
        }

        for (Anomaly anomaly : bugAnomalies) {
            scorer.score(anomaly);
            classifier.classify(anomaly);
            report.addAnomaly(anomaly);
        }

        logger.info("Analysis complete. Total anomalies: {}", report.getTotalCount());
        return report;
    }
}