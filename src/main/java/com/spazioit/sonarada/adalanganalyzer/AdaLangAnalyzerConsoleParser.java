/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.spazioit.sonarada.adalanganalyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class AdaLangAnalyzerConsoleParser {

  private static final Pattern FINDING = Pattern.compile(
    "^(.*):(\\d+):(\\d+):\\s*(warning|error):\\s*(.*?)\\s*\\[([^]\\s]+)]\\s*$",
    Pattern.CASE_INSENSITIVE
  );
  private static final String RULE_PREFIX = "  rule:";
  private static final String ADVICE_PREFIX = "  advice:";
  private static final String WHY_PREFIX = "  why:";
  private static final String EVIDENCE_PREFIX = "  evidence:";
  private static final Pattern QUALITY = Pattern.compile(
    "^\\s*quality:\\s*([^()]+?)\\s*\\(([^()]+)\\)\\s*$",
    Pattern.CASE_INSENSITIVE
  );
  private static final String SOURCE_PREFIX = "  source:";
  private static final Pattern SOURCE_CARET = Pattern.compile("^\\s*(\\^+)\\s*$");
  private static final Pattern PROOF_OBLIGATION = Pattern.compile(
    "^\\s{4}(.*):(\\d+):(\\d+)\\s+\\[([^]\\s]+)]\\s+(\\S+)\\s*$",
    Pattern.CASE_INSENSITIVE
  );
  private static final Pattern PROOF_DETAIL = Pattern.compile(
    "^\\s{6}(method|why|evidence|imprecision|reason|blocked at|inline path):\\s*(.*)$",
    Pattern.CASE_INSENSITIVE
  );
  private static final Pattern VIOLATION_COUNT = Pattern.compile(
    "^Violations\\s*:\\s*(\\d+)\\s*$",
    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
  );
  private static final Pattern FILE_COUNT = Pattern.compile(
    "^Files scanned\\s*:\\s*(\\d+)\\s*$",
    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
  );
  private static final Pattern SKIPPED_CHECK_COUNT = Pattern.compile(
    "^Skipped checks\\s*:\\s*(\\d+)\\s+location\\(s\\).*$",
    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
  );
  private static final Pattern PROOF_OBLIGATION_COUNT = Pattern.compile(
    "^Proof obligations[^\\r\\n]*\\R\\s*Total\\s*:\\s*(\\d+)\\s*$",
    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
  );

  List<AdaLangAnalyzerFinding> parse(String output) {
    List<AdaLangAnalyzerFinding> findings = new ArrayList<>();
    PendingFinding pending = null;
    for (String line : output.lines().toList()) {
      Matcher matcher = FINDING.matcher(line);
      if (matcher.matches()) {
        if (pending != null) {
          findings.add(pending.toFinding());
        }
        pending = new PendingFinding(
          matcher.group(1),
          Integer.parseInt(matcher.group(2)),
          Integer.parseInt(matcher.group(3)),
          matcher.group(4),
          matcher.group(5),
          matcher.group(6)
        );
      } else if (pending != null && line.startsWith(RULE_PREFIX)) {
        pending.ruleDescription = line.substring(RULE_PREFIX.length()).trim();
      } else if (pending != null && line.startsWith(ADVICE_PREFIX)) {
        pending.advice = line.substring(ADVICE_PREFIX.length()).trim();
      } else if (pending != null && line.startsWith(WHY_PREFIX)) {
        pending.explanation = line.substring(WHY_PREFIX.length()).trim();
      } else if (pending != null && line.startsWith(EVIDENCE_PREFIX)) {
        pending.evidence = line.substring(EVIDENCE_PREFIX.length()).trim();
      } else if (pending != null && line.startsWith(SOURCE_PREFIX)) {
        pending.readingSource = true;
      } else if (pending != null) {
        Matcher qualityMatcher = QUALITY.matcher(line);
        if (qualityMatcher.matches()) {
          pending.softwareQuality = qualityMatcher.group(1).trim();
          pending.qualitySeverity = qualityMatcher.group(2).trim();
        } else if (pending.readingSource && pending.source.isEmpty()) {
          pending.source = line.strip();
        } else if (pending.readingSource) {
          Matcher caretMatcher = SOURCE_CARET.matcher(line);
          if (caretMatcher.matches()) {
            pending.sourceSpanLength = caretMatcher.group(1).length();
          }
          pending.readingSource = false;
        }
      }
    }
    if (pending != null) {
      findings.add(pending.toFinding());
    }
    return List.copyOf(findings);
  }

  List<AdaLangAnalyzerProofObligation> parseProofObligations(String output) {
    List<AdaLangAnalyzerProofObligation> obligations = new ArrayList<>();
    PendingProofObligation pending = null;
    for (String line : output.lines().toList()) {
      Matcher obligationMatcher = PROOF_OBLIGATION.matcher(line);
      if (obligationMatcher.matches()) {
        if (pending != null) {
          obligations.add(pending.toProofObligation());
        }
        pending = new PendingProofObligation(
          obligationMatcher.group(1),
          Integer.parseInt(obligationMatcher.group(2)),
          Integer.parseInt(obligationMatcher.group(3)),
          obligationMatcher.group(4),
          obligationMatcher.group(5)
        );
        continue;
      }

      if (pending != null) {
        Matcher detailMatcher = PROOF_DETAIL.matcher(line);
        if (detailMatcher.matches()) {
          pending.setDetail(detailMatcher.group(1), detailMatcher.group(2).trim());
        }
      }
    }
    if (pending != null) {
      obligations.add(pending.toProofObligation());
    }
    return List.copyOf(obligations);
  }

  int reportedViolationCount(String output) {
    return parseCount(VIOLATION_COUNT, output);
  }

  int reportedFileCount(String output) {
    return parseCount(FILE_COUNT, output);
  }

  int reportedSkippedCheckCount(String output) {
    return parseCount(SKIPPED_CHECK_COUNT, output);
  }

  int reportedProofObligationCount(String output) {
    return parseCount(PROOF_OBLIGATION_COUNT, output);
  }

  private static int parseCount(Pattern pattern, String output) {
    Matcher matcher = pattern.matcher(output);
    return matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
  }

  private static final class PendingFinding {
    private final String file;
    private final int line;
    private final int column;
    private final String severity;
    private final String message;
    private final String ruleId;
    private String ruleDescription = "";
    private String advice = "";
    private String explanation = "";
    private String evidence = "";
    private String softwareQuality = "";
    private String qualitySeverity = "";
    private String source = "";
    private int sourceSpanLength = 1;
    private boolean readingSource;

    private PendingFinding(String file, int line, int column, String severity, String message, String ruleId) {
      this.file = file;
      this.line = line;
      this.column = column;
      this.severity = severity;
      this.message = message;
      this.ruleId = ruleId;
    }

    private AdaLangAnalyzerFinding toFinding() {
      return new AdaLangAnalyzerFinding(
        file, line, column, severity, message, ruleId, ruleDescription, advice, explanation, evidence,
        softwareQuality, qualitySeverity, source, sourceSpanLength);
    }
  }

  private static final class PendingProofObligation {
    private final String file;
    private final int line;
    private final int column;
    private final String kind;
    private final String outcome;
    private String method = "";
    private String why = "";
    private String imprecision = "";
    private String evidence = "";
    private String reasonCode = "";
    private String blockingExpression = "";
    private String inlinePath = "";

    private PendingProofObligation(String file, int line, int column, String kind, String outcome) {
      this.file = file;
      this.line = line;
      this.column = column;
      this.kind = kind;
      this.outcome = outcome;
    }

    private void setDetail(String name, String value) {
      switch (name.toLowerCase(Locale.ROOT)) {
        case "method" -> method = value;
        case "why" -> why = value;
        case "evidence" -> evidence = value;
        case "reason" -> reasonCode = value;
        case "blocked at" -> blockingExpression = value;
        case "inline path" -> inlinePath = value;
        default -> imprecision = value;
      }
    }

    private AdaLangAnalyzerProofObligation toProofObligation() {
      return new AdaLangAnalyzerProofObligation(
        file, line, column, kind, outcome, method, why, imprecision, evidence, reasonCode, blockingExpression, inlinePath);
    }
  }
}
