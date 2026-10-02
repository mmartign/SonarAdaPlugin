/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.spazioit.sonarada.adalanganalyzer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class AdaLangAnalyzerReportTest {
  private static final List<AdaLangAnalyzerProofObligation> OBLIGATIONS = List.of(
    obligation("unproved"), obligation("definite-error"), obligation("proved-safe"), obligation("unproved"));

  @Test
  void importsUnprovedObligationsOfAVerificationRun() {
    AdaLangAnalyzerReport report = report("bounded scalar verification; unsupported boundaries are explicit");

    assertThat(report.proofAttempted()).isTrue();
    assertThat(OBLIGATIONS).extracting(report::isIssue).containsExactly(true, true, false, true);
    assertThat(report.unattemptedProofCount()).isZero();
  }

  @Test
  void keepsOnlyDefiniteErrorsOfARunThatAttemptedNoProof() {
    AdaLangAnalyzerReport report = report("enumerated outcomes in current analysis scope; not exhaustive");

    assertThat(report.proofAttempted()).isFalse();
    assertThat(OBLIGATIONS).extracting(report::isIssue).containsExactly(false, true, false, false);
    assertThat(report.unattemptedProofCount()).isEqualTo(2);
  }

  @Test
  void treatsAReportWithoutAScopeAsAVerificationRun() {
    AdaLangAnalyzerReport report = new AdaLangAnalyzerReport(List.of(), OBLIGATIONS, -1, -1, -1, -1);

    assertThat(report.proofAttempted()).isTrue();
    assertThat(OBLIGATIONS).extracting(report::isIssue).containsExactly(true, true, false, true);
  }

  private static AdaLangAnalyzerReport report(String proofScope) {
    return new AdaLangAnalyzerReport(List.of(), OBLIGATIONS, 1, 0, OBLIGATIONS.size(), -1, proofScope);
  }

  private static AdaLangAnalyzerProofObligation obligation(String outcome) {
    return new AdaLangAnalyzerProofObligation(
      "src/demo.adb", 10, 7, "index-check", outcome, "abstract-interpretation", "", "");
  }
}
