/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.spazioit.sonarada.adalanganalyzer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class AdaLangAnalyzerStructuredReportParserTest {
  private final AdaLangAnalyzerStructuredReportParser parser = new AdaLangAnalyzerStructuredReportParser();

  @Test
  void recognizesNativeJsonReports() {
    Optional<AdaLangAnalyzerReport> report = parser.parse("""
      {"findings": [{"ruleId": "No_Goto", "message": "goto statements are forbidden", "file": "demo.adb", \
      "line": 1, "column": 1, "severity": "Medium", "quality": "Maintainability", "baseline": false}], \
      "proofObligations": [], "filesScanned": 1, "newViolations": 1}
      """);

    assertThat(report).isPresent();
    assertThat(report.get().findings()).extracting(AdaLangAnalyzerFinding::ruleId).containsExactly("No_Goto");
  }

  @Test
  void recognizesSarifReports() {
    Optional<AdaLangAnalyzerReport> report = parser.parse("""
      {"version": "2.1.0", "runs": [{"tool": {"driver": {"rules": []}}, "results": [
        {"ruleId": "No_Goto", "level": "warning", "message": {"text": "goto statements are forbidden"}, \
      "baselineState": "new", "properties": {}, "locations": [{"physicalLocation": {\
      "artifactLocation": {"uri": "demo.adb"}, "region": {"startLine": 1, "startColumn": 1}}}]}
      ]}]}
      """);

    assertThat(report).isPresent();
    assertThat(report.get().findings()).extracting(AdaLangAnalyzerFinding::ruleId).containsExactly("No_Goto");
  }

  @Test
  void ignoresConsoleTextReports() {
    assertThat(parser.parse("/project/src/demo.adb:12:7: warning: goto statements are forbidden [No_Goto]\n"))
      .isEmpty();
  }

  @Test
  void ignoresMalformedJson() {
    assertThat(parser.parse("{\"findings\": [")).isEmpty();
  }

  @Test
  void ignoresUnrecognizedJsonShapes() {
    assertThat(parser.parse("{\"hello\": \"world\"}")).isEmpty();
  }
}
