/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.spazioit.sonarada.adalanganalyzer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
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
      "No_Goto", "Avoid unstructured control flow", "Replace goto with structured statements", "", "",
      "Maintainability", "Medium", "goto Finished;", 14));
    assertThat(findings.getFirst().sonarMessage())
      .isEqualTo("goto statements are forbidden — Avoid unstructured control flow Advice: Replace goto with structured statements");
    assertThat(findings.getFirst().highlightLength()).isEqualTo(14);
  }

  @Test
  void parsesExplanationAndEvidenceWhenPresent() {
    String output = """
      /project/src/demo.adb:9:3: warning: division may fail [Division_By_Zero]
        rule: Find divisions whose divisor may be zero.
        advice: Guard the division or prove the divisor is non-zero.
        why: the divisor is not bounded away from zero on this path
        evidence: Divisor = Count - Limit
        quality: Reliability (High)
      """;

    AdaLangAnalyzerFinding finding = parser.parse(output).getFirst();

    assertThat(finding.explanation()).isEqualTo("the divisor is not bounded away from zero on this path");
    assertThat(finding.evidence()).isEqualTo("Divisor = Count - Limit");
    assertThat(finding.sonarMessage()).isEqualTo(
      "division may fail — Find divisions whose divisor may be zero."
        + " Advice: Guard the division or prove the divisor is non-zero."
        + " Why: the divisor is not bounded away from zero on this path"
        + " Evidence: Divisor = Count - Limit");
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

  @Test
  void parsesProofObligationsAndEveryDetail() {
    String report = """
      Files scanned : 1
      Violations    : 0

      Proof obligations (enumerated outcomes in current analysis scope; not exhaustive):
        Total : 2
        unproved : 2
        Details:
          /checkout/src/demo.adb:18:23 [index-check] unproved
            method: abstract-interpretation
            why: index-check failure is not established, but absence is not proved
            evidence: Index in 1 .. N
            imprecision: index and bound ranges remain inconclusive
            reason: widening-at-loop-header
            blocked at: A (I)
            inline path: demo.adb:12 -> demo.adb:18
          C:\\checkout\\src\\other.adb:9:4 [precondition] unproved
            method: contract-transfer
            why: precondition failure is not established, but absence is not proved
            imprecision: current contract transfer does not certify safety
      """;

    List<AdaLangAnalyzerProofObligation> obligations = parser.parseProofObligations(report);

    assertThat(obligations).containsExactly(
      new AdaLangAnalyzerProofObligation(
        "/checkout/src/demo.adb", 18, 23, "index-check", "unproved", "abstract-interpretation",
        "index-check failure is not established, but absence is not proved",
        "index and bound ranges remain inconclusive",
        "Index in 1 .. N", "widening-at-loop-header", "A (I)", "demo.adb:12 -> demo.adb:18"),
      new AdaLangAnalyzerProofObligation(
        "C:\\checkout\\src\\other.adb", 9, 4, "precondition", "unproved", "contract-transfer",
        "precondition failure is not established, but absence is not proved",
        "current contract transfer does not certify safety", "", "", "", ""));
    assertThat(parser.reportedProofObligationCount(report)).isEqualTo(obligations.size());
    assertThat(parser.reportedFileCount(report)).isEqualTo(1);
    assertThat(parser.reportedSkippedCheckCount(report)).isEqualTo(-1);
    assertThat(obligations.getFirst().ruleId()).isEqualTo("proof-obligation:index-check");
    assertThat(obligations.getFirst().sonarMessage()).isEqualTo(
      "Proof obligation [index-check] unproved. Method: abstract-interpretation"
        + ". Why: index-check failure is not established, but absence is not proved"
        + ". Evidence: Index in 1 .. N"
        + ". Imprecision: index and bound ranges remain inconclusive"
        + ". Reason: widening-at-loop-header"
        + ". Blocked at: A (I)"
        + ". Inline path: demo.adb:12 -> demo.adb:18");
  }

  @Test
  void parsesAnalysisCoverageSummary() {
    String report = """
      Files scanned : 53
      Violations    : 1719
      Skipped checks: 5129 location(s) (semantic resolution limits; rerun with -v for details)
      """;

    assertThat(parser.reportedFileCount(report)).isEqualTo(53);
    assertThat(parser.reportedViolationCount(report)).isEqualTo(1719);
    assertThat(parser.reportedSkippedCheckCount(report)).isEqualTo(5129);
    assertThat(parser.reportedProofObligationCount(report)).isEqualTo(-1);
  }

  @Test
  void parsesRealAnalyzerConsoleOutputEvenWhenTruncatedMidFinding() {
    String report = readResource("adalanganalyzer/sparknacl-aes-console-output.txt");

    List<AdaLangAnalyzerFinding> findings = parser.parse(report);

    assertThat(findings).isNotEmpty();
    assertThat(findings)
      .extracting(AdaLangAnalyzerFinding::file)
      .allMatch(file -> file.equals("/opt/SPARKNaCl/src/sparknacl-aes.adb"));
    assertThat(findings.getFirst())
      .extracting(AdaLangAnalyzerFinding::ruleId, AdaLangAnalyzerFinding::line, AdaLangAnalyzerFinding::column)
      .containsExactly("No_Pragma", 6, 4);
    assertThat(findings.getLast().ruleId()).isEqualTo("Magic_Number");
    assertThat(parser.reportedFileCount(report)).isEqualTo(-1);
    assertThat(parser.reportedViolationCount(report)).isEqualTo(-1);
  }

  private static String readResource(String name) {
    try (InputStream stream = AdaLangAnalyzerConsoleParserTest.class.getClassLoader().getResourceAsStream(name)) {
      if (stream == null) {
        throw new IllegalStateException("Missing test resource: " + name);
      }
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
