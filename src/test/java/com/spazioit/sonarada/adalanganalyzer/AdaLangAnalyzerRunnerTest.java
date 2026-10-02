/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.spazioit.sonarada.adalanganalyzer;

import static org.assertj.core.api.Assertions.assertThat;

import com.spazioit.sonarada.AdaProperties;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.config.Configuration;

class AdaLangAnalyzerRunnerTest {

  @Test
  void fallsBackToRecommendedWhenNothingSelectsTheAnalysis(@TempDir Path projectDirectory) {
    assertThat(command(projectDirectory, Map.of()))
      .containsExactly("adalang_analyzer", "-v", "--recommended", "/project/src/a.adb", "/project/src/b.adb");
  }

  @Test
  void passesTheConfiguredPreset(@TempDir Path projectDirectory) {
    assertThat(command(projectDirectory, Map.of(AdaProperties.ADALANG_ANALYZER_PRESET_KEY, "verify")))
      .startsWith("adalang_analyzer", "-v", "--verify", "/project/src/a.adb");
    assertThat(command(projectDirectory, Map.of(AdaProperties.ADALANG_ANALYZER_PRESET_KEY, " --do178c=B ")))
      .startsWith("adalang_analyzer", "-v", "--do178c=B", "/project/src/a.adb");
  }

  @Test
  void passesChecksAloneWithoutAPreset(@TempDir Path projectDirectory) {
    assertThat(command(projectDirectory, Map.of(AdaProperties.ADALANG_ANALYZER_CHECKS_KEY, "No_Goto,Division_By_Zero")))
      .startsWith("adalang_analyzer", "-v", "-checks=No_Goto,Division_By_Zero", "/project/src/a.adb");
  }

  @Test
  void passesChecksAfterThePresetTheyRefine(@TempDir Path projectDirectory) {
    // A preset resets every check, so -checks= given before it would be discarded.
    assertThat(command(projectDirectory, Map.of(
      AdaProperties.ADALANG_ANALYZER_PRESET_KEY, "verify",
      AdaProperties.ADALANG_ANALYZER_CHECKS_KEY, "-Missing_Depends_Contract")))
      .startsWith("adalang_analyzer", "-v", "--verify", "-checks=-Missing_Depends_Contract", "/project/src/a.adb");
  }

  @Test
  void leavesTheSelectionToAProjectConfigFile(@TempDir Path projectDirectory) throws IOException {
    // The analyzer reads the config file's flags before the command line, so a
    // --recommended added here would reset a --verify the file selected.
    Files.writeString(projectDirectory.resolve(AdaLangAnalyzerConfiguration.CONFIG_FILE_NAME), "--verify\n");

    assertThat(command(projectDirectory, Map.of()))
      .containsExactly("adalang_analyzer", "-v", "/project/src/a.adb", "/project/src/b.adb");
    assertThat(command(projectDirectory, Map.of(AdaProperties.ADALANG_ANALYZER_PRESET_KEY, "spark")))
      .startsWith("adalang_analyzer", "-v", "--spark", "/project/src/a.adb");
    assertThat(command(projectDirectory, Map.of(AdaProperties.ADALANG_ANALYZER_EXTRA_ARGS_KEY, "--no-config")))
      .startsWith("adalang_analyzer", "-v", "--recommended", "--no-config", "/project/src/a.adb");
  }

  @Test
  void leavesTheSelectionToAnExplicitConfigFile(@TempDir Path projectDirectory) {
    assertThat(command(projectDirectory, Map.of(AdaProperties.ADALANG_ANALYZER_EXTRA_ARGS_KEY, "--config=ci/adalang.cfg")))
      .startsWith("adalang_analyzer", "-v", "--config=ci/adalang.cfg", "/project/src/a.adb");
  }

  @Test
  void passesProjectFileAndExtraArgumentsBeforeTheInputFiles(@TempDir Path projectDirectory) {
    assertThat(command(projectDirectory, Map.of(
      AdaProperties.ADALANG_ANALYZER_EXECUTABLE_KEY, "/opt/bin/adalang_analyzer",
      AdaProperties.ADALANG_ANALYZER_PRESET_KEY, "verify",
      AdaProperties.ADALANG_ANALYZER_PROJECT_FILE_KEY, "my_project.gpr",
      AdaProperties.ADALANG_ANALYZER_EXTRA_ARGS_KEY, "-XBUILD_MODE=release -complexity-threshold=15")))
      .containsExactly(
        "/opt/bin/adalang_analyzer", "-v", "--verify",
        "-P" + projectDirectory.resolve("my_project.gpr"),
        "-XBUILD_MODE=release", "-complexity-threshold=15",
        "/project/src/a.adb", "/project/src/b.adb");
  }

  private static List<String> command(Path baseDir, Map<String, String> values) {
    return AdaLangAnalyzerRunner.buildCommand(
      new AdaLangAnalyzerConfiguration(settings(values), fileSystem(baseDir)),
      List.of(inputFile("/project/src/b.adb"), inputFile("/project/src/a.adb")));
  }

  private static InputFile inputFile(String absolutePath) {
    return (InputFile) Proxy.newProxyInstance(
      InputFile.class.getClassLoader(),
      new Class<?>[] {InputFile.class},
      (proxy, method, arguments) -> switch (method.getName()) {
        case "absolutePath" -> absolutePath;
        case "toString" -> absolutePath;
        case "hashCode" -> System.identityHashCode(proxy);
        case "equals" -> proxy == arguments[0];
        default -> throw new UnsupportedOperationException(method.getName());
      });
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
        case "toString" -> "AdaLangAnalyzerRunnerTest.Configuration";
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
        case "toString" -> "AdaLangAnalyzerRunnerTest.FileSystem";
        case "hashCode" -> System.identityHashCode(proxy);
        case "equals" -> proxy == arguments[0];
        default -> throw new UnsupportedOperationException(method.getName());
      });
  }
}
