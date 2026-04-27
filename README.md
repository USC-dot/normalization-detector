# 🔮 Normalization of Deviance Detector

> A Java-based static analysis tool that scans git commit histories, server logs, and bug tracker data to automatically detect recurring anomalies that engineering teams have silently normalized over time.

![Java](https://img.shields.io/badge/Java-17-orange)
![Maven](https://img.shields.io/badge/Maven-3.9-red)
![License](https://img.shields.io/badge/License-MIT-yellow)

---

## 🧠 What Is Normalization of Deviance?

**Normalization of Deviance** is when a team encounters a warning, error, or failure so many times that they stop treating it as a problem. It becomes background noise. Nobody acts on it. Eventually it causes a major incident.

Real examples:
- A `NullPointerException` in logs fires 50 times a day — nobody looks at it
- A bug ticket gets reopened 6 times over 8 months — never actually fixed
- Every deploy has a "hotfix" commit — teams stop questioning why

This tool detects these patterns **automatically** by analyzing:
1. Git commit history — looking for "fix again", "hotfix", "workaround" patterns
2. Server logs — finding recurring ERROR/WARN lines across time
3. Bug tracker CSV exports — finding reopened and duplicate tickets

Each pattern gets a **Deviance Score** and is classified as **Critical**, **Moderate**, or **Low** risk.

---

## 📸 Output Preview

The tool generates:
- Console summary showing top anomalies by score
- `output/report.html` — a dark-themed HTML report with all findings and recommendations

---

## ⚙️ Tech Stack

| Technology | Purpose |
|---|---|
| Java 17 | Core language |
| Maven | Build tool and dependency management |
| JGit (Eclipse) | Reads real git commit history |
| Apache Tika | Text extraction from log files |
| OpenCSV | Reads bug tracker CSV exports |
| Jackson | JSON parsing |
| SLF4J + Logback | Logging |

---

## 🚀 Setup Instructions

### Prerequisites

Make sure you have installed:
- Java 17+
- Maven 3.6+
- Git

Verify:
```bash
java --version
mvn --version
```

---

### Step 1 — Clone the Repo

```bash
git clone https://github.com/UtkarshSinghChauhan/normalization-detector.git
cd normalization-detector
```

---

### Step 2 — Install Dependencies

```bash
mvn install
```

Maven will automatically download all required libraries. You should see `BUILD SUCCESS`.

---

### Step 3 — Add Your Data

The tool works with three data sources. Sample data is already included in `data/` for immediate testing.

#### Option A — Use Sample Data (Quick Start)

Sample files are already in:
```
data/sample_logs/server.log       ← sample server errors
data/sample_bugs/bugs.csv         ← sample bug tracker export
data/sample_commits/              ← uses built-in sample commits
```

Just skip to Step 4.

#### Option B — Use Real Data

**Real Git Repo:**
```bash
# Clone any real Flask/Java/Python repo into data/sample_commits/
git clone https://github.com/miguelgrinberg/flasky.git data/sample_commits/flasky
```

Then update `Main.java`:
```java
String repoPath = "data/sample_commits/flasky";
```

**Real Server Logs:**

Any Nginx or Apache access/error log works. Standard format:
```
2025-01-15 10:22:01 ERROR UserService - NullPointerException in getUserById
```

Save it as `data/sample_logs/server.log`.

**Real Bug Tracker Data:**

Export from JIRA (Apache JIRA is publicly accessible):
1. Go to `https://issues.apache.org/jira/projects/HADOOP/issues`
2. Click **Export** → **Export Excel CSV (all fields)**
3. Save as `data/sample_bugs/bugs.csv`

Your CSV must have these columns (adjust `BugTrackerScanner.java` if column names differ):
```
title, status, reopen_count, days_open
```

---

### Step 4 — Build the Project

```bash
mvn clean package -q
```

You should see `BUILD SUCCESS`. This creates the runnable JAR in `target/`.

---

### Step 5 — Run the Detector

```bash
java -cp target/normalization-detector-1.0.0-jar-with-dependencies.jar com.deviance.Main
```

Expected console output:
```
============================================================
  NORMALIZATION OF DEVIANCE DETECTOR
============================================================

📊 DEVIANCE SUMMARY
----------------------------------------
Total anomalies detected : 7
🚨 Critical               : 3
⚠️  Moderate               : 2
📋 Low                    : 2

🔥 TOP ANOMALIES BY DEVIANCE SCORE:
----------------------------------------
  [95.0] CRITICAL | Recurring issue: login timeout on high load
  [82.5] CRITICAL | Repeatedly reopened: NullPointerException in payment
  [76.0] CRITICAL | GIT: fix  yet again
  [45.0] MODERATE | LOG: Connection pool exhausted
  [32.0] MODERATE | LOG: NullPointerException in getUserById

📄 Generating HTML report...
Report generated: output/report.html

✅ Done. Open output/report.html in your browser.
============================================================
```

---

### Step 6 — View the Report

Open `output/report.html` in any browser.

The report includes:
- Summary cards (Total, Critical, Moderate, Low)
- Each anomaly with its deviance score
- Evidence lines from logs/commits/bugs
- Actionable recommendations per anomaly

---

## 🔬 How It Works

### Deviance Score Formula

```
Score = (occurrenceCount × 2.0)
      + (daysSinceLastAction × 0.3)
      + sourceBonus

sourceBonus:
  BUG_TRACKER = +15  (strongest signal — human filed a ticket)
  GIT         = +10  (medium signal — developer mentioned it)
  LOG         = +5   (lower signal — automated noise possible)

Score capped at 100.
```

### Risk Classification

| Score | Level | Action |
|---|---|---|
| 60–100 | CRITICAL | Immediate fix sprint within 2 weeks |
| 30–59 | MODERATE | Add to next sprint backlog |
| 0–29 | LOW | Add to technical debt tracker |

### Git Pattern Detection

The scanner flags these commit message patterns:
- `fix X again` / `yet another fix`
- `hotfix` appearing repeatedly
- `workaround` / `hack` / `temporary fix`
- `NullPointerException` / `timeout` / `memory leak` in commit messages
- `revert: broke production`

### Log Pattern Detection

Flags any line containing:
`ERROR`, `WARN`, `EXCEPTION`, `FAILED`, `Timeout`, `Connection refused`, `deprecated`, `OutOfMemoryError`

Patterns that appear 3+ times are flagged as normalized.

---

## 📁 Project Structure

```
normalization-detector/
├── src/main/java/com/deviance/
│   ├── Main.java                      ← Entry point
│   ├── scanner/
│   │   ├── LogScanner.java            ← Parses server log files
│   │   ├── GitScanner.java            ← Reads git commit history (JGit)
│   │   └── BugTrackerScanner.java     ← Reads CSV bug tracker exports
│   ├── engine/
│   │   ├── PatternEngine.java         ← Orchestrates all scanners
│   │   ├── DevianceScorer.java        ← Calculates deviance score
│   │   └── RiskClassifier.java        ← Critical/Moderate/Low + recommendations
│   ├── model/
│   │   ├── Anomaly.java               ← Data class for one anomaly
│   │   └── DevianceReport.java        ← Full report model
│   └── report/
│       └── ReportGenerator.java       ← Generates HTML report
├── data/
│   ├── sample_logs/server.log         ← Sample server errors
│   ├── sample_commits/                ← Point at any git repo
│   └── sample_bugs/bugs.csv           ← Sample bug tracker data
├── output/
│   └── report.html                    ← Generated report
└── pom.xml                            ← Maven dependencies
```

---

## 🧪 Tested Against

| Data Source | Dataset | Findings |
|---|---|---|
| Server Logs | Sample error logs | 2 recurring patterns (NullPointerException, connection pool) |
| Git History | miguelgrinberg/flasky | Recurring hotfix and workaround patterns |
| Bug Tracker | Sample JIRA-format CSV | 3 normalized issues including login timeout (reopened 5x) |

Compatible with NASA HTTP log format (1.8M+ requests) and Apache JIRA CSV exports.

---

## ⚠️ Limitations

- Git scanner currently uses regex pattern matching on commit messages
- Log scanner expects standard Nginx/Apache/Log4j format
- Bug tracker scanner expects 4 columns: `title, status, reopen_count, days_open`

---

## 🔮 Planned Features

- [ ] GitHub API integration — scan any public repo directly via API
- [ ] Stanford CoreNLP integration for smarter commit message analysis
- [ ] Week-over-week trend tracking
- [ ] Slack/email alerts when new critical patterns emerge
- [ ] ML-based clustering to group similar anomalies automatically

---

## 📄 License

MIT License — free to use, modify, and reference in your portfolio.
