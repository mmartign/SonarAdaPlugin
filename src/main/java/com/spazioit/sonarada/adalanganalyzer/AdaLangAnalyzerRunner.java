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
    Path workDir,
    List<InputFile> inputFiles
  ) throws IOException, InterruptedException {
    Files.createDirectories(workDir);
    List<String> command = buildCommand(configuration, inputFiles);
    ProcessBuilder processBuilder = new ProcessBuilder(command)
      .directory(workDir.toFile())
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
    configuration.checks().ifPresent(checks -> command.add("-checks=" + checks));
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
