/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.spazioit.sonarada.gnattest;

import com.spazioit.sonarada.AdaProperties;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.config.Configuration;

final class GnattestConfiguration {

  private final Configuration configuration;
  private final FileSystem fileSystem;

  GnattestConfiguration(Configuration configuration, FileSystem fileSystem) {
    this.configuration = configuration;
    this.fileSystem = fileSystem;
  }

  List<Path> reportPaths() {
    String configured = configuration.get(AdaProperties.GNATTEST_REPORT_PATHS_KEY).orElse("");
    if (configured.isBlank()) {
      return List.of();
    }
    return Arrays.stream(configured.split(","))
      .map(String::trim)
      .filter(path -> !path.isEmpty())
      .map(this::resolve)
      .toList();
  }

  boolean failOnError() {
    return configuration.getBoolean(AdaProperties.GNATTEST_FAIL_ON_ERROR_KEY).orElse(true);
  }

  private Path resolve(String value) {
    Path path = Path.of(value);
    return (path.isAbsolute() ? path : fileSystem.baseDir().toPath().resolve(path)).normalize();
  }
}
