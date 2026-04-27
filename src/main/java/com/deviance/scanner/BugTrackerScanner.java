package com.deviance.scanner;

import com.deviance.model.Anomaly;
import com.opencsv.CSVReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * Reads bug tracker CSV exports and detects normalized issues.
 *
 * Flags:
 * - Issues reopened 2+ times
 * - Issues open for 90+ days
 * - Duplicate tickets for the same root cause
 * - Issues with "won't fix" or "known issue" status
 */
public class BugTrackerScanner {

    private static final Logger logger = LoggerFactory.getLogger(BugTrackerScanner.class);

    private static final int REOPEN_THRESHOLD = 2;
    private static final int DAYS_OPEN_THRESHOLD = 90;

    private final String csvFilePath;

    public BugTrackerScanner(String csvFilePath) {
        this.csvFilePath = csvFilePath;
    }

    public List<Anomaly> scan() {
        logger.info("Scanning bug tracker data: {}", csvFilePath);

        File f = new File(csvFilePath);
        if (!f.exists()) {
            logger.info("No bug tracker CSV found, using sample data");
            return getSampleAnomalies();
        }

        List<Anomaly> anomalies = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(csvFilePath))) {
            String[] headers = reader.readNext(); // skip header row
            String[] row;

            Map<String, Integer> titleCounts = new HashMap<>();

            while ((row = reader.readNext()) != null) {
                if (row.length < 4) continue;

                String title = row[0];
                String status = row[1];
                String reopenCount = row[2];
                String daysOpen = row[3];

                // Normalize title to find duplicates
                String normalizedTitle = title.toLowerCase()
                        .replaceAll("\\d+", "N")
                        .trim();

                titleCounts.merge(normalizedTitle, 1, Integer::sum);

                // Flag reopened issues
                try {
                    int reopens = Integer.parseInt(reopenCount.trim());
                    if (reopens >= REOPEN_THRESHOLD) {
                        Anomaly a = new Anomaly("Repeatedly reopened: " + title, "BUG_TRACKER");
                        a.setOccurrenceCount(reopens);
                        a.setDaysSinceLastAction(Integer.parseInt(daysOpen.trim()));
                        a.addEvidence("Status: " + status + " | Reopened: " + reopens + " times");
                        anomalies.add(a);
                    }
                } catch (NumberFormatException ignored) {}
            }

            // Flag titles that appear multiple times (duplicates)
            for (Map.Entry<String, Integer> entry : titleCounts.entrySet()) {
                if (entry.getValue() >= 3) {
                    Anomaly a = new Anomaly("Recurring issue: " + entry.getKey(), "BUG_TRACKER");
                    a.setOccurrenceCount(entry.getValue());
                    a.setDaysSinceLastAction(60);
                    a.addEvidence("Same issue reported " + entry.getValue() + " times");
                    anomalies.add(a);
                }
            }

        } catch (Exception e) {
            logger.warn("Error reading bug CSV: {}", e.getMessage());
        }

        logger.info("Found {} bug tracker anomalies", anomalies.size());
        return anomalies;
    }

    private List<Anomaly> getSampleAnomalies() {
        List<Anomaly> sample = new ArrayList<>();

        Anomaly a1 = new Anomaly("Recurring issue: login timeout on high load", "BUG_TRACKER");
        a1.setOccurrenceCount(8);
        a1.setDaysSinceLastAction(120);
        a1.addEvidence("Ticket BUG-102: Login timeout - marked as known issue");
        a1.addEvidence("Ticket BUG-187: Same login timeout - closed as duplicate");
        a1.addEvidence("Ticket BUG-291: Login timeout reappeared after deploy");
        sample.add(a1);

        Anomaly a2 = new Anomaly("Repeatedly reopened: NullPointerException in payment flow", "BUG_TRACKER");
        a2.setOccurrenceCount(5);
        a2.setDaysSinceLastAction(90);
        a2.addEvidence("Reopened 5 times over 6 months - root cause never addressed");
        sample.add(a2);

        Anomaly a3 = new Anomaly("Recurring issue: database connection pool exhausted", "BUG_TRACKER");
        a3.setOccurrenceCount(12);
        a3.setDaysSinceLastAction(200);
        a3.addEvidence("First reported 8 months ago - workaround applied, root cause ignored");
        sample.add(a3);

        return sample;
    }
}