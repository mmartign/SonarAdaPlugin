/*
 * Copyright (C) 2026 Spazio IT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.spazioit.sonarada.adacontrol;

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
final class AdaControlRunner {

  AdaControlExecutionResult run(
    AdaControlConfiguration configuration,
    Path workDir,
    List<InputFile> inputFiles
  ) throws IOException, InterruptedException {
    Files.createDirectories(workDir);
    Path reportPath = workDir.resolve("adacontrol-report.csv").normalize();
    Files.deleteIfExists(reportPath);

    List<String> command = buildCommand(configuration, reportPath, inputFiles);
    ProcessBuilder processBuilder = new ProcessBuilder(command);
    processBuilder.directory(workDir.toFile());
    processBuilder.redirectErrorStream(true);

    Process process = processBuilder.start();
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Future<String> consoleOutput = executor.submit(() -> new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
    boolean completed = process.waitFor(configuration.timeoutSeconds(), TimeUnit.SECONDS);

    if (!completed) {
      process.destroyForcibly();
      executor.shutdownNow();
      return new AdaControlExecutionResult(-1, reportPath, timeoutMessage(command, configuration.timeoutSeconds()), true);
    }

    executor.shutdown();
    return new AdaControlExecutionResult(process.exitValue(), reportPath, readConsoleOutput(consoleOutput), false);
  }

  private static List<String> buildCommand(AdaControlConfiguration configuration, Path reportPath, List<InputFile> inputFiles) {
    List<String> command = new ArrayList<>();
    command.add(configuration.executable());
    command.add("-F");
    command.add("CSV_Long");
    command.add("-w");
    command.add("-o");
    command.add(reportPath.toString());

    configuration.rulesFile().ifPresent(path -> {
      command.add("-f");
      command.add(path.toString());
    });
    configuration.projectFile().ifPresent(path -> {
      command.add("-p");
      command.add(path.toString());
    });

    inputFiles.stream()
      .map(IndexedFile.class::cast)
      .map(IndexedFile::absolutePath)
      .sorted()
      .forEach(command::add);
    command.addAll(configuration.extraArguments());
    return command;
  }

  private static String readConsoleOutput(Future<String> consoleOutput) throws InterruptedException {
    try {
      return consoleOutput.get();
    } catch (ExecutionException e) {
      return e.getCause() == null ? e.getMessage() : e.getCause().getMessage();
    }
  }

  private static String timeoutMessage(List<String> command, int timeoutSeconds) {
    return "AdaControl timed out after " + Duration.ofSeconds(timeoutSeconds).toSeconds()
      + " seconds while running: " + String.join(" ", command);
  }
}
