/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.spazioit.sonarada.adalanganalyzer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class AdaLangAnalyzerJsonReportParserTest {
  private final AdaLangAnalyzerJsonReportParser parser = new AdaLangAnalyzerJsonReportParser();

  @Test
  void parsesFindingsProofObligationsAndSummaryCounts() {
    String json = """
      {
        "version": "1.0",
        "analysisConfiguration": {
          "toolVersion": "1.0.0-rc1",
          "skippedChecks": 3
        },
        "filesScanned": 1,
        "newViolations": 2,
        "baselineMatches": 1,
        "proofSummary": {"scope": "bounded scalar checks", "total": 3, "provedSafe": 1, "definiteError": 1, "unproved": 1},
        "findings": [
          {"ruleId": "No_Goto", "message": "goto statements are forbidden", "explanation": "control flow becomes unstructured", \
      "evidence": "goto Finished;", "file": "/project/src/demo.adb", "line": 12, "column": 7, "severity": "Medium", \
      "quality": "Maintainability", "fingerprint": "abc123", "baseline": false},
          {"ruleId": "Division_By_Zero", "message": "division may fail", "explanation": "", "evidence": "", \
      "file": "/project/src/demo.adb", "line": 20, "column": 3, "severity": "High", "quality": "Reliability", \
      "fingerprint": "def456", "baseline": false},
          {"ruleId": "No_Pragma", "message": "already-accepted finding", "explanation": "", "evidence": "", \
      "file": "/project/src/demo.adb", "line": 1, "column": 1, "severity": "Low", "quality": "Maintainability", \
      "fingerprint": "ghi789", "baseline": true}
        ],
        "proofObligations": [
          {"id": "proof/v1/abc", "kind": "division-by-zero-check", "status": "definite-error", \
      "method": "static-evaluation", "file": "/project/src/demo.adb", "line": 20, "column": 3, "operation": "X / Y", \
      "assumptions": "", "abstractState": "Y = 0", "explanation": "divisor is always zero", "imprecisionSource": "", \
      "reasonCode": "constant-propagation", "blockingExpression": "X / Y", "inlinePath": "demo.adb:5 -> demo.adb:20", \
      "configurationId": ""},
          {"id": "proof/v1/def", "kind": "range-check", "status": "unproved", "method": "abstract-interpretation", \
      "file": "/project/src/demo.adb", "line": 30, "column": 5, "operation": "A(I)", "assumptions": "", \
      "abstractState": "", "explanation": "bound not established", "imprecisionSource": "loop widening", \
      "reasonCode": "", "blockingExpression": "", "inlinePath": "", "configurationId": ""},
          {"id": "proof/v1/ghi", "kind": "index-check", "status": "proved-safe", "method": "flow-analysis", \
      "file": "/project/src/demo.adb", "line": 40, "column": 2, "operation": "B(J)", "assumptions": "", \
      "abstractState": "", "explanation": "", "imprecisionSource": "", \
      "reasonCode": "", "blockingExpression": "", "inlinePath": "", "configurationId": ""}
        ]
      }
      """;

    Map<String, Object> root = AdaLangAnalyzerJson.mapOf(AdaLangAnalyzerJson.parse(json));
    AdaLangAnalyzerReport report = parser.parse(root);

    // The baseline:true finding is already accepted and must be excluded, matching how
    // console-text output hides baseline matches.
    assertThat(report.findings())
      .extracting(AdaLangAnalyzerFinding::ruleId)
      .containsExactly("No_Goto", "Division_By_Zero");

    AdaLangAnalyzerFinding first = report.findings().getFirst();
    assertThat(first.file()).isEqualTo("/project/src/demo.adb");
    assertThat(first.line()).isEqualTo(12);
    assertThat(first.column()).isEqualTo(7);
    assertThat(first.explanation()).isEqualTo("control flow becomes unstructured");
    assertThat(first.evidence()).isEqualTo("goto Finished;");
    assertThat(first.softwareQuality()).isEqualTo("Maintainability");
    assertThat(first.qualitySeverity()).isEqualTo("Medium");

    assertThat(report.proofObligations()).hasSize(3);
    AdaLangAnalyzerProofObligation definiteError = report.proofObligations().get(0);
    assertThat(definiteError.kind()).isEqualTo("division-by-zero-check");
    assertThat(definiteError.outcome()).isEqualTo("definite-error");
    assertThat(definiteError.isActionable()).isTrue();
    assertThat(definiteError.evidence()).isEqualTo("Y = 0");
    assertThat(definiteError.reasonCode()).isEqualTo("constant-propagation");
    assertThat(definiteError.blockingExpression()).isEqualTo("X / Y");
    assertThat(definiteError.inlinePath()).isEqualTo("demo.adb:5 -> demo.adb:20");
    assertThat(report.proofObligations().get(2).isActionable()).isFalse();

    assertThat(report.fileCount()).isEqualTo(1);
    assertThat(report.violationCount()).isEqualTo(2);
    assertThat(report.proofObligationCount()).isEqualTo(3);
    assertThat(report.skippedCheckCount()).isEqualTo(3);
  }

  @Test
  void toleratesMissingOptionalSections() {
    Map<String, Object> root = AdaLangAnalyzerJson.mapOf(AdaLangAnalyzerJson.parse("{\"findings\": []}"));

    AdaLangAnalyzerReport report = parser.parse(root);

    assertThat(report.findings()).isEmpty();
    assertThat(report.proofObligations()).isEmpty();
    assertThat(report.fileCount()).isEqualTo(-1);
    assertThat(report.violationCount()).isEqualTo(-1);
    assertThat(report.proofObligationCount()).isEqualTo(-1);
    assertThat(report.skippedCheckCount()).isEqualTo(-1);
  }
}
