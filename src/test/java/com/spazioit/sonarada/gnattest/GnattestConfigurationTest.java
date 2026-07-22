/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.spazioit.sonarada.gnattest;

import static org.assertj.core.api.Assertions.assertThat;

import com.spazioit.sonarada.AdaProperties;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.config.Configuration;

class GnattestConfigurationTest {

  @Test
  void resolvesReportPathsAgainstProjectDirectory(@TempDir Path projectDirectory) {
    Path absolute = projectDirectory.resolveSibling("absolute.xml").toAbsolutePath();
    GnattestConfiguration configuration = new GnattestConfiguration(
      configuration(" reports/native.txt, " + absolute + ", reports/junit.xml "),
      fileSystem(projectDirectory));

    assertThat(configuration.reportPaths()).containsExactly(
      projectDirectory.resolve("reports/native.txt"),
      absolute.normalize(),
      projectDirectory.resolve("reports/junit.xml"));
  }

  @Test
  void registersGnattestSettings() {
    assertThat(AdaProperties.definitions())
      .extracting(definition -> definition.key())
      .contains(AdaProperties.GNATTEST_REPORT_PATHS_KEY, AdaProperties.GNATTEST_FAIL_ON_ERROR_KEY);
  }

  private static Configuration configuration(String paths) {
    return (Configuration) Proxy.newProxyInstance(
      Configuration.class.getClassLoader(),
      new Class<?>[] {Configuration.class},
      (proxy, method, arguments) -> switch (method.getName()) {
        case "get" -> AdaProperties.GNATTEST_REPORT_PATHS_KEY.equals(arguments[0])
          ? Optional.of(paths)
          : Optional.empty();
        case "getBoolean" -> Optional.empty();
        case "toString" -> "GnattestConfigurationTest.Configuration";
        case "hashCode" -> System.identityHashCode(proxy);
        case "equals" -> proxy == arguments[0];
        default -> throw new UnsupportedOperationException(method.getName());
      });
  }

  private static FileSystem fileSystem(Path directory) {
    return (FileSystem) Proxy.newProxyInstance(
      FileSystem.class.getClassLoader(),
      new Class<?>[] {FileSystem.class},
      (proxy, method, arguments) -> switch (method.getName()) {
        case "baseDir" -> directory.toFile();
        case "toString" -> "GnattestConfigurationTest.FileSystem";
        case "hashCode" -> System.identityHashCode(proxy);
        case "equals" -> proxy == arguments[0];
        default -> throw new UnsupportedOperationException(method.getName());
      });
  }
}
