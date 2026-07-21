/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.spazioit.sonarada.adalanganalyzer;

import com.spazioit.sonarada.AdaProperties;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.config.Configuration;

final class AdaLangAnalyzerConfiguration {

  private final Configuration configuration;
  private final FileSystem fileSystem;

  AdaLangAnalyzerConfiguration(Configuration configuration, FileSystem fileSystem) {
    this.configuration = configuration;
    this.fileSystem = fileSystem;
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

  List<Path> reportPaths() {
    String configuredPaths = configuration.get(AdaProperties.ADALANG_ANALYZER_REPORT_PATHS_KEY).orElse("");
    if (configuredPaths.isBlank()) {
      return List.of();
    }
    return Arrays.stream(configuredPaths.split(","))
      .map(String::trim)
      .filter(path -> !path.isEmpty())
      .map(this::resolvePath)
      .toList();
  }

  int timeoutSeconds() {
    return Math.max(1, configuration.getInt(AdaProperties.ADALANG_ANALYZER_TIMEOUT_SECONDS_KEY).orElse(300));
  }

  boolean failOnError() {
    return configuration.getBoolean(AdaProperties.ADALANG_ANALYZER_FAIL_ON_ERROR_KEY).orElse(true);
  }

  private Path resolvePath(String path) {
    Path candidate = Path.of(path);
    if (candidate.isAbsolute()) {
      return candidate.normalize();
    }
    return fileSystem.baseDir().toPath().resolve(candidate).normalize();
  }
}
