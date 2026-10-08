/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.spazioit.sonarada.adalanganalyzer;

import java.util.Locale;

/**
 * One proof obligation of an AdaLang Analyzer report. {@code gnatprove} is what GNATprove said of
 * the same check ({@code proved}, {@code justified}, or {@code not-proved}) when the analyzer was
 * given a GNATprove log, and empty otherwise. The subject is the declaration of the object the
 * obligation is about, which only JSON reports carry and only for initialization checks; its
 * file is empty when there is none.
 */
record AdaLangAnalyzerProofObligation(
  String file,
  int line,
  int column,
  String kind,
  String outcome,
  String method,
  String why,
  String imprecision,
  String evidence,
  String reasonCode,
  String blockingExpression,
  String inlinePath,
  String operation,
  String assumptions,
  String configurationId,
  String gnatprove,
  String subjectFile,
  int subjectLine,
  int subjectColumn
) {
  AdaLangAnalyzerProofObligation(
    String file, int line, int column, String kind, String outcome, String method, String why, String imprecision
  ) {
    this(file, line, column, kind, outcome, method, why, imprecision, "", "", "", "");
  }

  AdaLangAnalyzerProofObligation(
    String file, int line, int column, String kind, String outcome, String method, String why, String imprecision,
    String evidence, String reasonCode, String blockingExpression, String inlinePath
  ) {
    this(file, line, column, kind, outcome, method, why, imprecision, evidence, reasonCode, blockingExpression,
      inlinePath, "", "", "", "", "", 0, 0);
  }

  AdaLangAnalyzerProofObligation withGnatprove(String verdict) {
    return new AdaLangAnalyzerProofObligation(
      file, line, column, kind, outcome, method, why, imprecision, evidence, reasonCode, blockingExpression,
      inlinePath, operation, assumptions, configurationId, verdict, subjectFile, subjectLine, subjectColumn);
  }

  String ruleId() {
    String sanitizedKind = kind.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]+", "_");
    return "proof-obligation:" + (sanitizedKind.isBlank() ? "unknown" : sanitizedKind);
  }

  String sonarMessage() {
    StringBuilder result = new StringBuilder("Proof obligation [")
      .append(kind)
      .append("] ")
      .append(outcome);
    appendDetail(result, "Operation", operation);
    appendDetail(result, "Method", method);
    appendDetail(result, "GNATprove", gnatprove);
    appendDetail(result, "Why", why);
    appendDetail(result, "Evidence", evidence);
    appendDetail(result, "Imprecision", imprecision);
    appendDetail(result, "Assumptions", assumptions);
    appendDetail(result, "Reason", reasonCode);
    appendDetail(result, "Blocked at", blockingExpression);
    appendDetail(result, "Inline path", inlinePath);
    return result.toString();
  }

  /**
   * True for statuses that represent a concrete risk worth surfacing as an issue:
   * an outcome that is proved to fail ("definite-error") or one that could not be
   * ruled out ("unproved"). "proved-safe" is good news, and "unreachable"/"unsupported"
   * mean the check does not apply or could not run, so neither is a finding.
   */
  boolean isActionable() {
    return isUnproved() || isDefiniteError();
  }

  boolean isUnproved() {
    return "unproved".equalsIgnoreCase(outcome);
  }

  boolean isDefiniteError() {
    return "definite-error".equalsIgnoreCase(outcome);
  }

  /** True when the declaration of the object is known and is not where the obligation itself is. */
  boolean hasSeparateSubject() {
    return !subjectFile.isBlank()
      && subjectLine > 0
      && !(subjectFile.equals(file) && subjectLine == line && subjectColumn == column);
  }

  private static void appendDetail(StringBuilder result, String label, String value) {
    if (!value.isBlank()) {
      result.append(". ").append(label).append(": ").append(value);
    }
  }
}
