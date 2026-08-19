/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.spazioit.sonarada.adalanganalyzer;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads AdaLang Analyzer's {@code --format=sarif} report: a single {@code runs[0]} with a
 * {@code tool.driver.rules} catalog and a {@code results} array. SARIF's result-oriented schema
 * has no slot for a located proof obligation, so {@code results} carries only findings; proof
 * obligations instead appear as an unlocated {@code properties.proofObligations} summary array
 * (id/kind/status/reasonCode/blockingExpression/inlinePath, no file/line/column) that this parser
 * turns into obligations with an empty location so they still count towards the total but cannot
 * be resolved to an input file (see {@code Adalang_Analyzer.Report.Emit_SARIF} and {@code
 * Adalang_Analyzer.Report.Emit_JSON} for the format that carries fully located obligations).
 */
final class AdaLangAnalyzerSarifReportParser {

  AdaLangAnalyzerReport parse(Map<String, Object> root) {
    List<Object> runs = AdaLangAnalyzerJson.listOf(root.get("runs"));
    if (runs.isEmpty()) {
      return new AdaLangAnalyzerReport(List.of(), List.of(), -1, -1, -1, -1);
    }
    Map<String, Object> run = AdaLangAnalyzerJson.mapOf(runs.get(0));
    Map<String, RuleMetadata> rules = ruleCatalog(run);

    List<AdaLangAnalyzerFinding> findings = new ArrayList<>();
    for (Object item : AdaLangAnalyzerJson.listOf(run.get("results"))) {
      Map<String, Object> result = AdaLangAnalyzerJson.mapOf(item);
      if ("unchanged".equalsIgnoreCase(AdaLangAnalyzerJson.stringOf(result, "baselineState"))) {
        // Already accepted in a prior run; console-text output hides these too.
        continue;
      }
      List<Object> locations = AdaLangAnalyzerJson.listOf(result.get("locations"));
      if (locations.isEmpty()) {
        continue;
      }
      Map<String, Object> physicalLocation =
        AdaLangAnalyzerJson.mapOf(AdaLangAnalyzerJson.mapOf(locations.get(0)).get("physicalLocation"));
      Map<String, Object> artifactLocation = AdaLangAnalyzerJson.mapOf(physicalLocation.get("artifactLocation"));
      Map<String, Object> region = AdaLangAnalyzerJson.mapOf(physicalLocation.get("region"));
      Map<String, Object> message = AdaLangAnalyzerJson.mapOf(result.get("message"));
      Map<String, Object> properties = AdaLangAnalyzerJson.mapOf(result.get("properties"));
      String ruleId = AdaLangAnalyzerJson.stringOf(result, "ruleId");
      RuleMetadata rule = rules.getOrDefault(ruleId, RuleMetadata.EMPTY);

      findings.add(new AdaLangAnalyzerFinding(
        percentDecode(AdaLangAnalyzerJson.stringOf(artifactLocation, "uri")),
        AdaLangAnalyzerJson.intOf(region, "startLine"),
        AdaLangAnalyzerJson.intOf(region, "startColumn"),
        "warning",
        AdaLangAnalyzerJson.stringOf(message, "text"),
        ruleId,
        rule.description(),
        rule.help(),
        AdaLangAnalyzerJson.stringOf(properties, "explanation"),
        AdaLangAnalyzerJson.stringOf(properties, "evidence"),
        "",
        qualitySeverity(AdaLangAnalyzerJson.stringOf(result, "level")),
        "",
        1));
    }

    List<AdaLangAnalyzerProofObligation> proofObligations = proofObligations(run);

    // SARIF reports no file/violation/skipped-check summary counts, only the proof-obligation
    // total implied by the properties.proofObligations array.
    return new AdaLangAnalyzerReport(
      List.copyOf(findings), proofObligations, -1, -1, proofObligations.size(), -1);
  }

  private static List<AdaLangAnalyzerProofObligation> proofObligations(Map<String, Object> run) {
    Map<String, Object> properties = AdaLangAnalyzerJson.mapOf(run.get("properties"));
    List<AdaLangAnalyzerProofObligation> proofObligations = new ArrayList<>();
    for (Object item : AdaLangAnalyzerJson.listOf(properties.get("proofObligations"))) {
      Map<String, Object> obligation = AdaLangAnalyzerJson.mapOf(item);
      proofObligations.add(new AdaLangAnalyzerProofObligation(
        "",
        0,
        0,
        AdaLangAnalyzerJson.stringOf(obligation, "kind"),
        AdaLangAnalyzerJson.stringOf(obligation, "status"),
        "",
        "",
        "",
        "",
        AdaLangAnalyzerJson.stringOf(obligation, "reasonCode"),
        AdaLangAnalyzerJson.stringOf(obligation, "blockingExpression"),
        AdaLangAnalyzerJson.stringOf(obligation, "inlinePath")));
    }
    return proofObligations;
  }

  private static Map<String, RuleMetadata> ruleCatalog(Map<String, Object> run) {
    Map<String, Object> driver = AdaLangAnalyzerJson.mapOf(AdaLangAnalyzerJson.mapOf(run.get("tool")).get("driver"));
    Map<String, RuleMetadata> catalog = new HashMap<>();
    for (Object item : AdaLangAnalyzerJson.listOf(driver.get("rules"))) {
      Map<String, Object> rule = AdaLangAnalyzerJson.mapOf(item);
      String id = AdaLangAnalyzerJson.stringOf(rule, "id");
      String description =
        AdaLangAnalyzerJson.stringOf(AdaLangAnalyzerJson.mapOf(rule.get("shortDescription")), "text");
      String help = AdaLangAnalyzerJson.stringOf(AdaLangAnalyzerJson.mapOf(rule.get("help")), "text");
      catalog.put(id, new RuleMetadata(description, help));
    }
    return catalog;
  }

  private static String qualitySeverity(String level) {
    return switch (level.toLowerCase(Locale.ROOT)) {
      case "error" -> "High";
      case "warning" -> "Medium";
      case "note" -> "Low";
      default -> "";
    };
  }

  private static String percentDecode(String uri) {
    try {
      return URLDecoder.decode(uri, StandardCharsets.UTF_8);
    } catch (IllegalArgumentException malformed) {
      return uri;
    }
  }

  private record RuleMetadata(String description, String help) {
    private static final RuleMetadata EMPTY = new RuleMetadata("", "");
  }
}
