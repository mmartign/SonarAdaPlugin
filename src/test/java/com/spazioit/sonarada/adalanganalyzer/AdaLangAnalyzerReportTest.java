/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.spazioit.sonarada.adalanganalyzer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class AdaLangAnalyzerReportTest {
  private static final String VERIFICATION_SCOPE = "bounded scalar verification; unsupported boundaries are explicit";
  private static final List<AdaLangAnalyzerProofObligation> OBLIGATIONS = List.of(
    obligation("unproved"), obligation("definite-error"), obligation("proved-safe"), obligation("unproved"));

  @Test
  void importsUnprovedObligationsOfAVerificationRun() {
    AdaLangAnalyzerReport report = report(VERIFICATION_SCOPE);

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

  @Test
  void raisesAnUnprovedObligationWhateverGnatproveSaidOfItsCheck() {
    // GNATprove's verdict stands beside the analyzer's own outcome and does not replace it.
    List<AdaLangAnalyzerProofObligation> obligations = List.of(
      obligation("unproved").withGnatprove("proved"),
      obligation("unproved").withGnatprove("justified"),
      obligation("unproved").withGnatprove("not-proved"),
      obligation("unproved"),
      obligation("definite-error").withGnatprove("proved"),
      obligation("proved-safe").withGnatprove("not-proved"));
    AdaLangAnalyzerReport report = new AdaLangAnalyzerReport(
      List.of(), obligations, 1, 0, obligations.size(), -1, VERIFICATION_SCOPE);

    assertThat(obligations).extracting(report::isIssue).containsExactly(true, true, true, true, true, false);
    assertThat(report.unattemptedProofCount()).isZero();
  }

  @Test
  void raisesAnUnprovedObligationOfAVerificationRunThatNoAnalysisWasAppliedTo() {
    // flow-dependencies in AdaLang Analyzer 1.8.1 to 1.8.4: no route proves a Depends aspect
    // yet, so it is always unproved, with the method none. It is an issue like any other.
    AdaLangAnalyzerProofObligation depends = new AdaLangAnalyzerProofObligation(
      "src/demo.adb", 40, 11, "flow-dependencies", "unproved", "none",
      "the Depends aspect is not shown to be the dependencies of the subprogram",
      "information flow is not yet analyzed to the point of proof");
    List<AdaLangAnalyzerProofObligation> obligations = List.of(depends, obligation("unproved"));
    AdaLangAnalyzerReport report = new AdaLangAnalyzerReport(
      List.of(), obligations, 1, 0, obligations.size(), -1, VERIFICATION_SCOPE);

    assertThat(report.proofAttempted()).isTrue();
    assertThat(obligations).extracting(report::isIssue).containsExactly(true, true);
    assertThat(report.unattemptedProofCount()).isZero();
  }

  private static AdaLangAnalyzerReport report(String proofScope) {
    return new AdaLangAnalyzerReport(List.of(), OBLIGATIONS, 1, 0, OBLIGATIONS.size(), -1, proofScope);
  }

  private static AdaLangAnalyzerProofObligation obligation(String outcome) {
    return new AdaLangAnalyzerProofObligation(
      "src/demo.adb", 10, 7, "index-check", outcome, "abstract-interpretation", "", "");
  }
}
