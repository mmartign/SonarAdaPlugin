/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.spazioit.sonarada.adalanganalyzer;

import java.util.List;
import java.util.Locale;

/**
 * The findings, proof obligations, and self-reported summary counts of one AdaLang Analyzer
 * run or report, regardless of whether it was read from console text, JSON, or SARIF. A
 * negative count means the source format does not report that figure. The proof scope is the
 * analyzer's own description of what its obligation outcomes mean, empty when not reported.
 */
record AdaLangAnalyzerReport(
  List<AdaLangAnalyzerFinding> findings,
  List<AdaLangAnalyzerProofObligation> proofObligations,
  int fileCount,
  int violationCount,
  int proofObligationCount,
  int skippedCheckCount,
  String proofScope
) {
  // Proof_Obligations.Scope_Description of a run without --verify.
  private static final String ENUMERATION_SCOPE_PREFIX = "enumerated outcomes";

  AdaLangAnalyzerReport(
    List<AdaLangAnalyzerFinding> findings, List<AdaLangAnalyzerProofObligation> proofObligations,
    int fileCount, int violationCount, int proofObligationCount, int skippedCheckCount
  ) {
    this(findings, proofObligations, fileCount, violationCount, proofObligationCount, skippedCheckCount, "");
  }

  /**
   * False when the analyzer says it only enumerated the obligations, which is what it does
   * without {@code --verify}: nothing is proved, and every obligation that is not a known
   * failure is reported as unproved. A report that does not state its scope is taken to come
   * from a verification run.
   */
  boolean proofAttempted() {
    return !proofScope.toLowerCase(Locale.ROOT).startsWith(ENUMERATION_SCOPE_PREFIX);
  }

  /**
   * True for an obligation worth an issue: a definite error always, an unproved one only when
   * a proof was attempted. Unproved obligations of a run that attempted none say nothing about
   * the code.
   */
  boolean isIssue(AdaLangAnalyzerProofObligation proofObligation) {
    return proofObligation.isActionable()
      && (proofAttempted() || !"unproved".equalsIgnoreCase(proofObligation.outcome()));
  }

  long unattemptedProofCount() {
    return proofAttempted() ? 0 : proofObligations.stream()
      .filter(proofObligation -> "unproved".equalsIgnoreCase(proofObligation.outcome()))
      .count();
  }
}
