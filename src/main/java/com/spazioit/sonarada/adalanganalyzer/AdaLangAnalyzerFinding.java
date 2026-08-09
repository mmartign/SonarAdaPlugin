/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.spazioit.sonarada.adalanganalyzer;

record AdaLangAnalyzerFinding(
  String file,
  int line,
  int column,
  String severity,
  String message,
  String ruleId,
  String ruleDescription,
  String advice,
  String explanation,
  String evidence,
  String softwareQuality,
  String qualitySeverity,
  String source,
  int sourceSpanLength
) {
  String sonarMessage() {
    StringBuilder result = new StringBuilder(message);
    if (!ruleDescription.isBlank()) {
      result.append(" — ").append(ruleDescription);
    }
    if (!advice.isBlank()) {
      result.append(" Advice: ").append(advice);
    }
    if (!explanation.isBlank()) {
      result.append(" Why: ").append(explanation);
    }
    if (!evidence.isBlank()) {
      result.append(" Evidence: ").append(evidence);
    }
    return result.toString();
  }

  int highlightLength() {
    return Math.max(1, sourceSpanLength);
  }
}
