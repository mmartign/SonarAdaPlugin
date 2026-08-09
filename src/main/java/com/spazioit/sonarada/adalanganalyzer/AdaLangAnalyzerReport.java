/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.spazioit.sonarada.adalanganalyzer;

import java.util.List;

/**
 * The findings, proof obligations, and self-reported summary counts of one AdaLang Analyzer
 * run or report, regardless of whether it was read from console text, JSON, or SARIF. A
 * negative count means the source format does not report that figure.
 */
record AdaLangAnalyzerReport(
  List<AdaLangAnalyzerFinding> findings,
  List<AdaLangAnalyzerProofObligation> proofObligations,
  int fileCount,
  int violationCount,
  int proofObligationCount,
  int skippedCheckCount
) {
}
