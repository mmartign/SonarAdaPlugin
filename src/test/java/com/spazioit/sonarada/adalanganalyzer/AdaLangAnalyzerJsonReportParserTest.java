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

  @Test
  void importsFindingsOfTheCodingStandardChecks() {
    // Findings copied verbatim from an AdaLang Analyzer JSON report produced with the
    // opt-in coding-standard checks and -rule-param: one located at a token rather than
    // a syntax node (End_Of_Line_Comment, Maximum_Lines), one whose message carries a
    // check parameter (Identifier_Casing), and two located at a node.
    String json = """
      {
        "analysisConfiguration": {"toolVersion": "1.7.0", "selectedPreset": "none", \
      "enabledRules": ["No_Use_Package_Clause", "End_Of_Line_Comment", "Maximum_Lines", "Positional_Parameter", \
      "Identifier_Casing"], "skippedChecks": 0},
        "findings": [
          {"ruleId": "Maximum_Lines", "message": "file has 26 lines, more than 5", "explanation": "", "evidence": "", \
      "file": "tests/precision_positional_pkg.ads", "line": 25, "column": 30, "severity": "Medium", \
      "quality": "Maintainability", "fingerprint": "7b70b98e09db3c5b", "baseline": false},
          {"ruleId": "Identifier_Casing", "message": "Zero does not have the casing required for constants (upper)", \
      "explanation": "", "evidence": "", "file": "tests/precision_positional_pkg.ads", "line": 8, "column": 7, \
      "severity": "Low", "quality": "Maintainability", "fingerprint": "9013cc96973ed580", "baseline": false},
          {"ruleId": "No_Use_Package_Clause", "message": "use clause for a package", "explanation": "", "evidence": "", \
      "file": "tests/precision_positional_parameter_finding.adb", "line": 1, "column": 32, "severity": "Low", \
      "quality": "Maintainability", "fingerprint": "c2b263bc13438136", "baseline": false},
          {"ruleId": "Positional_Parameter", "message": "positional parameter association", "explanation": "", \
      "evidence": "", "file": "tests/precision_positional_parameter_finding.adb", "line": 5, "column": 10, \
      "severity": "Low", "quality": "Maintainability", "fingerprint": "c40806d5f782f215", "baseline": false},
          {"ruleId": "End_Of_Line_Comment", "message": "end of line comment", "explanation": "", "evidence": "", \
      "file": "tests/precision_end_of_line_comment_finding.adb", "line": 3, "column": 11, "severity": "Low", \
      "quality": "Maintainability", "fingerprint": "7fe1bd00517f8611", "baseline": false}
        ]
      }
      """;

    AdaLangAnalyzerReport report = parser.parse(AdaLangAnalyzerJson.mapOf(AdaLangAnalyzerJson.parse(json)));

    assertThat(report.findings())
      .extracting(AdaLangAnalyzerFinding::ruleId)
      .containsExactly(
        "Maximum_Lines", "Identifier_Casing", "No_Use_Package_Clause", "Positional_Parameter", "End_Of_Line_Comment");
    assertThat(report.skippedCheckCount()).isZero();

    AdaLangAnalyzerFinding casing = report.findings().get(1);
    assertThat(casing.file()).isEqualTo("tests/precision_positional_pkg.ads");
    assertThat(casing.line()).isEqualTo(8);
    assertThat(casing.column()).isEqualTo(7);
    assertThat(casing.softwareQuality()).isEqualTo("Maintainability");
    assertThat(casing.qualitySeverity()).isEqualTo("Low");
    assertThat(casing.sonarMessage()).isEqualTo("Zero does not have the casing required for constants (upper)");

    AdaLangAnalyzerFinding fileLength = report.findings().getFirst();
    assertThat(fileLength.qualitySeverity()).isEqualTo("Medium");
    assertThat(fileLength.line()).isEqualTo(25);
  }

  @Test
  void importsTheObligationsAboutASubprogramAsAWhole() {
    // Obligations copied verbatim from an AdaLang Analyzer 1.8.3 --verify JSON report: the
    // termination, data-dependencies, and flow-dependencies kinds 1.8.1 added. A
    // flow-dependencies obligation is always unproved, with the method none: no route proves a
    // Depends aspect yet. It is an issue like the other unproved ones.
    String json = """
      {
        "analysisConfiguration": {"toolVersion": "1.8.3", "selectedPreset": "verify", "skippedChecks": 0},
        "proofSummary": {"scope": "bounded scalar verification; unsupported boundaries are explicit", \
      "total": 5, "provedSafe": 2, "definiteError": 0, "unproved": 3, "unreachable": 0, "unsupported": 0},
        "findings": [],
        "proofObligations": [
          {"id": "proof/v1/611ab10cb7c6015d", "kind": "termination", "status": "proved-safe", \
      "method": "flow-analysis", "file": "tests/verification_actual_range.adb", "line": 22, "column": 13, \
      "operation": "Twice", "assumptions": "", \
      "abstractState": "no unbounded loop, no recursion, every callee terminates", \
      "explanation": "the subprogram returns: its loops are bounded, it is not recursive and what it calls returns", \
      "imprecisionSource": "", "reasonCode": "", "blockingExpression": "", "inlinePath": "", "configurationId": "none"},
          {"id": "proof/v1/cc2b6caa7911fe47", "kind": "termination", "status": "unproved", \
      "method": "flow-analysis", "file": "tests/verification_mutation_call_effects.adb", "line": 65, "column": 13, \
      "operation": "In_Loop", "assumptions": "", "abstractState": "", \
      "explanation": "the subprogram is not shown to return", \
      "imprecisionSource": "a loop that is not a for loop is not shown to end", "reasonCode": "", \
      "blockingExpression": "", "inlinePath": "", "configurationId": "none"},
          {"id": "proof/v1/ec08c9dd2d3fbb44", "kind": "data-dependencies", "status": "proved-safe", \
      "method": "flow-analysis", "file": "tests/verification_actual_range.adb", "line": 16, "column": 24, \
      "operation": "Global of Take", "assumptions": "", \
      "abstractState": "every object the body may read or write is allowed by the aspect", \
      "explanation": "the subprogram reads and writes no outside object its Global aspect does not allow", \
      "imprecisionSource": "", "reasonCode": "", "blockingExpression": "", "inlinePath": "", "configurationId": "none"},
          {"id": "proof/v1/81a79a11ac05d76a", "kind": "data-dependencies", "status": "unproved", \
      "method": "flow-analysis", "file": "tests/verification_global_aspect_reference_clean.adb", "line": 5, \
      "column": 11, "operation": "Global of Bump", "assumptions": "", "abstractState": "", \
      "explanation": "the Global aspect is not shown to cover what the subprogram reads and writes", \
      "imprecisionSource": "Arg is used and is not listed", "reasonCode": "", "blockingExpression": "", \
      "inlinePath": "", "configurationId": "none"},
          {"id": "proof/v1/dda1f8dd910d3305", "kind": "flow-dependencies", "status": "unproved", "method": "none", \
      "file": "tests/verification_flow_contracts.adb", "line": 40, "column": 11, "operation": "Depends of Scaled", \
      "assumptions": "", "abstractState": "", \
      "explanation": "the Depends aspect is not shown to be the dependencies of the subprogram", \
      "imprecisionSource": "information flow is not yet analyzed to the point of proof", "reasonCode": "", \
      "blockingExpression": "", "inlinePath": "", "configurationId": "none"}
        ]
      }
      """;

    AdaLangAnalyzerReport report = parser.parse(AdaLangAnalyzerJson.mapOf(AdaLangAnalyzerJson.parse(json)));

    assertThat(report.proofObligations()).hasSize(report.proofObligationCount());
    assertThat(report.proofObligations())
      .extracting(AdaLangAnalyzerProofObligation::ruleId)
      .containsExactly(
        "proof-obligation:termination", "proof-obligation:termination", "proof-obligation:data-dependencies",
        "proof-obligation:data-dependencies", "proof-obligation:flow-dependencies");
    assertThat(report.proofObligations()).extracting(report::isIssue).containsExactly(false, true, false, true, true);
    assertThat(report.unattemptedProofCount()).isZero();
    assertThat(report.gnatproveSummary()).isEmpty();
    assertThat(report.proofObligations().get(4).sonarMessage()).isEqualTo(
      "Proof obligation [flow-dependencies] unproved. Operation: Depends of Scaled. Method: none"
        + ". Why: the Depends aspect is not shown to be the dependencies of the subprogram"
        + ". Imprecision: information flow is not yet analyzed to the point of proof");

    assertThat(report.proofObligations().get(3).sonarMessage()).isEqualTo(
      "Proof obligation [data-dependencies] unproved. Operation: Global of Bump. Method: flow-analysis"
        + ". Why: the Global aspect is not shown to cover what the subprogram reads and writes"
        + ". Imprecision: Arg is used and is not listed");
  }

  @Test
  void importsGnatproveVerdictsAndSubjectsFromARealReport() {
    // The complete --format=json report of AdaLang Analyzer 1.8.4 run with --verify
    // --gnatprove-log on the analyzer's own tests/gnatprove_import fixture.
    AdaLangAnalyzerReport report = parser.parse(AdaLangAnalyzerJson.mapOf(
      AdaLangAnalyzerJson.parse(readResource("adalanganalyzer/gnatprove-import-report.json"))));

    assertThat(report.findings()).hasSize(3).hasSize(report.violationCount());
    assertThat(report.proofObligations()).hasSize(27).hasSize(report.proofObligationCount());
    assertThat(report.gnatproveSummary()).contains(
      new AdaLangAnalyzerGnatproveSummary(1, 14, 11, 0, 3, 10, 1, 0, 0, 0, 14));
    assertThat(report.proofObligations())
      .filteredOn(obligation -> !obligation.gnatprove().isBlank())
      .hasSize(report.gnatproveSummary().orElseThrow().obligationsWithVerdict());

    // The addition only GNATprove proves keeps the analyzer's own status, and stays an issue
    // that says what GNATprove said.
    AdaLangAnalyzerProofObligation overflow = report.proofObligations().get(9);
    assertThat(overflow.kind()).isEqualTo("integer-overflow");
    assertThat(overflow.outcome()).isEqualTo("unproved");
    assertThat(overflow.gnatprove()).isEqualTo("proved");
    assertThat(report.isIssue(overflow)).isTrue();
    assertThat(overflow.sonarMessage())
      .startsWith("Proof obligation [integer-overflow] unproved. Operation: Data (Data'First) + Data (Data'Last)"
        + ". Method: abstract-interpretation. GNATprove: proved. Why: ");

    // Every unproved obligation and the definite error are issues, with or without a verdict.
    assertThat(report.proofObligations())
      .filteredOn(report::isIssue)
      .extracting(AdaLangAnalyzerProofObligation::outcome, AdaLangAnalyzerProofObligation::gnatprove)
      .containsExactly(
        org.assertj.core.groups.Tuple.tuple("unproved", "not-proved"),
        org.assertj.core.groups.Tuple.tuple("unproved", "not-proved"),
        org.assertj.core.groups.Tuple.tuple("unproved", "proved"),
        org.assertj.core.groups.Tuple.tuple("definite-error", "not-proved"),
        org.assertj.core.groups.Tuple.tuple("unproved", ""),
        org.assertj.core.groups.Tuple.tuple("unproved", ""),
        org.assertj.core.groups.Tuple.tuple("unproved", ""));
    assertThat(report.proofObligations().get(4).sonarMessage()).isEqualTo(
      "Proof obligation [integer-overflow] unproved. Operation: Left / Right. Method: abstract-interpretation"
        + ". GNATprove: not-proved"
        + ". Why: overflow is not established, but absence is not proved"
        + ". Imprecision: Ada division semantics require a provably nonzero divisor"
        + ". Reason: unsafe-divisor-semantics"
        + ". Blocked at: Left / Right");

    // An initialization check is located at a read and names the declaration of the object read.
    AdaLangAnalyzerProofObligation read = report.proofObligations().get(18);
    assertThat(read.kind()).isEqualTo("initialization-check");
    assertThat(read.file()).isEqualTo("tests/gnatprove_import/sample.adb");
    assertThat(read.line()).isEqualTo(5);
    assertThat(read.column()).isEqualTo(17);
    assertThat(read.subjectFile()).isEqualTo("tests/gnatprove_import/sample.adb");
    assertThat(read.subjectLine()).isEqualTo(3);
    assertThat(read.subjectColumn()).isEqualTo(20);
    assertThat(read.hasSeparateSubject()).isTrue();

    // An out parameter checked at its subprogram's exit is located at its own declaration.
    AdaLangAnalyzerProofObligation outParameter = report.proofObligations().get(21);
    assertThat(outParameter.operation()).isEqualTo("Target");
    assertThat(outParameter.subjectLine()).isEqualTo(outParameter.line());
    assertThat(outParameter.hasSeparateSubject()).isFalse();

    // Only initialization checks have a subject.
    assertThat(report.proofObligations())
      .filteredOn(obligation -> !obligation.subjectFile().isBlank())
      .hasSize(10)
      .allMatch(obligation -> obligation.kind().equals("initialization-check"));
  }

  @Test
  void keepsTheAnalyzersOwnResultWhereGnatproveDisagrees() {
    // Obligations and counts copied verbatim from an AdaLang Analyzer 1.8.3 JSON report, for the
    // hand-written log of the analyzer's tests that goes against its own results
    // (tests/gnatprove_import/made_up.log): a proof GNATprove is said not to have, a justified
    // check, and a definite error GNATprove is said to prove.
    String json = """
      {
        "analysisConfiguration": {"toolVersion": "1.8.3", "selectedPreset": "verify", "skippedChecks": 0},
        "proofSummary": {"scope": "bounded scalar verification; unsupported boundaries are explicit", \
      "total": 3, "provedSafe": 1, "definiteError": 1, "unproved": 1, "unreachable": 0, "unsupported": 0},
        "findings": [],
        "proofObligations": [
          {"id": "proof/v1/54493de18b31b463", "kind": "division-by-zero", "status": "proved-safe", \
      "method": "abstract-interpretation", "file": "tests/gnatprove_import/sample.ads", "line": 9, "column": 63, \
      "operation": "2", "assumptions": "", "abstractState": "right operand is strictly negative or positive", \
      "explanation": "right operand range excludes zero", "imprecisionSource": "", "reasonCode": "", \
      "blockingExpression": "", "inlinePath": "", "configurationId": "none", "gnatprove": "not-proved"},
          {"id": "proof/v1/509a357295e4bad1", "kind": "division-by-zero", "status": "unproved", \
      "method": "abstract-interpretation", "file": "tests/gnatprove_import/sample.ads", "line": 13, "column": 14, \
      "operation": "Right", "assumptions": "", "abstractState": "", \
      "explanation": "zero has not been excluded from the divisor", \
      "imprecisionSource": "the divisor range is unknown or contains zero", "reasonCode": "", \
      "blockingExpression": "", "inlinePath": "", "configurationId": "none", "gnatprove": "justified"},
          {"id": "proof/v1/220511861509692d", "kind": "division-by-zero", "status": "definite-error", \
      "method": "abstract-interpretation", "file": "tests/gnatprove_import/sample.adb", "line": 11, "column": 22, \
      "operation": "Zero", "assumptions": "", "abstractState": "right operand => 0", \
      "explanation": "right operand is zero in the incoming abstract state", "imprecisionSource": "", \
      "reasonCode": "", "blockingExpression": "", "inlinePath": "", "configurationId": "none", "gnatprove": "proved"}
        ],
        "gnatproveImport": {"logs": ["tests/gnatprove_import/made_up.log"], "checks": 7, "proved": 4, \
      "justified": 1, "notProved": 2, "provedByBoth": 1, "provedByGnatproveOnObligation": 0, \
      "provedByGnatproveWithoutObligation": 2, "definiteErrorWhereGnatproveProved": 1, \
      "provedSafeWhereGnatproveNotProved": 1, "obligationsWithVerdict": 5},
        "gnatproveChecks": [
          {"file": "sample.adb", "line": 11, "column": 20, "check": "division check", "kind": "division-by-zero", \
      "verdict": "proved", "instances": 1, "adalang": "definite-error", "obligation": "proof/v1/220511861509692d"},
          {"file": "other.adb", "line": 3, "column": 4, "check": "range check", "kind": "range-check", \
      "verdict": "proved", "instances": 1, "adalang": "file without any obligation", "obligation": null}
        ]
      }
      """;

    AdaLangAnalyzerReport report = parser.parse(AdaLangAnalyzerJson.mapOf(AdaLangAnalyzerJson.parse(json)));

    // The checks of the log are GNATprove's and are not findings.
    assertThat(report.findings()).isEmpty();
    assertThat(report.proofObligations()).extracting(report::isIssue).containsExactly(false, true, true);
    assertThat(report.proofObligations().get(2).sonarMessage())
      .startsWith("Proof obligation [division-by-zero] definite-error. Operation: Zero")
      .contains("GNATprove: proved");

    AdaLangAnalyzerGnatproveSummary summary = report.gnatproveSummary().orElseThrow();
    assertThat(summary.definiteErrorWhereGnatproveProved()).isEqualTo(1);
    assertThat(summary.provedSafeWhereGnatproveNotProved()).isEqualTo(1);
    assertThat(summary.justified()).isEqualTo(1);
  }

  @Test
  void importsTheLengthChecksOfARealReport() {
    // The complete --format=json report of AdaLang Analyzer 1.8.4 run with --verify on the
    // analyzer's own tests/verification_length_check_state.adb: the length-check kind 1.8.4
    // added.
    AdaLangAnalyzerReport report = parser.parse(AdaLangAnalyzerJson.mapOf(
      AdaLangAnalyzerJson.parse(readResource("adalanganalyzer/length-check-report.json"))));

    assertThat(report.findings()).hasSize(3).hasSize(report.violationCount());
    assertThat(report.proofObligations()).hasSize(47).hasSize(report.proofObligationCount());
    assertThat(report.gnatproveSummary()).isEmpty();

    List<AdaLangAnalyzerProofObligation> lengthChecks = report.proofObligations().stream()
      .filter(obligation -> obligation.kind().equals("length-check"))
      .toList();
    // The unproved ones are issues, under a rule of their own.
    assertThat(lengthChecks)
      .extracting(AdaLangAnalyzerProofObligation::outcome)
      .containsExactly(
        "proved-safe", "unproved", "unproved", "unproved", "unproved", "proved-safe", "unproved");
    assertThat(lengthChecks).filteredOn(report::isIssue).hasSize(5);
    assertThat(lengthChecks)
      .extracting(AdaLangAnalyzerProofObligation::ruleId)
      .containsOnly("proof-obligation:length-check");

    // An assignment to a slice has a check at the value and one of its own, which the analyzer
    // reports at the ":=" and whose operation is the whole statement.
    assertThat(lengthChecks.get(1))
      .extracting(
        AdaLangAnalyzerProofObligation::line, AdaLangAnalyzerProofObligation::column,
        AdaLangAnalyzerProofObligation::operation)
      .containsExactly(18, 27, "Local");
    assertThat(lengthChecks.get(2))
      .extracting(
        AdaLangAnalyzerProofObligation::line, AdaLangAnalyzerProofObligation::column,
        AdaLangAnalyzerProofObligation::operation)
      .containsExactly(18, 24, "Goal (1 .. Size) := Local;");
    assertThat(lengthChecks.get(2).sonarMessage()).isEqualTo(
      "Proof obligation [length-check] unproved. Operation: Goal (1 .. Size) := Local;"
        + ". Method: abstract-interpretation"
        + ". Why: length-check failure is not established, but absence is not proved"
        + ". Imprecision: the two lengths are not known to be equal");
    assertThat(lengthChecks.getFirst().sonarMessage()).isEqualTo(
      "Proof obligation [length-check] proved-safe. Operation: (others => 0). Method: abstract-interpretation"
        + ". Why: an aggregate with an others choice has the bounds of what it is given to"
        + ". Evidence: the value takes the bounds of its target");
    // A length check is about no declared object.
    assertThat(lengthChecks).noneMatch(AdaLangAnalyzerProofObligation::hasSeparateSubject);
  }

  private static String readResource(String name) {
    try (InputStream stream = AdaLangAnalyzerJsonReportParserTest.class.getClassLoader().getResourceAsStream(name)) {
      if (stream == null) {
        throw new IllegalStateException("Missing test resource: " + name);
      }
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
