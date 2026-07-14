/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.spazioit.sonarada.adalanganalyzer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class AdaLangAnalyzerConsoleParserTest {
  private final AdaLangAnalyzerConsoleParser parser = new AdaLangAnalyzerConsoleParser();

  @Test
  void parsesFindingAndItsDetails() {
    String output = """
      /project/src/demo.adb:12:7: warning: goto statements are forbidden [No_Goto]
        rule: Avoid unstructured control flow
        advice: Replace goto with structured statements
        source:
          goto Finished;
          ^^^^^^^^^^^^^^

      Files scanned : 1
      Violations    : 1
      """;

    List<AdaLangAnalyzerFinding> findings = parser.parse(output);

    assertThat(findings).containsExactly(new AdaLangAnalyzerFinding(
      "/project/src/demo.adb", 12, 7, "warning", "goto statements are forbidden",
      "No_Goto", "Avoid unstructured control flow", "Replace goto with structured statements"));
    assertThat(findings.getFirst().sonarMessage())
      .isEqualTo("goto statements are forbidden — Avoid unstructured control flow Advice: Replace goto with structured statements");
  }

  @Test
  void supportsWindowsDriveLettersAndMultipleFindings() {
    String output = """
      C:\\src\\demo.adb:2:4: warning: first message [No_Label]
      C:\\src\\demo.adb:8:3: error: second message [Division_By_Zero]
      """;

    assertThat(parser.parse(output))
      .extracting(AdaLangAnalyzerFinding::file, AdaLangAnalyzerFinding::ruleId)
      .containsExactly(
        org.assertj.core.groups.Tuple.tuple("C:\\src\\demo.adb", "No_Label"),
        org.assertj.core.groups.Tuple.tuple("C:\\src\\demo.adb", "Division_By_Zero"));
  }

  @Test
  void ignoresSummaryAndUnrelatedDiagnostics() {
    assertThat(parser.parse("Files scanned : 3\nViolations    : 0\nNo violations found.\n")).isEmpty();
  }
}
