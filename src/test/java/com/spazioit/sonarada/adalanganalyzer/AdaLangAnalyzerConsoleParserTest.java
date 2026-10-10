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

  @Test
  void parsesGnatproveVerdictsAndTheirSummaryFromRealAnalyzerOutput() {
    // The complete output of AdaLang Analyzer 1.8.6 run with --verify -v --gnatprove-log on the
    // analyzer's own tests/gnatprove_import fixture, with the checkout directory shortened.
    String report = readResource("adalanganalyzer/gnatprove-import-console-output.txt");

    List<AdaLangAnalyzerProofObligation> obligations = parser.parseProofObligations(report);

    assertThat(parser.parse(report)).hasSize(3).hasSize(parser.reportedViolationCount(report));
    assertThat(obligations).hasSize(27).hasSize(parser.reportedProofObligationCount(report));
    assertThat(parser.reportedProofScope(report))
      .isEqualTo("bounded scalar verification; unsupported boundaries are explicit");

    // The verdict follows the method of each obligation a check of the log is paired with.
    assertThat(obligations).filteredOn(obligation -> !obligation.gnatprove().isBlank()).hasSize(14);
    assertThat(obligations)
      .extracting(AdaLangAnalyzerProofObligation::outcome, AdaLangAnalyzerProofObligation::gnatprove)
      .contains(
        org.assertj.core.groups.Tuple.tuple("proved-safe", "proved"),
        org.assertj.core.groups.Tuple.tuple("unproved", "proved"),
        org.assertj.core.groups.Tuple.tuple("unproved", "not-proved"),
        org.assertj.core.groups.Tuple.tuple("definite-error", "not-proved"));

    // The addition only GNATprove proves: the analyzer's own outcome and details are as they
    // are without the log.
    assertThat(obligations.get(9)).isEqualTo(
      new AdaLangAnalyzerProofObligation(
        "/checkout/tests/gnatprove_import/sample.ads", 18, 7, "integer-overflow", "unproved",
        "abstract-interpretation", "overflow is not established, but absence is not proved",
        "this call form cannot be inlined safely", "", "unsupported-call", "Data (Data'First)", "")
        .withGnatprove("proved"));
    assertThat(obligations.get(9).sonarMessage()).isEqualTo(
      "Proof obligation [integer-overflow] unproved. Method: abstract-interpretation. GNATprove: proved"
        + ". Why: overflow is not established, but absence is not proved"
        + ". Imprecision: this call form cannot be inlined safely"
        + ". Reason: unsupported-call"
        + ". Blocked at: Data (Data'First)");

    assertThat(parser.reportedGnatproveSummary(report)).contains(
      new AdaLangAnalyzerGnatproveSummary(1, 14, 11, 0, 3, 10, 1, 0, 0, 0, 14));
  }

  @Test
  void readsTheGnatproveCountsThatAreToBeLookedAtFirst() {
    // Summary copied verbatim from AdaLang Analyzer 1.8.4 output, for the hand-written log of
    // the analyzer's tests that goes against its own results (tests/gnatprove_import/made_up.log).
    String report = """
      Files scanned : 2
      Violations    : 3

      GNATprove verdicts (read from 1 log; not AdaLang's own results):
        Checks in the log : 7 (4 proved, 1 justified, 2 not proved)
        Of the checks GNATprove proved:
          proved by AdaLang too : 1
          GNATprove's verdict alone, on an AdaLang obligation : 0
          GNATprove's verdict alone, no AdaLang obligation : 2
          a definite error for AdaLang : 1
        Proved by AdaLang where GNATprove did not prove : 1
        Obligations with a GNATprove verdict : 5 of 27

      Violations by check:
        Division_By_Zero : 1  [Reliability/Blocker]
      """;

    AdaLangAnalyzerGnatproveSummary summary = parser.reportedGnatproveSummary(report).orElseThrow();

    assertThat(summary).isEqualTo(new AdaLangAnalyzerGnatproveSummary(1, 7, 4, 1, 2, 1, 0, 2, 1, 1, 5));
    assertThat(summary.definiteErrorWhereGnatproveProved()).isEqualTo(1);
    assertThat(summary.provedSafeWhereGnatproveNotProved()).isEqualTo(1);
    // The four ways a check GNATprove proved is accounted for add up to the checks it proved.
    assertThat(summary.provedByBoth() + summary.provedByGnatproveOnObligation()
      + summary.provedByGnatproveWithoutObligation() + summary.definiteErrorWhereGnatproveProved())
      .isEqualTo(summary.proved());
  }

  @Test
  void readsTheGnatproveSummaryOfSeveralLogs() {
    // Copied verbatim from AdaLang Analyzer 1.8.4 output of a run given two logs and no -v.
    String report = """
      Proof obligations (bounded scalar verification; unsupported boundaries are explicit):
        Total : 27
        proved-safe : 20
        definite-error : 1
        unproved : 6
        (details suppressed; rerun with -v to list each proof obligation)

      GNATprove verdicts (read from 2 logs; not AdaLang's own results):
        Checks in the log : 18 (13 proved, 0 justified, 5 not proved)
        Of the checks GNATprove proved:
          proved by AdaLang too : 10
          GNATprove's verdict alone, on an AdaLang obligation : 0
          GNATprove's verdict alone, no AdaLang obligation : 3
          a definite error for AdaLang : 0
        Proved by AdaLang where GNATprove did not prove : 0
        Obligations with a GNATprove verdict : 14 of 27
      """;

    assertThat(parser.reportedGnatproveSummary(report)).contains(
      new AdaLangAnalyzerGnatproveSummary(2, 18, 13, 0, 5, 10, 0, 3, 0, 0, 14));
    assertThat(parser.reportedProofObligationCount(report)).isEqualTo(27);
  }

  @Test
  void reportsNoGnatproveSummaryForARunWithoutALog() {
    assertThat(parser.reportedGnatproveSummary("Files scanned : 3\nViolations    : 0\n")).isEmpty();
  }

  @Test
  void parsesTheObligationsAboutASubprogramAsAWhole() {
    // Obligations copied verbatim from AdaLang Analyzer 1.8.3 --verify -v output, with the
    // checkout directory shortened: the termination, data-dependencies, and flow-dependencies
    // kinds 1.8.1 added. No route proves a Depends aspect yet, which the method none says.
    // All three unproved ones are worth an issue.
    String report = """
      Proof obligations (bounded scalar verification; unsupported boundaries are explicit):
        Total : 5
        proved-safe : 2
        unproved : 3
        Details:
          /checkout/tests/verification_global_aspect_reference_clean.adb:5:11 [data-dependencies] unproved
            method: flow-analysis
            why: the Global aspect is not shown to cover what the subprogram reads and writes
            imprecision: Arg is used and is not listed
          /checkout/tests/verification_mutation_call_effects.adb:65:13 [termination] unproved
            method: flow-analysis
            why: the subprogram is not shown to return
            imprecision: a loop that is not a for loop is not shown to end
          /checkout/tests/verification_flow_contracts.adb:39:11 [data-dependencies] proved-safe
            method: flow-analysis
            why: the subprogram reads and writes no outside object its Global aspect does not allow
            evidence: every object the body may read or write is allowed by the aspect
          /checkout/tests/verification_flow_contracts.adb:40:11 [flow-dependencies] unproved
            method: none
            why: the Depends aspect is not shown to be the dependencies of the subprogram
            imprecision: information flow is not yet analyzed to the point of proof
          /checkout/tests/verification_termination.adb:7:13 [termination] proved-safe
            method: flow-analysis
            why: the subprogram returns: its loops are bounded, it is not recursive and what it calls returns
            evidence: no unbounded loop, no recursion, every callee terminates
      """;

    List<AdaLangAnalyzerProofObligation> obligations = parser.parseProofObligations(report);

    assertThat(obligations).hasSize(parser.reportedProofObligationCount(report));
    assertThat(obligations)
      .extracting(AdaLangAnalyzerProofObligation::ruleId)
      .containsExactly(
        "proof-obligation:data-dependencies", "proof-obligation:termination", "proof-obligation:data-dependencies",
        "proof-obligation:flow-dependencies", "proof-obligation:termination");
    assertThat(obligations)
      .extracting(AdaLangAnalyzerProofObligation::method)
      .containsExactly("flow-analysis", "flow-analysis", "flow-analysis", "none", "flow-analysis");
    assertThat(obligations)
      .extracting(AdaLangAnalyzerProofObligation::isActionable)
      .containsExactly(true, true, false, true, false);
    assertThat(obligations.get(1).sonarMessage()).isEqualTo(
      "Proof obligation [termination] unproved. Method: flow-analysis"
        + ". Why: the subprogram is not shown to return"
        + ". Imprecision: a loop that is not a for loop is not shown to end");
  }

  @Test
  void parsesTheLengthChecksOfRealAnalyzerOutput() {
    // The complete output of AdaLang Analyzer 1.8.6 run with --verify -v on the analyzer's own
    // tests/verification_length_check_state.adb, with the checkout directory shortened: the
    // length-check kind 1.8.4 added.
    String report = readResource("adalanganalyzer/length-check-console-output.txt");

    List<AdaLangAnalyzerProofObligation> obligations = parser.parseProofObligations(report);

    assertThat(parser.parse(report)).hasSize(3).hasSize(parser.reportedViolationCount(report));
    assertThat(obligations).hasSize(47).hasSize(parser.reportedProofObligationCount(report));

    List<AdaLangAnalyzerProofObligation> lengthChecks = obligations.stream()
      .filter(obligation -> obligation.kind().equals("length-check"))
      .toList();
    // An assignment to a slice has two: one at the value, one of its own at the ":=".
    assertThat(lengthChecks)
      .extracting(
        AdaLangAnalyzerProofObligation::line, AdaLangAnalyzerProofObligation::column,
        AdaLangAnalyzerProofObligation::outcome)
      .containsExactly(
        org.assertj.core.groups.Tuple.tuple(15, 45, "proved-safe"),
        org.assertj.core.groups.Tuple.tuple(18, 27, "unproved"),
        org.assertj.core.groups.Tuple.tuple(18, 24, "unproved"),
        org.assertj.core.groups.Tuple.tuple(29, 28, "unproved"),
        org.assertj.core.groups.Tuple.tuple(29, 25, "unproved"),
        org.assertj.core.groups.Tuple.tuple(38, 32, "proved-safe"),
        org.assertj.core.groups.Tuple.tuple(42, 48, "unproved"));
    assertThat(lengthChecks)
      .extracting(AdaLangAnalyzerProofObligation::ruleId)
      .containsOnly("proof-obligation:length-check");
    assertThat(lengthChecks)
      .extracting(AdaLangAnalyzerProofObligation::isActionable)
      .containsExactly(false, true, true, true, true, false, true);
    assertThat(lengthChecks.get(2)).isEqualTo(
      new AdaLangAnalyzerProofObligation(
        "/checkout/tests/verification_length_check_state.adb", 18, 24, "length-check", "unproved",
        "abstract-interpretation", "length-check failure is not established, but absence is not proved",
        "the two lengths are not known to be equal"));
    assertThat(lengthChecks.get(2).sonarMessage()).isEqualTo(
      "Proof obligation [length-check] unproved. Method: abstract-interpretation"
        + ". Why: length-check failure is not established, but absence is not proved"
        + ". Imprecision: the two lengths are not known to be equal");
    assertThat(lengthChecks.getFirst().sonarMessage()).isEqualTo(
      "Proof obligation [length-check] proved-safe. Method: abstract-interpretation"
        + ". Why: an aggregate with an others choice has the bounds of what it is given to"
        + ". Evidence: the value takes the bounds of its target");
  }

  @Test
  void parsesTheReadsOfAnOutActualItsCalleeMayNotHaveWritten() {
    // The complete output of AdaLang Analyzer 1.8.6 run with --verify -v on the analyzer's own
    // tests/verification_fp117_unwritten_out.adb, with the checkout directory shortened. Up to
    // 1.8.4 every one of these reads was proved safe: an out actual was taken to be initialized
    // after a call whose callee has a path that returns without writing the parameter.
    String report = readResource("adalanganalyzer/unwritten-out-console-output.txt");

    List<AdaLangAnalyzerProofObligation> obligations = parser.parseProofObligations(report);

    assertThat(parser.parse(report)).hasSize(18).hasSize(parser.reportedViolationCount(report));
    assertThat(obligations).hasSize(35).hasSize(parser.reportedProofObligationCount(report));

    // The read of the actual, in the statement after each of the five calls: not proved where a
    // path of the callee passes the write, an error where the callee never writes the
    // parameter, and proved where every path of the callee writes it.
    List<AdaLangAnalyzerProofObligation> reads = obligations.stream()
      .filter(obligation -> obligation.kind().equals("initialization-check"))
      .filter(obligation -> List.of(69, 76, 83, 90, 97).contains(obligation.line()))
      .toList();
    assertThat(reads)
      .extracting(AdaLangAnalyzerProofObligation::line, AdaLangAnalyzerProofObligation::outcome)
      .containsExactly(
        org.assertj.core.groups.Tuple.tuple(69, "unproved"),
        org.assertj.core.groups.Tuple.tuple(76, "unproved"),
        org.assertj.core.groups.Tuple.tuple(83, "unproved"),
        org.assertj.core.groups.Tuple.tuple(90, "definite-error"),
        org.assertj.core.groups.Tuple.tuple(97, "proved-safe"));
    assertThat(reads)
      .extracting(AdaLangAnalyzerProofObligation::isActionable)
      .containsExactly(true, true, true, true, false);
    assertThat(reads.get(3)).isEqualTo(
      new AdaLangAnalyzerProofObligation(
        "/checkout/tests/verification_fp117_unwritten_out.adb", 90, 12, "initialization-check", "definite-error",
        "flow-analysis", "object is uninitialized on every incoming path", "", "initialization => false", "", "", ""));
    assertThat(reads.get(3).sonarMessage()).isEqualTo(
      "Proof obligation [initialization-check] definite-error. Method: flow-analysis"
        + ". Why: object is uninitialized on every incoming path"
        + ". Evidence: initialization => false");
    assertThat(reads.getFirst().sonarMessage()).isEqualTo(
      "Proof obligation [initialization-check] unproved. Method: flow-analysis"
        + ". Why: object initialization is not established"
        + ". Imprecision: incoming paths disagree or object is external");

    // A check on the value read says which object stands in its way.
    assertThat(obligations)
      .filteredOn(obligation -> obligation.reasonCode().equals("uninitialized-object"))
      .extracting(AdaLangAnalyzerProofObligation::kind, AdaLangAnalyzerProofObligation::blockingExpression)
      .containsExactly(
        org.assertj.core.groups.Tuple.tuple("range-check", "Looped"),
        org.assertj.core.groups.Tuple.tuple("range-check", "Jumped"),
        org.assertj.core.groups.Tuple.tuple("range-check", "Blocked"),
        org.assertj.core.groups.Tuple.tuple("range-check", "Swallowed"));
  }

  @Test
  void parsesAReadOfAnObjectThatAlwaysHoldsAValue() {
    // Obligations copied verbatim from AdaLang Analyzer 1.8.5 --verify -v output, with the
    // checkout directory shortened: the division by a constant of a package. 1.8.4 left both
    // unproved, not knowing the constant to be initialized. The read is now proved by what the
    // object is, with a method no initialization check had before, and the division with it.
    String report = """
      Proof obligations (bounded scalar verification; unsupported boundaries are explicit):
        Total : 2
        proved-safe : 2
        Details:
          /checkout/tests/verification_standing_values.adb:14:25 [division-by-zero] proved-safe
            method: abstract-interpretation
            why: right operand range excludes zero
            evidence: right operand is strictly negative or positive
          /checkout/tests/verification_standing_values.adb:14:25 [initialization-check] proved-safe
            method: static-evaluation
            why: object holds a value wherever it is read
            evidence: a constant, an in parameter or a loop parameter
      """;

    List<AdaLangAnalyzerProofObligation> obligations = parser.parseProofObligations(report);

    assertThat(obligations).hasSize(2).hasSize(parser.reportedProofObligationCount(report));
    assertThat(obligations.getLast()).isEqualTo(
      new AdaLangAnalyzerProofObligation(
        "/checkout/tests/verification_standing_values.adb", 14, 25, "initialization-check", "proved-safe",
        "static-evaluation", "object holds a value wherever it is read", "",
        "a constant, an in parameter or a loop parameter", "", "", ""));
    assertThat(obligations).noneMatch(AdaLangAnalyzerProofObligation::isActionable);
  }

  @Test
  void parsesAComponentReadInTwoElementsOfAnArray() {
    // The complete output of AdaLang Analyzer 1.8.6 run with --verify -v on the analyzer's own
    // tests/verification_fp119_element_component.ads and .adb, with the checkout directory
    // shortened. Up to 1.8.5 a component was one value for every element of its array, and the
    // first two of these obligations were proved safe: an assertion that two elements hold the
    // same count, and a division by a component that can be zero, under a condition on two
    // elements that was taken for one that cannot hold.
    String report = readResource("adalanganalyzer/element-component-console-output.txt");

    List<AdaLangAnalyzerProofObligation> obligations = parser.parseProofObligations(report);

    assertThat(parser.parse(report)).hasSize(1).hasSize(parser.reportedViolationCount(report));
    assertThat(obligations).hasSize(22).hasSize(parser.reportedProofObligationCount(report));

    // What is said of two elements is not proved; what is said of one element still is.
    assertThat(obligations.subList(0, 3))
      .extracting(
        AdaLangAnalyzerProofObligation::line, AdaLangAnalyzerProofObligation::kind,
        AdaLangAnalyzerProofObligation::outcome, AdaLangAnalyzerProofObligation::method)
      .containsExactly(
        org.assertj.core.groups.Tuple.tuple(6, "assertion", "unproved", "abstract-interpretation"),
        org.assertj.core.groups.Tuple.tuple(15, "division-by-zero", "unproved", "abstract-interpretation"),
        org.assertj.core.groups.Tuple.tuple(22, "assertion", "proved-safe", "external-prover"));
    assertThat(obligations.subList(0, 3))
      .extracting(AdaLangAnalyzerProofObligation::isActionable)
      .containsExactly(true, true, false);
    assertThat(obligations.getFirst().sonarMessage()).isEqualTo(
      "Proof obligation [assertion] unproved. Method: abstract-interpretation"
        + ". Why: assertion failure is not established, but the assertion is not proved"
        + ". Imprecision: abstract interpretation and the scalar VC portfolio did not certify it");
    assertThat(obligations.get(1)).isEqualTo(
      new AdaLangAnalyzerProofObligation(
        "/checkout/tests/verification_fp119_element_component.adb", 15, 26, "division-by-zero", "unproved",
        "abstract-interpretation", "zero has not been excluded from the divisor",
        "the divisor range is unknown or contains zero"));

    // The two checks on the quotient were not proved before either, and say what stands in
    // their way.
    assertThat(obligations)
      .filteredOn(obligation -> obligation.reasonCode().equals("unsafe-divisor-semantics"))
      .extracting(AdaLangAnalyzerProofObligation::kind, AdaLangAnalyzerProofObligation::blockingExpression)
      .containsExactly(
        org.assertj.core.groups.Tuple.tuple("integer-overflow", "Share / Mixed (2).Limit"),
        org.assertj.core.groups.Tuple.tuple("range-check", "Share / Mixed (2).Limit"));
    assertThat(obligations).filteredOn(AdaLangAnalyzerProofObligation::isActionable).hasSize(4);
  }

  @Test
  void parsesWhatAConditionalAndACaseExpressionDecide() {
    // A finding and obligations copied verbatim from AdaLang Analyzer 1.8.6 --verify -v output,
    // with the checkout directory shortened: a call whose precondition is "if Up then Amount >
    // 5", and the assertion of a function whose body is a case expression, which is false where
    // it stands. 1.8.5 handed neither expression to the solvers and left the three unproved, the
    // precondition with the reason unsupported-expression-kind. The precondition is now proved,
    // and the assertion is a definite error and a finding of the Known_Assertion_Failure check.
    String report = """
      /checkout/tests/verification_mutation_expression_forms.adb:57:22: warning: assertion condition is false here \
      based on earlier state [Known_Assertion_Failure]
        rule: Find Assert, Assert_And_Cut, Check, and Loop_Invariant pragmas whose condition is statically false.
        advice: Correct the implementation or assertion so the asserted property holds at this program point.
        why: The incoming flow state makes the assertion condition False.
        evidence: SMT-LIB scalar VC; CVC5 and Z3 agreement required
        quality: Reliability (High)
        source:
                pragma Assert (Fits (Medium, Ten));
                               ^^^^^^^^^^^^^^^^^^

      Files scanned : 4
      Violations    : 1

      Proof obligations (bounded scalar verification; unsupported boundaries are explicit):
        Total : 3
        proved-safe : 2
        definite-error : 1
        Details:
          /checkout/tests/verification_expression_forms.adb:18:7 [precondition] proved-safe
            method: external-prover
            why: actual arguments satisfy the precondition
            evidence: SMT-LIB scalar VC; CVC5 and Z3 agreement required
          /checkout/tests/verification_expression_forms.adb:18:7 [precondition] proved-safe
            method: external-prover
            why: actual arguments satisfy the precondition
            evidence: SMT-LIB scalar VC; CVC5 and Z3 agreement required
          /checkout/tests/verification_mutation_expression_forms.adb:57:22 [assertion] definite-error
            method: external-prover
            why: assertion condition is false based on the incoming state
            evidence: SMT-LIB scalar VC; CVC5 and Z3 agreement required
      """;

    List<AdaLangAnalyzerFinding> findings = parser.parse(report);
    List<AdaLangAnalyzerProofObligation> obligations = parser.parseProofObligations(report);

    assertThat(obligations).hasSize(3).hasSize(parser.reportedProofObligationCount(report));
    assertThat(obligations)
      .extracting(AdaLangAnalyzerProofObligation::isActionable)
      .containsExactly(false, false, true);
    assertThat(obligations.getLast()).isEqualTo(
      new AdaLangAnalyzerProofObligation(
        "/checkout/tests/verification_mutation_expression_forms.adb", 57, 22, "assertion", "definite-error",
        "external-prover", "assertion condition is false based on the incoming state", "",
        "SMT-LIB scalar VC; CVC5 and Z3 agreement required", "", "", ""));
    assertThat(obligations.getLast().isDefiniteError()).isTrue();

    // The finding is where the obligation is, and has the evidence of the obligation.
    assertThat(findings).hasSize(1).hasSize(parser.reportedViolationCount(report));
    assertThat(findings.getFirst())
      .extracting(
        AdaLangAnalyzerFinding::file, AdaLangAnalyzerFinding::line, AdaLangAnalyzerFinding::column,
        AdaLangAnalyzerFinding::ruleId, AdaLangAnalyzerFinding::evidence)
      .containsExactly(
        "/checkout/tests/verification_mutation_expression_forms.adb", 57, 22, "Known_Assertion_Failure",
        "SMT-LIB scalar VC; CVC5 and Z3 agreement required");
  }

  @Test
  void keepsEveryLineOfAnExpressionWrittenOnSeveral() {
    // Obligations copied verbatim from AdaLang Analyzer 1.8.4 --verify -v output, with the
    // checkout directory shortened: the expression that blocked the proof is a case expression
    // written on two lines of the source, and is printed as it is written.
    String report = """
      Proof obligations (bounded scalar verification; unsupported boundaries are explicit):
        Total : 2
        proved-safe : 1
        unproved : 1
        Details:
          /checkout/tests/verification_guarded_operand.adb:64:17 [integer-overflow] unproved
            method: abstract-interpretation
            why: overflow is not established, but absence is not proved
            imprecision: this expression form is outside the scalar VC subset
            reason: unsupported-expression-kind
            blocked at: case Count is when 1 .. 4 => Table (Count),
                                              when others => 0
          /checkout/tests/verification_guarded_operand.adb:64:17 [initialization-check] proved-safe
            method: flow-analysis
            why: object is initialized on every incoming path
            evidence: initialization => true

      Violations by check:
        Missing_Depends_Contract : 1  [Maintainability/Medium]
      """;

    List<AdaLangAnalyzerProofObligation> obligations = parser.parseProofObligations(report);

    assertThat(obligations).hasSize(2).hasSize(parser.reportedProofObligationCount(report));
    // As the blockingExpression of the JSON report has it.
    assertThat(obligations.getFirst().blockingExpression()).isEqualTo(
      "case Count is when 1 .. 4 => Table (Count),\n" + " ".repeat(40) + "when others => 0");
    assertThat(obligations.getFirst().reasonCode()).isEqualTo("unsupported-expression-kind");
    // What follows the details is no part of the last of them.
    assertThat(obligations.getLast()).isEqualTo(
      new AdaLangAnalyzerProofObligation(
        "/checkout/tests/verification_guarded_operand.adb", 64, 17, "initialization-check", "proved-safe",
        "flow-analysis", "object is initialized on every incoming path", "", "initialization => true", "", "", ""));
  }

  @Test
  void readsFromTheTextOutputWhatTheJsonReportHas() {
    // The text output and the --format=json report of the same runs of AdaLang Analyzer 1.8.6.
    // Only the JSON report has the operation, the assumptions and the subject of an obligation;
    // only the text output has the rule, the advice and the source line of a finding.
    for (String run : List.of("gnatprove-import", "length-check", "unwritten-out", "element-component")) {
      String text = readResource("adalanganalyzer/" + run + "-console-output.txt");
      AdaLangAnalyzerReport json = new AdaLangAnalyzerJsonReportParser().parse(AdaLangAnalyzerJson.mapOf(
        AdaLangAnalyzerJson.parse(readResource("adalanganalyzer/" + run + "-report.json"))));

      assertThat(parser.parseProofObligations(text))
        .extracting(AdaLangAnalyzerConsoleParserTest::inBothFormats)
        .containsExactlyElementsOf(
          json.proofObligations().stream().map(AdaLangAnalyzerConsoleParserTest::inBothFormats).toList());
      assertThat(parser.parse(text))
        .extracting(AdaLangAnalyzerConsoleParserTest::inBothFormats)
        .containsExactlyElementsOf(
          json.findings().stream().map(AdaLangAnalyzerConsoleParserTest::inBothFormats).toList());
      assertThat(parser.reportedGnatproveSummary(text)).isEqualTo(json.gnatproveSummary());
    }
  }

  private static List<Object> inBothFormats(AdaLangAnalyzerProofObligation obligation) {
    return List.of(
      obligation.file().replace("/checkout/", ""), obligation.line(), obligation.column(), obligation.kind(),
      obligation.outcome(), obligation.method(), obligation.gnatprove(), obligation.why(), obligation.evidence(),
      obligation.imprecision(), obligation.reasonCode(), obligation.blockingExpression(), obligation.inlinePath());
  }

  private static List<Object> inBothFormats(AdaLangAnalyzerFinding finding) {
    return List.of(
      finding.file().replace("/checkout/", ""), finding.line(), finding.column(), finding.ruleId(),
      finding.message(), finding.explanation(), finding.evidence(), finding.softwareQuality(),
      finding.qualitySeverity());
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

  @Test
  void extractsTheAnalyzersOwnRunWarnings() {
    // Lines copied verbatim from AdaLang Analyzer 1.6.2 output.
    String output = """
      adalang-analyzer: warning: no checks are enabled; pass -checks=<list>, --recommended, --spark, --verify, \
      --automotive, or --do178c=<level> to actually analyze the source
      adalang-analyzer: warning: no 'gnatls' found on PATH; types from with'd runtime packages (Interfaces, Ada.*, \
      System, ...) will not resolve, which silently degrades some checks (see known_analysis_issues.tsv, FP-029); \
      add a GNAT toolchain to PATH to avoid this
      adalang-analyzer [INFO]: Built 1 subprogram summaries
      adalang-analyzer [INFO]: Parsing: p.adb
      /project/src/demo.adb:12:7: warning: goto statements are forbidden [No_Goto]

      Files scanned : 1
      Violations    : 1
      """;

    List<String> warnings = parser.warnings(output);

    assertThat(warnings).hasSize(2);
    assertThat(warnings.get(0)).startsWith("no checks are enabled; pass -checks=<list>");
    assertThat(warnings.get(1)).startsWith("no 'gnatls' found on PATH");
    assertThat(parser.parse(output)).hasSize(1);
  }

  @Test
  void readsTheProofScopeThatTellsAVerificationRunFromAnEnumeration() {
    // Summary headers copied verbatim from AdaLang Analyzer 1.6.2 output, with and without --verify.
    String verified = """
      Files scanned : 1
      Violations    : 0

      Proof obligations (bounded scalar verification; unsupported boundaries are explicit):
        Total : 1
        unproved : 1
        Details:
          /project/src/demo.adb:10:7 [index-check] unproved
            method: abstract-interpretation
      """;
    String enumerated = verified.replace(
      "bounded scalar verification; unsupported boundaries are explicit",
      "enumerated outcomes in current analysis scope; not exhaustive");

    assertThat(parser.reportedProofScope(verified))
      .isEqualTo("bounded scalar verification; unsupported boundaries are explicit");
    assertThat(parser.reportedProofScope(enumerated))
      .isEqualTo("enumerated outcomes in current analysis scope; not exhaustive");
    assertThat(parser.reportedProofScope("Files scanned : 1\n")).isEmpty();
    assertThat(parser.reportedProofObligationCount(enumerated)).isEqualTo(1);
  }
}
