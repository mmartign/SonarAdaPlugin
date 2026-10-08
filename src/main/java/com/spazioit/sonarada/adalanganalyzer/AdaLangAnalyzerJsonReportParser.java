/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.spazioit.sonarada.adalanganalyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reads AdaLang Analyzer's native {@code --format=json} report: a top-level {@code findings}
 * array and {@code proofObligations} array, alongside {@code filesScanned}, {@code
 * newViolations}, {@code proofSummary.total}, and {@code analysisConfiguration.skippedChecks}
 * summary figures. A report of a run that was given a GNATprove log also has a {@code
 * gnatprove} verdict on the obligations a check of the log is paired with and the counts in
 * {@code gnatproveImport}; its {@code gnatproveChecks} array, every check of the log, is not
 * read. See AdaLang Analyzer's {@code Adalang_Analyzer.Report.Emit_JSON}.
 */
final class AdaLangAnalyzerJsonReportParser {

  AdaLangAnalyzerReport parse(Map<String, Object> root) {
    List<AdaLangAnalyzerFinding> findings = new ArrayList<>();
    for (Object item : AdaLangAnalyzerJson.listOf(root.get("findings"))) {
      Map<String, Object> finding = AdaLangAnalyzerJson.mapOf(item);
      if (AdaLangAnalyzerJson.boolOf(finding, "baseline")) {
        // Already accepted in a prior run; console-text output hides these too.
        continue;
      }
      findings.add(new AdaLangAnalyzerFinding(
        AdaLangAnalyzerJson.stringOf(finding, "file"),
        AdaLangAnalyzerJson.intOf(finding, "line"),
        AdaLangAnalyzerJson.intOf(finding, "column"),
        "warning",
        AdaLangAnalyzerJson.stringOf(finding, "message"),
        AdaLangAnalyzerJson.stringOf(finding, "ruleId"),
        "",
        "",
        AdaLangAnalyzerJson.stringOf(finding, "explanation"),
        AdaLangAnalyzerJson.stringOf(finding, "evidence"),
        AdaLangAnalyzerJson.stringOf(finding, "quality"),
        AdaLangAnalyzerJson.stringOf(finding, "severity"),
        "",
        1));
    }

    List<AdaLangAnalyzerProofObligation> proofObligations = new ArrayList<>();
    for (Object item : AdaLangAnalyzerJson.listOf(root.get("proofObligations"))) {
      Map<String, Object> obligation = AdaLangAnalyzerJson.mapOf(item);
      // The declaration an initialization obligation is about, when it has one.
      Map<String, Object> subject = AdaLangAnalyzerJson.mapOf(obligation.get("subject"));
      proofObligations.add(new AdaLangAnalyzerProofObligation(
        AdaLangAnalyzerJson.stringOf(obligation, "file"),
        AdaLangAnalyzerJson.intOf(obligation, "line"),
        AdaLangAnalyzerJson.intOf(obligation, "column"),
        AdaLangAnalyzerJson.stringOf(obligation, "kind"),
        AdaLangAnalyzerJson.stringOf(obligation, "status"),
        AdaLangAnalyzerJson.stringOf(obligation, "method"),
        AdaLangAnalyzerJson.stringOf(obligation, "explanation"),
        AdaLangAnalyzerJson.stringOf(obligation, "imprecisionSource"),
        AdaLangAnalyzerJson.stringOf(obligation, "abstractState"),
        AdaLangAnalyzerJson.stringOf(obligation, "reasonCode"),
        AdaLangAnalyzerJson.stringOf(obligation, "blockingExpression"),
        AdaLangAnalyzerJson.stringOf(obligation, "inlinePath"),
        AdaLangAnalyzerJson.stringOf(obligation, "operation"),
        AdaLangAnalyzerJson.stringOf(obligation, "assumptions"),
        AdaLangAnalyzerJson.stringOf(obligation, "configurationId"),
        AdaLangAnalyzerJson.stringOf(obligation, "gnatprove"),
        AdaLangAnalyzerJson.stringOf(subject, "file"),
        AdaLangAnalyzerJson.intOf(subject, "line"),
        AdaLangAnalyzerJson.intOf(subject, "column")));
    }

    Map<String, Object> analysisConfiguration = AdaLangAnalyzerJson.mapOf(root.get("analysisConfiguration"));
    Map<String, Object> proofSummary = AdaLangAnalyzerJson.mapOf(root.get("proofSummary"));

    return new AdaLangAnalyzerReport(
      List.copyOf(findings),
      List.copyOf(proofObligations),
      AdaLangAnalyzerJson.intOf(root, "filesScanned"),
      AdaLangAnalyzerJson.intOf(root, "newViolations"),
      AdaLangAnalyzerJson.intOf(proofSummary, "total"),
      AdaLangAnalyzerJson.intOf(analysisConfiguration, "skippedChecks"),
      AdaLangAnalyzerJson.stringOf(proofSummary, "scope"),
      root.containsKey("gnatproveImport")
        ? Optional.of(AdaLangAnalyzerGnatproveSummary.of(AdaLangAnalyzerJson.mapOf(root.get("gnatproveImport"))))
        : Optional.empty());
  }
}
