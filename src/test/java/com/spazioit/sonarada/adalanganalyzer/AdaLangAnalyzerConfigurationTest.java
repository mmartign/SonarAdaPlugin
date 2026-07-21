/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.spazioit.sonarada.adalanganalyzer;

import static org.assertj.core.api.Assertions.assertThat;

import com.spazioit.sonarada.AdaProperties;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.config.Configuration;

class AdaLangAnalyzerConfigurationTest {

  @Test
  void resolvesCommaSeparatedReportPathsAgainstProjectDirectory(@TempDir Path projectDirectory) {
    Path absoluteReport = projectDirectory.resolveSibling("absolute.csv").toAbsolutePath();
    Configuration sonarConfiguration = configuration(
      " reports/first.csv, " + absoluteReport + ", reports/second.csv, ");
    FileSystem fileSystem = fileSystem(projectDirectory);

    AdaLangAnalyzerConfiguration configuration = new AdaLangAnalyzerConfiguration(sonarConfiguration, fileSystem);

    assertThat(configuration.reportPaths()).containsExactly(
      projectDirectory.resolve("reports/first.csv"),
      absoluteReport.normalize(),
      projectDirectory.resolve("reports/second.csv"));
  }

  @Test
  void registersReportPathsAsAnAdaSetting() {
    assertThat(AdaProperties.definitions())
      .extracting(definition -> definition.key())
      .contains(AdaProperties.ADALANG_ANALYZER_REPORT_PATHS_KEY);
  }

  private static Configuration configuration(String reportPaths) {
    return (Configuration) Proxy.newProxyInstance(
      Configuration.class.getClassLoader(),
      new Class<?>[] {Configuration.class},
      (proxy, method, arguments) -> switch (method.getName()) {
        case "get" -> AdaProperties.ADALANG_ANALYZER_REPORT_PATHS_KEY.equals(arguments[0])
          ? Optional.of(reportPaths)
          : Optional.empty();
        case "hasKey" -> AdaProperties.ADALANG_ANALYZER_REPORT_PATHS_KEY.equals(arguments[0]);
        case "toString" -> "AdaLangAnalyzerConfigurationTest.Configuration";
        case "hashCode" -> System.identityHashCode(proxy);
        case "equals" -> proxy == arguments[0];
        default -> throw new UnsupportedOperationException(method.getName());
      });
  }

  private static FileSystem fileSystem(Path projectDirectory) {
    return (FileSystem) Proxy.newProxyInstance(
      FileSystem.class.getClassLoader(),
      new Class<?>[] {FileSystem.class},
      (proxy, method, arguments) -> switch (method.getName()) {
        case "baseDir" -> projectDirectory.toFile();
        case "toString" -> "AdaLangAnalyzerConfigurationTest.FileSystem";
        case "hashCode" -> System.identityHashCode(proxy);
        case "equals" -> proxy == arguments[0];
        default -> throw new UnsupportedOperationException(method.getName());
      });
  }
}
