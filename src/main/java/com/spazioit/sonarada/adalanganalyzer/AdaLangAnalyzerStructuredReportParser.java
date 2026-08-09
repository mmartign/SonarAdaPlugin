/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.spazioit.sonarada.adalanganalyzer;

import java.util.Map;
import java.util.Optional;

/**
 * Detects and parses AdaLang Analyzer's JSON and SARIF report formats. Console-text and
 * legacy CSV/CSVX reports are not JSON, so callers fall back to
 * {@link AdaLangAnalyzerConsoleParser} (and beyond it, the legacy CSV/CSVX parser) whenever
 * this returns an empty {@link Optional}.
 */
final class AdaLangAnalyzerStructuredReportParser {

  private final AdaLangAnalyzerJsonReportParser jsonParser = new AdaLangAnalyzerJsonReportParser();
  private final AdaLangAnalyzerSarifReportParser sarifParser = new AdaLangAnalyzerSarifReportParser();

  Optional<AdaLangAnalyzerReport> parse(String content) {
    if (!content.stripLeading().startsWith("{")) {
      return Optional.empty();
    }

    Map<String, Object> root;
    try {
      root = AdaLangAnalyzerJson.mapOf(AdaLangAnalyzerJson.parse(content));
    } catch (RuntimeException malformed) {
      return Optional.empty();
    }

    if (root.containsKey("runs")) {
      return Optional.of(sarifParser.parse(root));
    }
    if (root.containsKey("findings")) {
      return Optional.of(jsonParser.parse(root));
    }
    return Optional.empty();
  }
}
