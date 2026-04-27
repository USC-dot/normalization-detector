package com.deviance.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single detected anomaly — a pattern that has been normalized.
 *
 * Example: "NullPointerException in UserService" appearing in:
 * - 47 log entries over 6 months
 * - 3 git commits mentioning "fix NPE again"
 * - 2 bug tickets marked as "known issue"
 */
public class Anomaly {

    // The pattern that was detected (e.g. "NullPointerException in UserService")
    private String pattern;

    // Where it was found: "LOG", "GIT", "BUG_TRACKER"
    private String source;

    // How many times this pattern appeared
    private int occurrenceCount;

    // When it first appeared
    private LocalDateTime firstSeen;

    // When it last appeared
    private LocalDateTime lastSeen;

    // How many days since someone last did something about it
    private long daysSinceLastAction;

    // The deviance score (higher = more dangerous)
    private double devianceScore;

    // Risk level: "CRITICAL", "MODERATE", "LOW"
    private String riskLevel;

    // Actual lines from logs/commits where it appeared
    private List<String> evidence = new ArrayList<>();

    // What the tool recommends doing about it
    private String recommendation;

    // Constructor
    public Anomaly(String pattern, String source) {
        this.pattern = pattern;
        this.source = source;
        this.firstSeen = LocalDateTime.now();
        this.lastSeen = LocalDateTime.now();
    }

    // Getters and Setters
    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public int getOccurrenceCount() { return occurrenceCount; }
    public void setOccurrenceCount(int occurrenceCount) { this.occurrenceCount = occurrenceCount; }

    public LocalDateTime getFirstSeen() { return firstSeen; }
    public void setFirstSeen(LocalDateTime firstSeen) { this.firstSeen = firstSeen; }

    public LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }

    public long getDaysSinceLastAction() { return daysSinceLastAction; }
    public void setDaysSinceLastAction(long daysSinceLastAction) { this.daysSinceLastAction = daysSinceLastAction; }

    public double getDevianceScore() { return devianceScore; }
    public void setDevianceScore(double devianceScore) { this.devianceScore = devianceScore; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public List<String> getEvidence() { return evidence; }
    public void addEvidence(String line) { this.evidence.add(line); }

    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }

    @Override
    public String toString() {
        return String.format("[%s] %s | Score: %.2f | Occurrences: %d | Risk: %s",
                source, pattern, devianceScore, occurrenceCount, riskLevel);
    }
}