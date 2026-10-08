/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.spazioit.sonarada.adalanganalyzer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AdaLangAnalyzerProofObligationTest {

  @Test
  void definiteErrorAndUnprovedAreActionableButNothingElseIs() {
    assertThat(obligationWithOutcome("definite-error").isActionable()).isTrue();
    assertThat(obligationWithOutcome("unproved").isActionable()).isTrue();
    assertThat(obligationWithOutcome("proved-safe").isActionable()).isFalse();
    assertThat(obligationWithOutcome("unreachable").isActionable()).isFalse();
    assertThat(obligationWithOutcome("unsupported").isActionable()).isFalse();
  }

  @Test
  void reportsTheGnatproveVerdictAsGnatprovesAfterTheMethod() {
    AdaLangAnalyzerProofObligation unproved = obligationWithOutcome("unproved");

    assertThat(unproved.sonarMessage()).isEqualTo("Proof obligation [range-check] unproved. Method: static-evaluation");
    assertThat(unproved.withGnatprove("not-proved").sonarMessage())
      .isEqualTo("Proof obligation [range-check] unproved. Method: static-evaluation. GNATprove: not-proved");
  }

  @Test
  void staysActionableWhateverGnatproveSaidOfItsCheck() {
    // The verdict is GNATprove's and stands beside the analyzer's own outcome, never in its place.
    assertThat(obligationWithOutcome("unproved").withGnatprove("proved").isActionable()).isTrue();
    assertThat(obligationWithOutcome("definite-error").withGnatprove("proved").isActionable()).isTrue();
    assertThat(obligationWithOutcome("proved-safe").withGnatprove("not-proved").isActionable()).isFalse();
  }

  @Test
  void hasASeparateSubjectOnlyWhenTheObjectIsDeclaredElsewhere() {
    assertThat(obligationWithOutcome("unproved").hasSeparateSubject()).isFalse();
    assertThat(initializationCheck("demo.adb", 5, 10, "demo.ads", 7, 4).hasSeparateSubject()).isTrue();
    assertThat(initializationCheck("demo.adb", 5, 10, "demo.adb", 3, 4).hasSeparateSubject()).isTrue();
    // An out parameter checked at its subprogram's exit is located at its own declaration.
    assertThat(initializationCheck("demo.ads", 24, 37, "demo.ads", 24, 37).hasSeparateSubject()).isFalse();
  }

  private static AdaLangAnalyzerProofObligation initializationCheck(
    String file, int line, int column, String subjectFile, int subjectLine, int subjectColumn
  ) {
    return new AdaLangAnalyzerProofObligation(
      file, line, column, "initialization-check", "unproved", "flow-analysis", "", "", "", "", "", "",
      "Total", "", "none", "", subjectFile, subjectLine, subjectColumn);
  }

  private static AdaLangAnalyzerProofObligation obligationWithOutcome(String outcome) {
    return new AdaLangAnalyzerProofObligation(
      "demo.adb", 1, 1, "range-check", outcome, "static-evaluation", "", "");
  }
}
