/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.spazioit.sonarada.adalanganalyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reads AdaLang Analyzer's native {@code --format=json} report: a top-level {@code findings}
 * array and {@code proofObligations} array, alongside {@code filesScanned}, {@code
 * newViolations}, {@code proofSummary.total}, and {@code analysisConfiguration.skippedChecks}
 * summary figures. See AdaLang Analyzer's {@code Adalang_Analyzer.Report.Emit_JSON}.
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
        AdaLangAnalyzerJson.stringOf(obligation, "inlinePath")));
    }

    Map<String, Object> analysisConfiguration = AdaLangAnalyzerJson.mapOf(root.get("analysisConfiguration"));
    Map<String, Object> proofSummary = AdaLangAnalyzerJson.mapOf(root.get("proofSummary"));

    return new AdaLangAnalyzerReport(
      List.copyOf(findings),
      List.copyOf(proofObligations),
      AdaLangAnalyzerJson.intOf(root, "filesScanned"),
      AdaLangAnalyzerJson.intOf(root, "newViolations"),
      AdaLangAnalyzerJson.intOf(proofSummary, "total"),
      AdaLangAnalyzerJson.intOf(analysisConfiguration, "skippedChecks"));
  }
}
