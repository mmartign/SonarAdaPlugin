/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.spazioit.sonarada.adalanganalyzer;

import com.spazioit.sonarada.AdaProperties;
import com.spazioit.sonarada.adacontrol.AdaControlArgumentParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.config.Configuration;

final class AdaLangAnalyzerConfiguration {

  // The file adalang_analyzer auto-discovers in its working directory (Default_Config_File_Name).
  static final String CONFIG_FILE_NAME = "adalang_analyzer.cfg";

  private final Configuration configuration;
  private final FileSystem fileSystem;
  private final AdaControlArgumentParser argumentParser = new AdaControlArgumentParser();

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

  /**
   * The preset name without its switch dashes, such as {@code verify} or {@code do178c=B}. It is
   * passed through as {@code --<preset>} without checking it against a fixed list, so a preset
   * added by a newer analyzer works unchanged and an unknown one is rejected by the analyzer.
   */
  java.util.Optional<String> preset() {
    return configuration.get(AdaProperties.ADALANG_ANALYZER_PRESET_KEY)
      .map(value -> value.trim().replaceFirst("^-+", ""))
      .filter(value -> !value.isEmpty());
  }

  java.util.Optional<String> checks() {
    return configuration.get(AdaProperties.ADALANG_ANALYZER_CHECKS_KEY)
      .map(String::trim)
      .filter(value -> !value.isEmpty());
  }

  java.util.Optional<Path> projectFile() {
    return configuration.get(AdaProperties.ADALANG_ANALYZER_PROJECT_FILE_KEY)
      .map(String::trim)
      .filter(value -> !value.isEmpty())
      .map(this::resolvePath);
  }

  List<String> extraArguments() {
    return argumentParser.parse(configuration.get(AdaProperties.ADALANG_ANALYZER_EXTRA_ARGS_KEY).orElse(""));
  }

  /**
   * True when the analyzer will read a config file: one named with {@code --config} in the extra
   * arguments, or {@code adalang_analyzer.cfg} in the project base directory, which is the
   * analyzer's working directory, unless {@code --no-config} disables that discovery.
   */
  boolean usesConfigFile() {
    List<String> extraArguments = extraArguments();
    if (extraArguments.stream().anyMatch(argument -> argument.equals("--config") || argument.startsWith("--config="))) {
      return true;
    }
    return !extraArguments.contains("--no-config")
      && Files.isRegularFile(fileSystem.baseDir().toPath().resolve(CONFIG_FILE_NAME));
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
