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

  @Test
  void importsAdaLangAnalyzer160ProofObligationShapes() {
    // Obligations copied verbatim from an AdaLang Analyzer 1.6.0 --verify JSON report:
    // the new branch-budget-exceeded reason code, a discriminant check proved safe by
    // the new static-constraint proof path and its definite-error sibling, and an
    // obligation of a subprogram whose analysis failed partway, which 1.6.0 reports as
    // unsupported (1.5.x reported unreachable).
    String json = """
      {
        "analysisConfiguration": {"toolVersion": "1.6.0"},
        "proofSummary": {"scope": "bounded scalar verification; unsupported boundaries are explicit", \
      "total": 4, "provedSafe": 1, "definiteError": 1, "unproved": 1, "unreachable": 0, "unsupported": 1},
        "findings": [],
        "proofObligations": [
          {"id": "proof/v1/b632fd5101e68983", "kind": "loop-invariant-preservation", "status": "unproved", \
      "method": "abstract-interpretation", "file": "tests/verification_loop_branch_third_conditional_unsupported.adb", \
      "line": 14, "column": 10, "operation": "I >= 0 and then I <= 3 and then Y = X + I and then Extra >= 0", \
      "assumptions": "", "abstractState": "", "explanation": "the loop body does not establish invariant preservation", \
      "imprecisionSource": "the loop path has more independent conditionals than the branch budget folds", \
      "reasonCode": "branch-budget-exceeded", "blockingExpression": "X = 1", "inlinePath": "", "configurationId": "none"},
          {"id": "proof/v1/59b448246f406e5e", "kind": "discriminant-check", "status": "proved-safe", \
      "method": "static-evaluation", "file": "tests/verification_pp_discriminant.adb", "line": 34, "column": 12, \
      "operation": "Sized.Small", "assumptions": "", "abstractState": "selected variant declares the referenced component", \
      "explanation": "the object's discriminant constraint selects the component's variant", "imprecisionSource": "", \
      "reasonCode": "", "blockingExpression": "", "inlinePath": "", "configurationId": "none"},
          {"id": "proof/v1/514d61e337d963dd", "kind": "discriminant-check", "status": "definite-error", \
      "method": "static-evaluation", "file": "tests/verification_pp_discriminant.adb", "line": 35, "column": 12, \
      "operation": "Sized.Big", "assumptions": "", "abstractState": "selected variant excludes the referenced component", \
      "explanation": "component belongs to a variant excluded by the object's discriminant constraint", \
      "imprecisionSource": "", "reasonCode": "", "blockingExpression": "", "inlinePath": "", "configurationId": "none"},
          {"id": "proof/v1/f125c6c436db4ede", "kind": "integer-overflow", "status": "unsupported", "method": "none", \
      "file": "tests/verification_fp084_missing_spec.adb", "line": 9, "column": 15, "operation": "Z.F + 1", \
      "assumptions": "", "abstractState": "", "explanation": "overflow safety has not been established", \
      "imprecisionSource": "outside bounded verification subset", "reasonCode": "", "blockingExpression": "", \
      "inlinePath": "", "configurationId": "none"}
        ]
      }
      """;

    AdaLangAnalyzerReport report = parser.parse(AdaLangAnalyzerJson.mapOf(AdaLangAnalyzerJson.parse(json)));

    assertThat(report.proofObligationCount()).isEqualTo(4);
    assertThat(report.proofObligations()).hasSize(4);
    assertThat(report.proofObligations())
      .extracting(AdaLangAnalyzerProofObligation::isActionable)
      .containsExactly(true, false, true, false);

    AdaLangAnalyzerProofObligation budget = report.proofObligations().get(0);
    assertThat(budget.reasonCode()).isEqualTo("branch-budget-exceeded");
    assertThat(budget.sonarMessage())
      .contains("Reason: branch-budget-exceeded")
      .contains("Blocked at: X = 1");

    AdaLangAnalyzerProofObligation excluded = report.proofObligations().get(2);
    assertThat(excluded.ruleId()).isEqualTo("proof-obligation:discriminant-check");
    assertThat(excluded.outcome()).isEqualTo("definite-error");
  }

  @Test
  void importsAdaLangAnalyzer162ProofObligationShapes() {
    // Finding and obligations copied verbatim from an AdaLang Analyzer 1.6.2 --verify JSON
    // report. 1.6.1 stopped deciding an index check against the index subtype of an array
    // whose bounds no declaration fixes: such a check is proved only by the new own-range
    // proof path and is otherwise unproved with the missing-static-bounds reason code. An
    // index outside the object's own static constraint, proved safe up to 1.6.0, is now a
    // definite error that Known_Index_Check_Failure reports as a finding as well.
    String json = """
      {
        "analysisConfiguration": {"toolVersion": "1.6.2", "selectedPreset": "verify", "skippedChecks": 0},
        "filesScanned": 1,
        "newViolations": 1,
        "proofSummary": {"scope": "bounded scalar verification; unsupported boundaries are explicit", \
      "total": 3, "provedSafe": 1, "definiteError": 1, "unproved": 1, "unreachable": 0, "unsupported": 0},
        "findings": [
          {"ruleId": "Known_Index_Check_Failure", "message": "index is outside the array index subtype", \
      "explanation": "Abstract interpretation found that every represented index value is outside the array bounds.", \
      "evidence": "index range is outside the array bounds", "file": "src/bounds.adb", "line": 11, "column": 12, \
      "severity": "High", "quality": "Reliability", "fingerprint": "8bbcc0532a457a26", "baseline": false}
        ],
        "proofObligations": [
          {"id": "proof/v1/9d48c1d46761b9f2", "kind": "index-check", "status": "proved-safe", \
      "method": "static-evaluation", "file": "src/bounds.adb", "line": 7, "column": 10, "operation": "J", \
      "assumptions": "", "abstractState": "loop parameter ranges over the indexed object's bounds", \
      "explanation": "index is the parameter of a loop over this array's own range", "imprecisionSource": "", \
      "reasonCode": "", "blockingExpression": "", "inlinePath": "", "configurationId": "none"},
          {"id": "proof/v1/9a6a579df9875a90", "kind": "index-check", "status": "unproved", \
      "method": "abstract-interpretation", "file": "src/bounds.adb", "line": 10, "column": 7, "operation": "N", \
      "assumptions": "", "abstractState": "", \
      "explanation": "index-check failure is not established, but absence is not proved", \
      "imprecisionSource": "the required bounds are not statically known", "reasonCode": "missing-static-bounds", \
      "blockingExpression": "N", "inlinePath": "", "configurationId": "none"},
          {"id": "proof/v1/764d69392f924c45", "kind": "index-check", "status": "definite-error", \
      "method": "abstract-interpretation", "file": "src/bounds.adb", "line": 11, "column": 12, "operation": "20", \
      "assumptions": "", "abstractState": "index range is outside the array bounds", \
      "explanation": "index is outside the array index subtype", "imprecisionSource": "", "reasonCode": "", \
      "blockingExpression": "", "inlinePath": "", "configurationId": "none"}
        ]
      }
      """;

    AdaLangAnalyzerReport report = parser.parse(AdaLangAnalyzerJson.mapOf(AdaLangAnalyzerJson.parse(json)));

    assertThat(report.violationCount()).isEqualTo(1);
    assertThat(report.findings()).hasSize(1);
    assertThat(report.proofObligationCount()).isEqualTo(3);
    assertThat(report.proofObligations())
      .extracting(AdaLangAnalyzerProofObligation::isActionable)
      .containsExactly(false, true, true);

    assertThat(report.proofScope()).isEqualTo("bounded scalar verification; unsupported boundaries are explicit");
    assertThat(report.proofObligations()).extracting(report::isIssue).containsExactly(false, true, true);

    AdaLangAnalyzerProofObligation unknownBounds = report.proofObligations().get(1);
    assertThat(unknownBounds.sonarMessage())
      .contains("Imprecision: the required bounds are not statically known")
      .contains("Reason: missing-static-bounds")
      .contains("Blocked at: N");

    AdaLangAnalyzerProofObligation outsideConstraint = report.proofObligations().get(2);
    assertThat(outsideConstraint.outcome()).isEqualTo("definite-error");
    assertThat(outsideConstraint.line()).isEqualTo(report.findings().getFirst().line());
    assertThat(outsideConstraint.sonarMessage()).contains("Evidence: index range is outside the array bounds");
  }
}
