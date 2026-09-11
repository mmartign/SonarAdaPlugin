/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.spazioit.sonarada.adalanganalyzer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.sonar.api.batch.fs.IndexedFile;
import org.sonar.api.batch.fs.InputFile;

@SuppressWarnings("deprecation")
final class AdaLangAnalyzerRunner {

  AdaLangAnalyzerExecutionResult run(
    AdaLangAnalyzerConfiguration configuration,
    Path baseDir,
    List<InputFile> inputFiles
  ) throws IOException, InterruptedException {
    Files.createDirectories(baseDir);
    List<String> command = buildCommand(configuration, inputFiles);
    // Run from the project base directory, not the scanner's work directory, so AdaLang
    // Analyzer's auto-discovery of adalang_analyzer.cfg (current working directory only,
    // no upward search) can find a config file placed at the project root.
    ProcessBuilder processBuilder = new ProcessBuilder(command)
      .directory(baseDir.toFile())
      .redirectErrorStream(true);

    Process process = processBuilder.start();
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Future<String> output = executor.submit(() -> new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
    boolean completed = process.waitFor(configuration.timeoutSeconds(), TimeUnit.SECONDS);
    if (!completed) {
      process.destroyForcibly();
      executor.shutdownNow();
      String message = "AdaLang Analyzer timed out after " + Duration.ofSeconds(configuration.timeoutSeconds()).toSeconds()
        + " seconds while running: " + String.join(" ", command);
      return new AdaLangAnalyzerExecutionResult(-1, message, true);
    }

    executor.shutdown();
    return new AdaLangAnalyzerExecutionResult(process.exitValue(), readOutput(output), false);
  }

  static List<String> buildCommand(AdaLangAnalyzerConfiguration configuration, List<InputFile> inputFiles) {
    List<String> command = new ArrayList<>();
    command.add(configuration.executable());
    // Without -v, AdaLang Analyzer's text summary suppresses per-obligation detail lines
    // (just prints the totals), which AdaLangAnalyzerConsoleParser cannot turn into
    // individual findings. The resulting mismatch between the reported proof obligation
    // count and the zero obligations actually parsed then fails the whole scan (see
    // AdaLangAnalyzerSensor#hasConsistentCounts), so -v is required whenever any
    // proof-obligation-producing check is enabled.
    command.add("-v");
    // AdaLang Analyzer enables no checks by default (see Adalang_Analyzer.Config.Rule_States),
    // so running it with no explicit selection is a silent no-op that reports zero findings.
    // Fall back to --recommended when the user has not configured sonar.ada.adalang.checks.
    java.util.Optional<String> checks = configuration.checks();
    if (checks.isPresent()) {
      command.add("-checks=" + checks.get());
    } else {
      command.add("--recommended");
    }
    inputFiles.stream()
      .map(IndexedFile.class::cast)
      .map(IndexedFile::absolutePath)
      .sorted()
      .forEach(command::add);
    return List.copyOf(command);
  }

  private static String readOutput(Future<String> output) throws InterruptedException {
    try {
      return output.get();
    } catch (ExecutionException e) {
      return e.getCause() == null ? e.getMessage() : e.getCause().getMessage();
    }
  }
}
