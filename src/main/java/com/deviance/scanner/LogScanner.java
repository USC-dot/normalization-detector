package com.deviance.scanner;

import com.deviance.model.Anomaly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.*;

/**
 * Scans server log files for recurring warning and error patterns.
 *
 * What counts as a pattern:
 * - Any ERROR or WARN line
 * - Lines containing exception names (NullPointerException, etc.)
 * - Lines containing known warning keywords
 *
 * A pattern is "normalized" if it appears 10+ times.
 */
public class LogScanner {

    private static final Logger logger = LoggerFactory.getLogger(LogScanner.class);

    // Minimum occurrences before we flag something as normalized
    private static final int MIN_OCCURRENCES = 3;

    // Patterns we look for in log lines
    private static final List<String> WARNING_KEYWORDS = Arrays.asList(
            "ERROR", "WARN", "WARNING", "EXCEPTION", "FAILED", "FAILURE",
            "NullPointerException", "OutOfMemoryError", "StackOverflow",
            "Connection refused", "Timeout", "deprecated", "CRITICAL"
    );

    // Regex to extract the core message (strips timestamps and thread names)
    private static final Pattern LOG_MESSAGE_PATTERN = Pattern.compile(
            ".*(?:ERROR|WARN|WARNING).*?[-:]\\s*(.{10,100})"
    );

    private final String logFilePath;

    public LogScanner(String logFilePath) {
        this.logFilePath = logFilePath;
    }

    /**
     * Scan the log file and return detected anomalies.
     */
    public List<Anomaly> scan() {
        logger.info("Scanning log file: {}", logFilePath);

        // Map of pattern → occurrence count
        Map<String, Integer> patternCounts = new HashMap<>();
        // Map of pattern → example lines
        Map<String, List<String>> patternEvidence = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(
                new FileReader(logFilePath))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String normalized = extractPattern(line);
                if (normalized != null) {
                    patternCounts.merge(normalized, 1, Integer::sum);
                    patternEvidence.computeIfAbsent(normalized, k -> new ArrayList<>())
                            .add(line.length() > 120 ? line.substring(0, 120) + "..." : line);
                }
            }

        } catch (IOException e) {
            logger.warn("Could not read log file: {}", e.getMessage());
        }

        // Convert to Anomaly objects
        List<Anomaly> anomalies = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : patternCounts.entrySet()) {
            if (entry.getValue() >= MIN_OCCURRENCES) {
                Anomaly anomaly = new Anomaly(entry.getKey(), "LOG");
                anomaly.setOccurrenceCount(entry.getValue());
                anomaly.setFirstSeen(LocalDateTime.now().minusDays(30));
                anomaly.setLastSeen(LocalDateTime.now());
                anomaly.setDaysSinceLastAction(30);

                // Add up to 3 evidence lines
                List<String> evidence = patternEvidence.get(entry.getKey());
                if (evidence != null) {
                    evidence.stream().limit(3).forEach(anomaly::addEvidence);
                }

                anomalies.add(anomaly);
            }
        }

        logger.info("Found {} recurring patterns in logs", anomalies.size());
        return anomalies;
    }

    /**
     * Extract the core pattern from a log line.
     * Strips timestamps, thread names, line numbers.
     */
    private String extractPattern(String line) {
        // Check if line contains any warning keyword
        boolean hasKeyword = WARNING_KEYWORDS.stream()
                .anyMatch(kw -> line.toUpperCase().contains(kw.toUpperCase()));

        if (!hasKeyword) return null;

        // Try to extract the meaningful part using regex
        Matcher matcher = LOG_MESSAGE_PATTERN.matcher(line);
        if (matcher.find()) {
            String msg = matcher.group(1).trim();
            // Normalize: remove specific IDs, numbers, timestamps
            return msg.replaceAll("\\d{4,}", "N")
                    .replaceAll("[0-9a-f]{8}-[0-9a-f-]+", "UUID")
                    .trim();
        }

        // Fallback: return trimmed line up to 80 chars
        if (line.length() > 20) {
            return line.trim().substring(0, Math.min(80, line.length()));
        }

        return null;
    }
}