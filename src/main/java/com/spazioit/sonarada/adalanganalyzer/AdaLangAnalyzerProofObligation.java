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

  boolean isUnproved() {
    return "unproved".equalsIgnoreCase(outcome);
  }

  private static void appendDetail(StringBuilder result, String label, String value) {
    if (!value.isBlank()) {
      result.append(". ").append(label).append(": ").append(value);
    }
  }
}
