/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
  String advice
) {
  String sonarMessage() {
    StringBuilder result = new StringBuilder(message);
    if (!ruleDescription.isBlank()) {
      result.append(" — ").append(ruleDescription);
    }
    if (!advice.isBlank()) {
      result.append(" Advice: ").append(advice);
    }
    return result.toString();
  }
}
