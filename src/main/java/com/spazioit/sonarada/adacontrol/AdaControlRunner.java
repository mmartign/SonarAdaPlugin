/*
 * Copyright (C) 2026 Spazio IT
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
