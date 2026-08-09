/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.spazioit.sonarada.adalanganalyzer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AdaLangAnalyzerJsonTest {

  @Test
  void parsesNestedObjectsAndArrays() {
    Map<String, Object> root = AdaLangAnalyzerJson.mapOf(AdaLangAnalyzerJson.parse("""
      {"name": "demo", "count": 3, "enabled": true, "missing": null, "tags": ["a", "b"], \
      "nested": {"inner": 1.5}}
      """));

    assertThat(AdaLangAnalyzerJson.stringOf(root, "name")).isEqualTo("demo");
    assertThat(AdaLangAnalyzerJson.intOf(root, "count")).isEqualTo(3);
    assertThat(AdaLangAnalyzerJson.boolOf(root, "enabled")).isTrue();
    assertThat(root.get("missing")).isNull();
    assertThat(AdaLangAnalyzerJson.listOf(root.get("tags"))).containsExactly("a", "b");
    assertThat(AdaLangAnalyzerJson.mapOf(root.get("nested")).get("inner")).isEqualTo(1.5);
  }

  @Test
  void unescapesStringContent() {
    Object value = AdaLangAnalyzerJson.parse("\"line1\\nline2 \\\"quoted\\\" \\u0041\"");

    assertThat(value).isEqualTo("line1\nline2 \"quoted\" A");
  }

  @Test
  void missingKeysFallBackToNeutralDefaults() {
    Map<String, Object> root = Map.of();

    assertThat(AdaLangAnalyzerJson.stringOf(root, "missing")).isEmpty();
    assertThat(AdaLangAnalyzerJson.intOf(root, "missing")).isEqualTo(-1);
    assertThat(AdaLangAnalyzerJson.boolOf(root, "missing")).isFalse();
    assertThat(AdaLangAnalyzerJson.listOf(null)).isEmpty();
    assertThat(AdaLangAnalyzerJson.mapOf(null)).isEmpty();
  }

  @Test
  void rejectsMalformedInput() {
    assertThatThrownBy(() -> AdaLangAnalyzerJson.parse("{\"a\": }")).isInstanceOf(RuntimeException.class);
  }

  @Test
  void parsesEmptyArraysAndObjects() {
    assertThat(AdaLangAnalyzerJson.parse("[]")).isEqualTo(List.of());
    assertThat(AdaLangAnalyzerJson.parse("{}")).isEqualTo(Map.of());
  }
}
