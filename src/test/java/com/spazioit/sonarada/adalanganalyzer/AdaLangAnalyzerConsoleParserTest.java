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
        quality: Maintainability (Medium)
        source:
          goto Finished;
          ^^^^^^^^^^^^^^

      Files scanned : 1
      Violations    : 1
      """;

    List<AdaLangAnalyzerFinding> findings = parser.parse(output);

    assertThat(findings).containsExactly(new AdaLangAnalyzerFinding(
      "/project/src/demo.adb", 12, 7, "warning", "goto statements are forbidden",
      "No_Goto", "Avoid unstructured control flow", "Replace goto with structured statements",
      "Maintainability", "Medium"));
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
  void parsesQualityMetadataFromCurrentAnalyzerOutput() {
    String output = """
      /project/src/demo.adb:15:7: warning: condition is contradictory [Contradictory_Condition]
        rule: Find boolean expressions of the form X and not X or X or not X.
        advice: Correct the copied or negated operand.
        quality: Reliability (High)
        source:
          if Flag and then not Flag then
             ^^^^^^^^^^^^^^^^^^^^^^
      """;

    assertThat(parser.parse(output).getFirst())
      .extracting(AdaLangAnalyzerFinding::softwareQuality, AdaLangAnalyzerFinding::qualitySeverity)
      .containsExactly("Reliability", "High");
  }

  @Test
  void ignoresSummaryAndUnrelatedDiagnostics() {
    assertThat(parser.parse("Files scanned : 3\nViolations    : 0\nNo violations found.\n")).isEmpty();
  }

  @Test
  void parsesSavedAnalyzerReportAndValidatesItsSummary() {
    String report = """
      adalang-analyzer [INFO]: Reading project with GPR2: demo.gpr
      /checkout/src/demo.adb:23:13: warning: subprogram has 2 return statements [No_Multiple_Return]
        rule: Find subprograms with more than one return statement.
        advice: Restructure the subprogram around a single return statement.
        quality: Maintainability (Low)
        source:
             function Node_Text
                      ^^^^^^^^^
      adalang-analyzer [INFO]: skipping checks at <CallExpr demo.adb:30:1-30:4>: unavailable
      /checkout/src/demo.adb:74:13: warning: assigned value is never read [Dead_Store]
        rule: Find assignments whose stored value is never read later.
        advice: Remove the assignment or restore the intended use.
        quality: Maintainability (Medium)

      Files scanned : 1
      Violations    : 2
      Skipped checks: 1 location(s)
      """;

    List<AdaLangAnalyzerFinding> findings = parser.parse(report);

    assertThat(findings)
      .extracting(AdaLangAnalyzerFinding::ruleId)
      .containsExactly("No_Multiple_Return", "Dead_Store");
    assertThat(parser.reportedViolationCount(report)).isEqualTo(findings.size());
  }
}
