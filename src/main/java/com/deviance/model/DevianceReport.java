package com.deviance.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The full report produced by one scan.
 * Contains all detected anomalies grouped by risk level.
 */
public class DevianceReport {

    private LocalDateTime generatedAt;
    private String repoPath;
    private List<Anomaly> anomalies = new ArrayList<>();

    public DevianceReport(String repoPath) {
        this.repoPath = repoPath;
        this.generatedAt = LocalDateTime.now();
    }

    public void addAnomaly(Anomaly anomaly) {
        anomalies.add(anomaly);
    }

    public List<Anomaly> getAllAnomalies() {
        // Return sorted by deviance score (highest first)
        return anomalies.stream()
                .sorted((a, b) -> Double.compare(b.getDevianceScore(), a.getDevianceScore()))
                .collect(Collectors.toList());
    }

    public List<Anomaly> getCritical() {
        return anomalies.stream()
                .filter(a -> "CRITICAL".equals(a.getRiskLevel()))
                .collect(Collectors.toList());
    }

    public List<Anomaly> getModerate() {
        return anomalies.stream()
                .filter(a -> "MODERATE".equals(a.getRiskLevel()))
                .collect(Collectors.toList());
    }

    public List<Anomaly> getLow() {
        return anomalies.stream()
                .filter(a -> "LOW".equals(a.getRiskLevel()))
                .collect(Collectors.toList());
    }

    public int getTotalCount() { return anomalies.size(); }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public String getRepoPath() { return repoPath; }
}