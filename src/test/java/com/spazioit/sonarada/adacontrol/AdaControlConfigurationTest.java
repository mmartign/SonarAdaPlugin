/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.spazioit.sonarada.adacontrol;

import static org.assertj.core.api.Assertions.assertThat;

import com.spazioit.sonarada.AdaProperties;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.config.Configuration;

class AdaControlConfigurationTest {

  @Test
  void enabledDefaultsToFalseAndReadsSetting(@TempDir Path projectDirectory) {
    AdaControlConfiguration disabled = configuration(projectDirectory, Map.of());
    AdaControlConfiguration enabled = configuration(projectDirectory, Map.of(AdaProperties.ADACONTROL_ENABLED_KEY, "true"));

    assertThat(disabled.enabled()).isFalse();
    assertThat(enabled.enabled()).isTrue();
  }

  @Test
  void executableDefaultsToAdactlAndUsesConfiguredValue(@TempDir Path projectDirectory) {
    AdaControlConfiguration unset = configuration(projectDirectory, Map.of());
    AdaControlConfiguration blank = configuration(projectDirectory, Map.of(AdaProperties.ADACONTROL_EXECUTABLE_KEY, "  "));
    AdaControlConfiguration custom = configuration(projectDirectory, Map.of(AdaProperties.ADACONTROL_EXECUTABLE_KEY, "/opt/bin/adactl"));

    assertThat(unset.executable()).isEqualTo("adactl");
    assertThat(blank.executable()).isEqualTo("adactl");
    assertThat(custom.executable()).isEqualTo("/opt/bin/adactl");
  }

  @Test
  void rulesFileAndProjectFileResolveRelativeToBaseDirAndAreEmptyWhenUnset(@TempDir Path projectDirectory) {
    AdaControlConfiguration unset = configuration(projectDirectory, Map.of());
    AdaControlConfiguration configured = configuration(projectDirectory, Map.of(
      AdaProperties.ADACONTROL_RULES_FILE_KEY, " adactl.aru ",
      AdaProperties.ADACONTROL_PROJECT_FILE_KEY, "my_project.gpr"));

    assertThat(unset.rulesFile()).isEmpty();
    assertThat(unset.projectFile()).isEmpty();
    assertThat(configured.rulesFile()).contains(projectDirectory.resolve("adactl.aru"));
    assertThat(configured.projectFile()).contains(projectDirectory.resolve("my_project.gpr"));
  }

  @Test
  void extraArgumentsDelegatesToArgumentParser(@TempDir Path projectDirectory) {
    AdaControlConfiguration unset = configuration(projectDirectory, Map.of());
    AdaControlConfiguration configured = configuration(projectDirectory, Map.of(
      AdaProperties.ADACONTROL_EXTRA_ARGS_KEY, "-- -gnat12"));

    assertThat(unset.extraArguments()).isEmpty();
    assertThat(configured.extraArguments()).containsExactly("--", "-gnat12");
  }

  @Test
  void reportPathsSplitsTrimsAndResolvesAgainstBaseDirectory(@TempDir Path projectDirectory) {
    Path absolute = projectDirectory.resolveSibling("external-report.csv").toAbsolutePath();
    AdaControlConfiguration configuration = configuration(projectDirectory, Map.of(
      AdaProperties.ADACONTROL_REPORT_PATHS_KEY, " build/report.csv, " + absolute + " "));

    assertThat(configuration.reportPaths()).containsExactly(
      projectDirectory.resolve("build/report.csv"),
      absolute.normalize());
  }

  @Test
  void timeoutSecondsDefaultsAndClampsToAMinimumOfOne(@TempDir Path projectDirectory) {
    AdaControlConfiguration unset = configuration(projectDirectory, Map.of());
    AdaControlConfiguration configured = configuration(projectDirectory, Map.of(AdaProperties.ADACONTROL_TIMEOUT_SECONDS_KEY, "45"));
    AdaControlConfiguration zero = configuration(projectDirectory, Map.of(AdaProperties.ADACONTROL_TIMEOUT_SECONDS_KEY, "0"));

    assertThat(unset.timeoutSeconds()).isEqualTo(300);
    assertThat(configured.timeoutSeconds()).isEqualTo(45);
    assertThat(zero.timeoutSeconds()).isEqualTo(1);
  }

  @Test
  void failOnErrorDefaultsToTrueAndReadsSetting(@TempDir Path projectDirectory) {
    AdaControlConfiguration unset = configuration(projectDirectory, Map.of());
    AdaControlConfiguration disabled = configuration(projectDirectory, Map.of(AdaProperties.ADACONTROL_FAIL_ON_ERROR_KEY, "false"));

    assertThat(unset.failOnError()).isTrue();
    assertThat(disabled.failOnError()).isFalse();
  }

  private static AdaControlConfiguration configuration(Path baseDir, Map<String, String> values) {
    return new AdaControlConfiguration(settings(values), fileSystem(baseDir));
  }

  private static Configuration settings(Map<String, String> values) {
    Map<String, String> copy = new HashMap<>(values);
    return (Configuration) Proxy.newProxyInstance(
      Configuration.class.getClassLoader(),
      new Class<?>[] {Configuration.class},
      (proxy, method, arguments) -> switch (method.getName()) {
        case "get" -> Optional.ofNullable(copy.get(arguments[0]));
        case "getBoolean" -> Optional.ofNullable(copy.get(arguments[0])).map(Boolean::parseBoolean);
        case "getInt" -> Optional.ofNullable(copy.get(arguments[0])).map(Integer::parseInt);
        case "hasKey" -> copy.containsKey(arguments[0]);
        case "toString" -> "AdaControlConfigurationTest.Configuration";
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
        case "toString" -> "AdaControlConfigurationTest.FileSystem";
        case "hashCode" -> System.identityHashCode(proxy);
        case "equals" -> proxy == arguments[0];
        default -> throw new UnsupportedOperationException(method.getName());
      });
  }
}
