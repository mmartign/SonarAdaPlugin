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

  private static AdaLangAnalyzerProofObligation obligationWithOutcome(String outcome) {
    return new AdaLangAnalyzerProofObligation(
      "demo.adb", 1, 1, "range-check", outcome, "static-evaluation", "", "");
  }
}
