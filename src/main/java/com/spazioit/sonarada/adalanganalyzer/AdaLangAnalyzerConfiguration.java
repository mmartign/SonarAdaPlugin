/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.spazioit.sonarada.adalanganalyzer;

import com.spazioit.sonarada.AdaProperties;
import org.sonar.api.config.Configuration;

final class AdaLangAnalyzerConfiguration {

  private final Configuration configuration;

  AdaLangAnalyzerConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }

  boolean enabled() {
    return configuration.getBoolean(AdaProperties.ADALANG_ANALYZER_ENABLED_KEY).orElse(false);
  }

  String executable() {
    return configuration.get(AdaProperties.ADALANG_ANALYZER_EXECUTABLE_KEY)
      .map(String::trim)
      .filter(value -> !value.isEmpty())
      .orElse("adalang_analyzer");
  }

  java.util.Optional<String> checks() {
    return configuration.get(AdaProperties.ADALANG_ANALYZER_CHECKS_KEY)
      .map(String::trim)
      .filter(value -> !value.isEmpty());
  }

  int timeoutSeconds() {
    return Math.max(1, configuration.getInt(AdaProperties.ADALANG_ANALYZER_TIMEOUT_SECONDS_KEY).orElse(300));
  }

  boolean failOnError() {
    return configuration.getBoolean(AdaProperties.ADALANG_ANALYZER_FAIL_ON_ERROR_KEY).orElse(true);
  }
}
