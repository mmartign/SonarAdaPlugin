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
  void readsGnatproveVerdictsAndCountsFromTheRunProperties() {
    // Obligation summaries and counts copied verbatim from the run properties of an AdaLang
    // Analyzer 1.8.4 --format=sarif report produced with --verify --gnatprove-log.
    String sarif = """
      {
        "version": "2.1.0",
        "$schema": "https://json.schemastore.org/sarif-2.1.0.json",
        "runs": [{
          "tool": {"driver": {"name": "AdaLang Analyzer", "rules": []}},
          "properties": {
            "proofObligations": [
              {"id": "proof/v1/54493de18b31b463", "kind": "division-by-zero", "status": "proved-safe", \
      "reasonCode": "", "blockingExpression": "", "inlinePath": "", "gnatprove": "proved"},
              {"id": "proof/v1/509a357295e4bad1", "kind": "division-by-zero", "status": "unproved", \
      "reasonCode": "", "blockingExpression": "", "inlinePath": "", "gnatprove": "not-proved"},
              {"id": "proof/v1/81577117d1d63a73", "kind": "integer-overflow", "status": "proved-safe", \
      "reasonCode": "", "blockingExpression": "", "inlinePath": ""},
              {"id": "proof/v1/d44c2209b745df62", "kind": "integer-overflow", "status": "unproved", \
      "reasonCode": "unsupported-call", "blockingExpression": "Data (Data'First)", "inlinePath": "", \
      "gnatprove": "proved"},
              {"id": "proof/v1/f7135e0426de4ca1", "kind": "termination", "status": "proved-safe", \
      "reasonCode": "", "blockingExpression": "", "inlinePath": "", "gnatprove": "proved"},
              {"id": "proof/v1/1a1cde184e601e05", "kind": "length-check", "status": "proved-safe", \
      "reasonCode": "", "blockingExpression": "", "inlinePath": "", "gnatprove": "proved"}
            ],
            "gnatproveImport": {"logs": ["tests/gnatprove_import/gnatprove.log"], "checks": 14, "proved": 11, \
      "justified": 0, "notProved": 3, "provedByBoth": 10, "provedByGnatproveOnObligation": 1, \
      "provedByGnatproveWithoutObligation": 0, "definiteErrorWhereGnatproveProved": 0, \
      "provedSafeWhereGnatproveNotProved": 0, "obligationsWithVerdict": 14},
            "analysisConfiguration": {"toolVersion": "1.8.4", "selectedPreset": "verify", "skippedChecks": 0}
          },
          "results": []
        }]
      }
      """;

    AdaLangAnalyzerReport report = parser.parse(AdaLangAnalyzerJson.mapOf(AdaLangAnalyzerJson.parse(sarif)));

    assertThat(report.proofObligations())
      .extracting(AdaLangAnalyzerProofObligation::gnatprove)
      .containsExactly("proved", "not-proved", "", "proved", "proved", "proved");
    // The unproved ones stay actionable whatever GNATprove said of their check.
    assertThat(report.proofObligations()).extracting(report::isIssue).containsExactly(false, true, false, true, false, false);
    assertThat(report.proofObligations().get(1).sonarMessage())
      .isEqualTo("Proof obligation [division-by-zero] unproved. GNATprove: not-proved");
    assertThat(report.gnatproveSummary()).contains(
      new AdaLangAnalyzerGnatproveSummary(1, 14, 11, 0, 3, 10, 1, 0, 0, 0, 14));
  }

  @Test
  void readsTheLengthChecksOfTheRunProperties() {
    // Obligation summaries copied verbatim from the run properties of an AdaLang Analyzer 1.8.4
    // --format=sarif report produced with --verify: the length-check kind 1.8.4 added.
    String sarif = """
      {
        "version": "2.1.0",
        "$schema": "https://json.schemastore.org/sarif-2.1.0.json",
        "runs": [{
          "tool": {"driver": {"name": "AdaLang Analyzer", "rules": []}},
          "properties": {
            "proofObligations": [
              {"id": "proof/v1/1507a9292cfdc9a3", "kind": "length-check", "status": "proved-safe", \
      "reasonCode": "", "blockingExpression": "", "inlinePath": ""},
              {"id": "proof/v1/43fc862f68af09f1", "kind": "length-check", "status": "unproved", \
      "reasonCode": "", "blockingExpression": "", "inlinePath": ""},
              {"id": "proof/v1/460c11b760569cb6", "kind": "length-check", "status": "unproved", \
      "reasonCode": "", "blockingExpression": "", "inlinePath": ""}
            ],
            "analysisConfiguration": {"toolVersion": "1.8.4", "selectedPreset": "verify", "skippedChecks": 0}
          },
          "results": []
        }]
      }
      """;

    AdaLangAnalyzerReport report = parser.parse(AdaLangAnalyzerJson.mapOf(AdaLangAnalyzerJson.parse(sarif)));

    assertThat(report.proofObligationCount()).isEqualTo(3);
    assertThat(report.proofObligations())
      .extracting(AdaLangAnalyzerProofObligation::ruleId)
      .containsOnly("proof-obligation:length-check");
    assertThat(report.proofObligations()).extracting(report::isIssue).containsExactly(false, true, true);
    assertThat(report.proofObligations().get(1).sonarMessage()).isEqualTo("Proof obligation [length-check] unproved");
    assertThat(report.gnatproveSummary()).isEmpty();
  }

  @Test
  void readsTheReadsOfAnOutActualItsCalleeMayNotHaveWritten() {
    // Obligation summaries copied verbatim from the run properties of an AdaLang Analyzer 1.8.5
    // --format=sarif report produced with --verify on the analyzer's own
    // tests/verification_fp117_unwritten_out.adb: the read of an out actual after a call whose
    // callee may not have written it, the read after a callee that never writes it, and the
    // range check on the value that read gives. 1.8.4 proved the two reads safe.
    String sarif = """
      {
        "version": "2.1.0",
        "$schema": "https://json.schemastore.org/sarif-2.1.0.json",
        "runs": [{
          "tool": {"driver": {"name": "AdaLang Analyzer", "rules": []}},
          "properties": {
            "proofObligations": [
              {"id": "proof/v1/743348d8701fca75", "kind": "initialization-check", "status": "unproved", \
      "reasonCode": "", "blockingExpression": "", "inlinePath": ""},
              {"id": "proof/v1/2026a19ddf3ed220", "kind": "initialization-check", "status": "definite-error", \
      "reasonCode": "", "blockingExpression": "", "inlinePath": ""},
              {"id": "proof/v1/81d09f99b177db1b", "kind": "range-check", "status": "unproved", \
      "reasonCode": "uninitialized-object", "blockingExpression": "Swallowed", "inlinePath": ""}
            ],
            "analysisConfiguration": {"toolVersion": "1.8.5", "selectedPreset": "verify", "skippedChecks": 0}
          },
          "results": []
        }]
      }
      """;

    AdaLangAnalyzerReport report = parser.parse(AdaLangAnalyzerJson.mapOf(AdaLangAnalyzerJson.parse(sarif)));

    assertThat(report.proofObligationCount()).isEqualTo(3);
    assertThat(report.proofObligations()).extracting(report::isIssue).containsExactly(true, true, true);
    assertThat(report.proofObligations())
      .extracting(AdaLangAnalyzerProofObligation::isDefiniteError)
      .containsExactly(false, true, false);
    assertThat(report.proofObligations().get(1).sonarMessage())
      .isEqualTo("Proof obligation [initialization-check] definite-error");
    assertThat(report.proofObligations().get(2).sonarMessage())
      .isEqualTo("Proof obligation [range-check] unproved. Reason: uninitialized-object. Blocked at: Swallowed");
  }

  @Test
  void readsAComponentReadInTwoElementsOfAnArray() {
    // Obligation summaries copied verbatim from the run properties of an AdaLang Analyzer 1.8.6
    // --format=sarif report produced with --verify on the analyzer's own
    // tests/verification_fp119_element_component.ads and .adb: an assertion that two elements of
    // an array hold the same count, a division by a component of one under a condition on two,
    // an assertion on one element, and the overflow check on the quotient. 1.8.5 proved the
    // first two safe.
    String sarif = """
      {
        "version": "2.1.0",
        "$schema": "https://json.schemastore.org/sarif-2.1.0.json",
        "runs": [{
          "tool": {"driver": {"name": "AdaLang Analyzer", "rules": []}},
          "properties": {
            "proofObligations": [
              {"id": "proof/v1/e13a45eb1272d0aa", "kind": "assertion", "status": "unproved", \
      "reasonCode": "", "blockingExpression": "", "inlinePath": ""},
              {"id": "proof/v1/78d0a2a2af2c0d44", "kind": "division-by-zero", "status": "unproved", \
      "reasonCode": "", "blockingExpression": "", "inlinePath": ""},
              {"id": "proof/v1/86646668761dc1f7", "kind": "assertion", "status": "proved-safe", \
      "reasonCode": "", "blockingExpression": "", "inlinePath": ""},
              {"id": "proof/v1/0a57515517dbb62c", "kind": "integer-overflow", "status": "unproved", \
      "reasonCode": "unsafe-divisor-semantics", "blockingExpression": "Share / Mixed (2).Limit", "inlinePath": ""}
            ],
            "analysisConfiguration": {"toolVersion": "1.8.6", "selectedPreset": "verify", "skippedChecks": 0}
          },
          "results": []
        }]
      }
      """;

    AdaLangAnalyzerReport report = parser.parse(AdaLangAnalyzerJson.mapOf(AdaLangAnalyzerJson.parse(sarif)));

    assertThat(report.proofObligationCount()).isEqualTo(4);
    assertThat(report.proofObligations()).extracting(report::isIssue).containsExactly(true, true, false, true);
    assertThat(report.proofObligations())
      .extracting(AdaLangAnalyzerProofObligation::ruleId)
      .containsExactly(
        "proof-obligation:assertion", "proof-obligation:division-by-zero", "proof-obligation:assertion",
        "proof-obligation:integer-overflow");
    assertThat(report.proofObligations().get(1).sonarMessage())
      .isEqualTo("Proof obligation [division-by-zero] unproved");
    assertThat(report.proofObligations().get(3).sonarMessage()).isEqualTo(
      "Proof obligation [integer-overflow] unproved. Reason: unsafe-divisor-semantics"
        + ". Blocked at: Share / Mixed (2).Limit");
  }

  @Test
  void reportsNoGnatproveSummaryForARunWithoutALog() {
    Map<String, Object> root = AdaLangAnalyzerJson.mapOf(AdaLangAnalyzerJson.parse(
      "{\"runs\": [{\"properties\": {\"proofObligations\": []}, \"results\": []}]}"));

    assertThat(parser.parse(root).gnatproveSummary()).isEmpty();
  }

  @Test
  void toleratesMissingRuns() {
    Map<String, Object> root = AdaLangAnalyzerJson.mapOf(AdaLangAnalyzerJson.parse("{\"runs\": []}"));

    AdaLangAnalyzerReport report = parser.parse(root);

    assertThat(report.findings()).isEmpty();
    assertThat(report.proofObligations()).isEmpty();
  }
}
