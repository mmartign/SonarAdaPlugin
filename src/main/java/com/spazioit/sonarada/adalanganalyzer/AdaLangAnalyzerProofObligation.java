/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.spazioit.sonarada.adalanganalyzer;

import java.util.Locale;

record AdaLangAnalyzerProofObligation(
  String file,
  int line,
  int column,
  String kind,
  String outcome,
  String method,
  String why,
  String imprecision
) {
  String ruleId() {
    String sanitizedKind = kind.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]+", "_");
    return "proof-obligation:" + (sanitizedKind.isBlank() ? "unknown" : sanitizedKind);
  }

  String sonarMessage() {
    StringBuilder result = new StringBuilder("Proof obligation [")
      .append(kind)
      .append("] ")
      .append(outcome);
    appendDetail(result, "Method", method);
    appendDetail(result, "Why", why);
    appendDetail(result, "Imprecision", imprecision);
    return result.toString();
  }

  /**
   * True for statuses that represent a concrete risk worth surfacing as an issue:
   * an outcome that is proved to fail ("definite-error") or one that could not be
   * ruled out ("unproved"). "proved-safe" is good news, and "unreachable"/"unsupported"
   * mean the check does not apply or could not run, so neither is a finding.
   */
  boolean isActionable() {
    return "unproved".equalsIgnoreCase(outcome) || "definite-error".equalsIgnoreCase(outcome);
  }

  private static void appendDetail(StringBuilder result, String label, String value) {
    if (!value.isBlank()) {
      result.append(". ").append(label).append(": ").append(value);
    }
  }
}
