package com.deviance.engine;

import com.deviance.model.Anomaly;

/**
 * Classifies each anomaly into Critical / Moderate / Low.
 * Also generates an actionable recommendation.
 */
public class RiskClassifier {

    private static final double CRITICAL_THRESHOLD = 60.0;
    private static final double MODERATE_THRESHOLD = 30.0;

    public void classify(Anomaly anomaly) {
        double score = anomaly.getDevianceScore();

        if (score >= CRITICAL_THRESHOLD) {
            anomaly.setRiskLevel("CRITICAL");
            anomaly.setRecommendation(buildRecommendation(anomaly, "CRITICAL"));
        } else if (score >= MODERATE_THRESHOLD) {
            anomaly.setRiskLevel("MODERATE");
            anomaly.setRecommendation(buildRecommendation(anomaly, "MODERATE"));
        } else {
            anomaly.setRiskLevel("LOW");
            anomaly.setRecommendation(buildRecommendation(anomaly, "LOW"));
        }
    }

    private String buildRecommendation(Anomaly anomaly, String level) {
        String source = anomaly.getSource();
        int count = anomaly.getOccurrenceCount();
        long days = anomaly.getDaysSinceLastAction();

        return switch (level) {
            case "CRITICAL" -> String.format(
                    "IMMEDIATE ACTION REQUIRED: This pattern has appeared %d times " +
                            "and has not been properly resolved in %d days. " +
                            "Schedule a dedicated fix sprint within 2 weeks. " +
                            "Conduct a root cause analysis and document findings.",
                    count, days);
            case "MODERATE" -> String.format(
                    "ACTION RECOMMENDED: This pattern has appeared %d times across %s data. " +
                            "Add to next sprint backlog. " +
                            "Assign an owner and set a resolution deadline.",
                    count, source);
            default -> String.format(
                    "MONITOR: This pattern (%d occurrences) is low priority but should be tracked. " +
                            "Add to technical debt backlog.",
                    count);
        };
    }
}