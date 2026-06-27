package com.spazioit.sonarada.adacontrol;

import com.spazioit.sonarada.AdaLanguage;
import com.spazioit.sonarada.AdaProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.rules.RuleType;

public final class AdaControlSensor implements Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(AdaControlSensor.class);
  private static final String ENGINE_ID = "AdaControl";
  private static final int ADACONTROL_VIOLATIONS_EXIT_CODE = 1;

  private final AdaControlRunner runner = new AdaControlRunner();
  private final AdaControlReportParser parser = new AdaControlReportParser();

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .name("AdaControl Sensor")
      .onlyOnLanguage(AdaLanguage.KEY)
      .onlyOnFileType(InputFile.Type.MAIN)
      .onlyWhenConfiguration(configuration ->
        configuration.getBoolean(AdaProperties.ADACONTROL_ENABLED_KEY).orElse(false)
          || configuration.hasKey(AdaProperties.ADACONTROL_REPORT_PATHS_KEY));
  }

  @Override
  public void execute(SensorContext context) {
    AdaControlConfiguration configuration = new AdaControlConfiguration(context.config(), context.fileSystem());
    List<InputFile> inputFiles = adaInputFiles(context);
    if (inputFiles.isEmpty()) {
      LOG.debug("Skipping AdaControl because no Ada input files were found");
      return;
    }

    List<Path> reportPaths = new ArrayList<>(configuration.reportPaths());
    if (configuration.enabled()) {
      runAdaControl(context, configuration, inputFiles).ifPresent(reportPaths::add);
    }

    if (reportPaths.isEmpty()) {
      return;
    }

    AdaControlFileIndex fileIndex = new AdaControlFileIndex(context.fileSystem().baseDir().toPath(), inputFiles);
    int imported = 0;
    int unresolved = 0;
    for (Path reportPath : reportPaths) {
      ImportCounts counts = importReport(context, configuration, fileIndex, reportPath);
      imported += counts.imported();
      unresolved += counts.unresolved();
    }

    LOG.info("AdaControl import completed: {} issue(s) imported, {} issue(s) skipped because their file was not indexed by Sonar", imported, unresolved);
  }

  private java.util.Optional<Path> runAdaControl(SensorContext context, AdaControlConfiguration configuration, List<InputFile> inputFiles) {
    try {
      AdaControlExecutionResult result = runner.run(configuration, context.fileSystem().workDir().toPath(), inputFiles);
      if (result.timedOut()) {
        handleExecutionError(configuration, result.consoleOutput());
        return java.util.Optional.empty();
      }

      if (result.exitCode() > ADACONTROL_VIOLATIONS_EXIT_CODE) {
        handleExecutionError(configuration, "AdaControl exited with code " + result.exitCode() + ". " + result.consoleOutput());
        return java.util.Optional.empty();
      }

      if (!Files.exists(result.reportPath())) {
        handleExecutionError(configuration, "AdaControl did not create the expected report: " + result.reportPath());
        return java.util.Optional.empty();
      }

      return java.util.Optional.of(result.reportPath());
    } catch (IOException e) {
      handleExecutionError(configuration, "Unable to run AdaControl: " + e.getMessage());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      handleExecutionError(configuration, "AdaControl execution was interrupted");
    }
    return java.util.Optional.empty();
  }

  private ImportCounts importReport(SensorContext context, AdaControlConfiguration configuration, AdaControlFileIndex fileIndex, Path reportPath) {
    try {
      int imported = 0;
      int unresolved = 0;
      for (AdaControlIssue issue : parser.parse(reportPath)) {
        java.util.Optional<InputFile> inputFile = fileIndex.find(issue.file());
        if (inputFile.isPresent()) {
          saveExternalIssue(context, inputFile.get(), issue);
          imported++;
        } else {
          unresolved++;
        }
      }
      return new ImportCounts(imported, unresolved);
    } catch (IOException e) {
      handleExecutionError(configuration, "Unable to read AdaControl report '" + reportPath + "': " + e.getMessage());
      return new ImportCounts(0, 0);
    }
  }

  private static List<InputFile> adaInputFiles(SensorContext context) {
    FilePredicates predicates = context.fileSystem().predicates();
    List<InputFile> inputFiles = new ArrayList<>();
    context.fileSystem().inputFiles(
      predicates.and(
        predicates.hasLanguage(AdaLanguage.KEY),
        predicates.hasType(InputFile.Type.MAIN)
      )
    ).forEach(inputFiles::add);
    return List.copyOf(inputFiles);
  }

  private static void saveExternalIssue(SensorContext context, InputFile inputFile, AdaControlIssue issue) {
    NewExternalIssue externalIssue = context.newExternalIssue()
      .engineId(ENGINE_ID)
      .ruleId(issue.ruleId())
      .type(RuleType.CODE_SMELL)
      .cleanCodeAttribute(CleanCodeAttribute.CONVENTIONAL)
      .severity(toSonarSeverity(issue.key()))
      .addImpact(SoftwareQuality.MAINTAINABILITY, toImpactSeverity(issue.key()))
      .remediationEffortMinutes(10L);

    NewIssueLocation location = externalIssue.newLocation()
      .on(inputFile)
      .message(issue.sonarMessage());
    try {
      int line = Math.max(1, Math.min(inputFile.lines(), issue.line()));
      int start = Math.max(0, issue.column() - 1);
      location.at(inputFile.newRange(line, start, line, start + 1));
    } catch (RuntimeException invalidRange) {
      location.at(inputFile.selectLine(Math.max(1, Math.min(inputFile.lines(), issue.line()))));
    }

    externalIssue.at(location).save();
  }

  private static Severity toSonarSeverity(String key) {
    return "Error".equalsIgnoreCase(key) ? Severity.MAJOR : Severity.MINOR;
  }

  private static org.sonar.api.issue.impact.Severity toImpactSeverity(String key) {
    return "Error".equalsIgnoreCase(key)
      ? org.sonar.api.issue.impact.Severity.MEDIUM
      : org.sonar.api.issue.impact.Severity.LOW;
  }

  private static void handleExecutionError(AdaControlConfiguration configuration, String message) {
    if (configuration.failOnError()) {
      throw new IllegalStateException(message);
    }
    LOG.warn(message);
  }

  private record ImportCounts(int imported, int unresolved) {
  }
}
