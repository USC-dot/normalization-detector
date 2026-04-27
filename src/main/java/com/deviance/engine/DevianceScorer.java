package com.deviance.engine;

import com.deviance.model.Anomaly;

/**
 * Calculates the Deviance Score for each anomaly.
 *
 * Formula:
 * Score = (occurrenceWeight * occurrenceCount)
 *       + (timeWeight * daysSinceLastAction)
 *       + (sourceWeight based on source)
 *
 * Normalized to 0-100 scale.
 */
public class DevianceScorer {

    // How much each factor contributes to the score
    private static final double OCCURRENCE_WEIGHT = 2.0;
    private static final double TIME_WEIGHT = 0.3;
    private static final double MAX_SCORE = 100.0;

    public void score(Anomaly anomaly) {
        double occurrenceScore = anomaly.getOccurrenceCount() * OCCURRENCE_WEIGHT;
        double timeScore = anomaly.getDaysSinceLastAction() * TIME_WEIGHT;
        double sourceBonus = getSourceBonus(anomaly.getSource());

        double rawScore = occurrenceScore + timeScore + sourceBonus;

        // Cap at 100
        double finalScore = Math.min(rawScore, MAX_SCORE);

        anomaly.setDevianceScore(Math.round(finalScore * 100.0) / 100.0);
    }

    /**
     * Some sources are more reliable signals than others.
     * Bug tracker data is the strongest signal (someone actually filed a ticket).
     * Git history is medium signal.
     * Log patterns are lower signal (could be noisy).
     */
    private double getSourceBonus(String source) {
        return switch (source) {
            case "BUG_TRACKER" -> 15.0;
            case "GIT" -> 10.0;
            case "LOG" -> 5.0;
            default -> 0.0;
        };
    }
}