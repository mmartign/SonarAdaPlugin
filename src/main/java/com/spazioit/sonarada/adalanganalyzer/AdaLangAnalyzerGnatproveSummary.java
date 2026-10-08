/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.spazioit.sonarada.adalanganalyzer;

import java.util.Map;

/**
 * The counts AdaLang Analyzer reports when it is given the log of a GNATprove run on the same
 * sources ({@code --gnatprove-log}, see {@code Adalang_Analyzer.Gnatprove_Import.Summary}). The
 * verdicts are GNATprove's, a separate tool the analyzer neither contains nor runs; they stand
 * beside the analyzer's own results and change none of them. Every check GNATprove proved is one
 * of four things, which add up to {@code proved}: proved by the analyzer too, an obligation the
 * analyzer did not decide, no obligation of the analyzer's, or an obligation the analyzer calls a
 * definite error. A negative count means the report does not give that figure.
 */
record AdaLangAnalyzerGnatproveSummary(
  int logs,
  int checks,
  int proved,
  int justified,
  int notProved,
  int provedByBoth,
  int provedByGnatproveOnObligation,
  int provedByGnatproveWithoutObligation,
  int definiteErrorWhereGnatproveProved,
  int provedSafeWhereGnatproveNotProved,
  int obligationsWithVerdict
) {
  /** Reads the {@code gnatproveImport} member of a JSON report or of a SARIF run's properties. */
  static AdaLangAnalyzerGnatproveSummary of(Map<String, Object> gnatproveImport) {
    return new AdaLangAnalyzerGnatproveSummary(
      AdaLangAnalyzerJson.listOf(gnatproveImport.get("logs")).size(),
      AdaLangAnalyzerJson.intOf(gnatproveImport, "checks"),
      AdaLangAnalyzerJson.intOf(gnatproveImport, "proved"),
      AdaLangAnalyzerJson.intOf(gnatproveImport, "justified"),
      AdaLangAnalyzerJson.intOf(gnatproveImport, "notProved"),
      AdaLangAnalyzerJson.intOf(gnatproveImport, "provedByBoth"),
      AdaLangAnalyzerJson.intOf(gnatproveImport, "provedByGnatproveOnObligation"),
      AdaLangAnalyzerJson.intOf(gnatproveImport, "provedByGnatproveWithoutObligation"),
      AdaLangAnalyzerJson.intOf(gnatproveImport, "definiteErrorWhereGnatproveProved"),
      AdaLangAnalyzerJson.intOf(gnatproveImport, "provedSafeWhereGnatproveNotProved"),
      AdaLangAnalyzerJson.intOf(gnatproveImport, "obligationsWithVerdict"));
  }
}
