/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.spazioit.sonarada.adalanganalyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class AdaLangAnalyzerConsoleParser {

  private static final Pattern FINDING = Pattern.compile(
    "^(.*):(\\d+):(\\d+):\\s*(warning|error):\\s*(.*?)\\s*\\[([^]\\s]+)]\\s*$",
    Pattern.CASE_INSENSITIVE
  );
  private static final String RULE_PREFIX = "  rule:";
  private static final String ADVICE_PREFIX = "  advice:";

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
      }
    }
    if (pending != null) {
      findings.add(pending.toFinding());
    }
    return List.copyOf(findings);
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

    private PendingFinding(String file, int line, int column, String severity, String message, String ruleId) {
      this.file = file;
      this.line = line;
      this.column = column;
      this.severity = severity;
      this.message = message;
      this.ruleId = ruleId;
    }

    private AdaLangAnalyzerFinding toFinding() {
      return new AdaLangAnalyzerFinding(file, line, column, severity, message, ruleId, ruleDescription, advice);
    }
  }
}
