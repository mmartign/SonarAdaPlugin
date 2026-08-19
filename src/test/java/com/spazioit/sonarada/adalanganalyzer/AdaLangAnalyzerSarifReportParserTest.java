/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.spazioit.sonarada.adalanganalyzer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class AdaLangAnalyzerSarifReportParserTest {
  private final AdaLangAnalyzerSarifReportParser parser = new AdaLangAnalyzerSarifReportParser();

  @Test
  void parsesResultsWithRuleCatalogAndSkipsBaselineMatches() {
    String sarif = """
      {
        "version": "2.1.0",
        "$schema": "https://json.schemastore.org/sarif-2.1.0.json",
        "runs": [{
          "tool": {"driver": {"name": "AdaLang Analyzer", "rules": [
            {"id": "No_Goto", "shortDescription": {"text": "Avoid unstructured control flow"}, \
      "help": {"text": "Replace goto with structured statements"}},
            {"id": "Division_By_Zero", "shortDescription": {"text": "Find divisions whose divisor may be zero"}, \
      "help": {"text": "Guard the division"}}
          ]}},
          "properties": {"proofObligations": [
            {"id": "proof/v1/abc", "kind": "division-by-zero-check", "status": "definite-error", \
      "reasonCode": "constant-propagation", "blockingExpression": "X / Y", "inlinePath": "demo.adb:5 -> demo.adb:20"},
            {"id": "proof/v1/def", "kind": "range-check", "status": "proved-safe", \
      "reasonCode": "", "blockingExpression": "", "inlinePath": ""}
          ]},
          "results": [
            {"ruleId": "No_Goto", "level": "warning", "message": {"text": "goto statements are forbidden"}, \
      "baselineState": "new", "properties": {"explanation": "control flow becomes unstructured", "evidence": "goto Finished;"}, \
      "locations": [{"physicalLocation": {"artifactLocation": {"uri": "/project/src/demo.adb"}, \
      "region": {"startLine": 12, "startColumn": 7}}}]},
            {"ruleId": "Division_By_Zero", "level": "error", "message": {"text": "division may fail"}, \
      "baselineState": "new", "properties": {"explanation": "", "evidence": ""}, \
      "locations": [{"physicalLocation": {"artifactLocation": {"uri": "src%2Fother%20file.adb"}, \
      "region": {"startLine": 5, "startColumn": 2}}}]},
            {"ruleId": "No_Pragma", "level": "note", "message": {"text": "already accepted"}, \
      "baselineState": "unchanged", "properties": {}, \
      "locations": [{"physicalLocation": {"artifactLocation": {"uri": "/project/src/demo.adb"}, \
      "region": {"startLine": 1, "startColumn": 1}}}]}
          ]
        }]
      }
      """;

    Map<String, Object> root = AdaLangAnalyzerJson.mapOf(AdaLangAnalyzerJson.parse(sarif));
    AdaLangAnalyzerReport report = parser.parse(root);

    // The "unchanged" baseline-state result is already accepted and must be excluded, matching
    // how console-text output hides baseline matches.
    assertThat(report.findings())
      .extracting(AdaLangAnalyzerFinding::ruleId)
      .containsExactly("No_Goto", "Division_By_Zero");

    AdaLangAnalyzerFinding first = report.findings().getFirst();
    assertThat(first.ruleDescription()).isEqualTo("Avoid unstructured control flow");
    assertThat(first.advice()).isEqualTo("Replace goto with structured statements");
    assertThat(first.explanation()).isEqualTo("control flow becomes unstructured");
    assertThat(first.qualitySeverity()).isEqualTo("Medium");

    AdaLangAnalyzerFinding second = report.findings().get(1);
    assertThat(second.file()).isEqualTo("src/other file.adb");
    assertThat(second.qualitySeverity()).isEqualTo("High");

    // SARIF's properties.proofObligations summary array has no file/line/column, so obligations
    // count towards the total but carry an empty location.
    assertThat(report.proofObligations()).hasSize(2);
    AdaLangAnalyzerProofObligation definiteError = report.proofObligations().getFirst();
    assertThat(definiteError.file()).isEmpty();
    assertThat(definiteError.kind()).isEqualTo("division-by-zero-check");
    assertThat(definiteError.outcome()).isEqualTo("definite-error");
    assertThat(definiteError.reasonCode()).isEqualTo("constant-propagation");
    assertThat(definiteError.blockingExpression()).isEqualTo("X / Y");
    assertThat(definiteError.inlinePath()).isEqualTo("demo.adb:5 -> demo.adb:20");
    assertThat(definiteError.isActionable()).isTrue();
    assertThat(report.proofObligations().get(1).isActionable()).isFalse();

    // SARIF reports no file/violation/skipped-check summary counts.
    assertThat(report.fileCount()).isEqualTo(-1);
    assertThat(report.violationCount()).isEqualTo(-1);
    assertThat(report.proofObligationCount()).isEqualTo(2);
    assertThat(report.skippedCheckCount()).isEqualTo(-1);
  }

  @Test
  void toleratesMissingRuns() {
    Map<String, Object> root = AdaLangAnalyzerJson.mapOf(AdaLangAnalyzerJson.parse("{\"runs\": []}"));

    AdaLangAnalyzerReport report = parser.parse(root);

    assertThat(report.findings()).isEmpty();
    assertThat(report.proofObligations()).isEmpty();
  }
}
