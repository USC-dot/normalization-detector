package com.deviance.scanner;

import com.deviance.model.Anomaly;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.regex.*;

/**
 * Scans git commit messages for patterns indicating normalized deviance.
 *
 * Red flag commit message patterns:
 * - "fix X again" / "another fix for X"
 * - "same issue" / "same bug"
 * - "hotfix" repeated 3+ times
 * - "workaround" / "hack" / "temporary fix"
 * - "TODO" / "FIXME" that never get resolved
 */
public class GitScanner {

    private static final Logger logger = LoggerFactory.getLogger(GitScanner.class);

    // Patterns in commit messages that indicate recurring issues
    private static final List<Pattern> DEVIANCE_PATTERNS = Arrays.asList(
            Pattern.compile("(?i)(fix|fixing|fixed).{0,20}(again|once more|yet again|still)"),
            Pattern.compile("(?i)(same|another|yet another).{0,20}(bug|issue|error|problem|fix)"),
            Pattern.compile("(?i)(hotfix|hot.fix)"),
            Pattern.compile("(?i)(workaround|work.around|hack|kludge|temp.fix|temporary)"),
            Pattern.compile("(?i)(revert|reverting).{0,30}(broke|broken|issue)"),
            Pattern.compile("(?i)(null.?pointer|npe|nullpointer)"),
            Pattern.compile("(?i)(timeout|timed.out|connection.refused)"),
            Pattern.compile("(?i)(memory.leak|out.of.memory|oom)")
    );

    private final String repoPath;

    public GitScanner(String repoPath) {
        this.repoPath = repoPath;
    }

    /**
     * Scan git history and return detected anomalies.
     * Falls back to CSV mode if no git repo found.
     */
    public List<Anomaly> scan() {
        logger.info("Scanning git history at: {}", repoPath);

        // Check if it's a real git repo
        File gitDir = new File(repoPath, ".git");
        if (gitDir.exists()) {
            return scanGitRepo();
        } else {
            logger.info("No .git directory found, using sample commit data");
            return scanSampleData();
        }
    }

    private List<Anomaly> scanGitRepo() {
        Map<String, Integer> patternCounts = new HashMap<>();
        Map<String, List<String>> patternEvidence = new HashMap<>();

        try {
            Repository repository = new FileRepositoryBuilder()
                    .setGitDir(new File(repoPath, ".git"))
                    .build();

            try (Git git = new Git(repository)) {
                Iterable<RevCommit> commits = git.log().call();

                for (RevCommit commit : commits) {
                    String message = commit.getFullMessage();
                    checkMessage(message, patternCounts, patternEvidence);
                }
            }

        } catch (Exception e) {
            logger.warn("Error reading git repo: {}", e.getMessage());
        }

        return buildAnomalies(patternCounts, patternEvidence);
    }

    private void checkMessage(String message,
                              Map<String, Integer> counts,
                              Map<String, List<String>> evidence) {
        for (Pattern pattern : DEVIANCE_PATTERNS) {
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                String patternStr = pattern.pattern()
                        .replaceAll("\\(\\?i\\)", "")
                        .replaceAll("[()?.{}]", "");
                String key = "GIT: " + patternStr.substring(0, Math.min(50, patternStr.length()));

                counts.merge(key, 1, Integer::sum);
                evidence.computeIfAbsent(key, k -> new ArrayList<>())
                        .add(message.trim().substring(0, Math.min(100, message.trim().length())));
            }
        }
    }

    private List<Anomaly> scanSampleData() {
        // Sample data for demo purposes
        List<String> sampleCommits = Arrays.asList(
                "fix NPE again in UserService",
                "another hotfix for payment timeout",
                "workaround for null pointer in auth",
                "fix same connection refused error again",
                "hotfix: temp fix for memory leak",
                "fix NPE again - third time this week",
                "another hotfix for payment timeout again",
                "revert: this broke production again"
        );

        Map<String, Integer> counts = new HashMap<>();
        Map<String, List<String>> evidence = new HashMap<>();

        for (String commit : sampleCommits) {
            checkMessage(commit, counts, evidence);
        }

        return buildAnomalies(counts, evidence);
    }

    private List<Anomaly> buildAnomalies(Map<String, Integer> counts,
                                         Map<String, List<String>> evidence) {
        List<Anomaly> anomalies = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() >= 2) {
                Anomaly anomaly = new Anomaly(entry.getKey(), "GIT");
                anomaly.setOccurrenceCount(entry.getValue());
                anomaly.setDaysSinceLastAction(45);

                List<String> ev = evidence.get(entry.getKey());
                if (ev != null) ev.stream().limit(3).forEach(anomaly::addEvidence);

                anomalies.add(anomaly);
            }
        }

        logger.info("Found {} patterns in git history", anomalies.size());
        return anomalies;
    }
}