/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.spazioit.sonarada.adalanganalyzer;

import com.spazioit.sonarada.AdaLanguage;
import com.spazioit.sonarada.AdaProperties;
import com.spazioit.sonarada.analysis.AdaLangAnalyzerIssue;
import com.spazioit.sonarada.analysis.AdaLangAnalyzerReportParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

public final class AdaLangAnalyzerSensor implements Sensor {
  private static final Logger LOG = LoggerFactory.getLogger(AdaLangAnalyzerSensor.class);
  private static final String ENGINE_ID = "SpazioIT AdaLang Analyzer";
  private static final int FINDINGS_EXIT_CODE = 1;

  private final AdaLangAnalyzerRunner runner = new AdaLangAnalyzerRunner();
  private final AdaLangAnalyzerConsoleParser parser = new AdaLangAnalyzerConsoleParser();
  private final AdaLangAnalyzerReportParser reportParser = new AdaLangAnalyzerReportParser();

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.name("Spazio IT AdaLang Analyzer Sensor")
      .onlyOnLanguage(AdaLanguage.KEY)
      .onlyOnFileType(InputFile.Type.MAIN)
      .onlyWhenConfiguration(configuration ->
        configuration.getBoolean(AdaProperties.ADALANG_ANALYZER_ENABLED_KEY).orElse(false)
          || configuration.hasKey(AdaProperties.ADALANG_ANALYZER_REPORT_PATHS_KEY));
  }

  @Override
  public void execute(SensorContext context) {
    AdaLangAnalyzerConfiguration configuration = new AdaLangAnalyzerConfiguration(context.config(), context.fileSystem());
    List<InputFile> inputFiles = adaInputFiles(context);
    if (inputFiles.isEmpty()) {
      LOG.debug("Skipping AdaLang Analyzer because no Ada input files were found");
      return;
    }

    if (configuration.enabled()) {
      runAnalyzer(context, configuration, inputFiles);
    }

    importReports(context, configuration, inputFiles);
  }

  private void runAnalyzer(
    SensorContext context,
    AdaLangAnalyzerConfiguration configuration,
    List<InputFile> inputFiles
  ) {
    try {
      AdaLangAnalyzerExecutionResult result = runner.run(
        configuration, context.fileSystem().workDir().toPath(), inputFiles);
      if (result.timedOut() || result.exitCode() > FINDINGS_EXIT_CODE) {
        handleError(configuration, result.output().isBlank()
          ? "AdaLang Analyzer exited with code " + result.exitCode()
          : result.output());
        return;
      }
      List<AdaLangAnalyzerFinding> findings = parser.parse(result.output());
      if (result.exitCode() == FINDINGS_EXIT_CODE && findings.isEmpty()) {
        handleError(configuration, "AdaLang Analyzer exited with code 1 but produced no parseable findings. " + result.output());
        return;
      }
      publishFindings(context, inputFiles, findings);
    } catch (IOException e) {
      handleError(configuration, "Unable to run AdaLang Analyzer: " + e.getMessage());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      handleError(configuration, "AdaLang Analyzer execution was interrupted");
    }
  }

  private void importReports(
    SensorContext context,
    AdaLangAnalyzerConfiguration configuration,
    List<InputFile> inputFiles
  ) {
    List<Path> reportPaths = configuration.reportPaths();
    if (reportPaths.isEmpty()) {
      return;
    }

    AdaLangAnalyzerFileIndex fileIndex = new AdaLangAnalyzerFileIndex(
      context.fileSystem().baseDir().toPath(), inputFiles);
    int imported = 0;
    int unresolved = 0;
    for (Path reportPath : reportPaths) {
      try {
        String reportContent = Files.readString(reportPath, StandardCharsets.UTF_8);
        List<AdaLangAnalyzerFinding> findings = parser.parse(reportContent);
        int reportedViolations = parser.reportedViolationCount(reportContent);
        if (reportedViolations >= 0 && reportedViolations != findings.size()) {
          handleError(configuration, "AdaLang Analyzer report '" + reportPath + "' declares "
            + reportedViolations + " violation(s), but " + findings.size() + " could be parsed");
          continue;
        }

        if (reportedViolations >= 0 || !findings.isEmpty()) {
          for (AdaLangAnalyzerFinding finding : findings) {
            java.util.Optional<InputFile> inputFile = fileIndex.find(finding.file());
            if (inputFile.isPresent()) {
              saveExternalIssue(context, inputFile.get(), finding);
              imported++;
            } else {
              unresolved++;
            }
          }
          continue;
        }

        for (AdaLangAnalyzerIssue issue : reportParser.parse(reportPath)) {
          java.util.Optional<InputFile> inputFile = fileIndex.find(issue.file());
          if (inputFile.isPresent()) {
            saveExternalIssue(context, inputFile.get(), issue);
            imported++;
          } else {
            unresolved++;
          }
        }
      } catch (IOException e) {
        handleError(configuration, "Unable to read AdaLang Analyzer report '" + reportPath + "': " + e.getMessage());
      }
    }

    LOG.info("AdaLang Analyzer report import completed: {} issue(s) imported, {} issue(s) skipped because their file was not indexed by Sonar",
      imported, unresolved);
  }

  private static void publishFindings(
    SensorContext context,
    List<InputFile> inputFiles,
    List<AdaLangAnalyzerFinding> findings
  ) {
    AdaLangAnalyzerFileIndex fileIndex = new AdaLangAnalyzerFileIndex(
      context.fileSystem().baseDir().toPath(), inputFiles);
    int imported = 0;
    int unresolved = 0;
    for (AdaLangAnalyzerFinding finding : findings) {
      java.util.Optional<InputFile> inputFile = fileIndex.find(finding.file());
      if (inputFile.isPresent()) {
        saveExternalIssue(context, inputFile.get(), finding);
        imported++;
      } else {
        unresolved++;
      }
    }
    LOG.info("AdaLang Analyzer import completed: {} issue(s) imported, {} issue(s) skipped because their file was not indexed by Sonar",
      imported, unresolved);
  }

  private static List<InputFile> adaInputFiles(SensorContext context) {
    FilePredicates predicates = context.fileSystem().predicates();
    List<InputFile> files = new ArrayList<>();
    context.fileSystem().inputFiles(predicates.and(
      predicates.hasLanguage(AdaLanguage.KEY), predicates.hasType(InputFile.Type.MAIN))).forEach(files::add);
    return List.copyOf(files);
  }

  private static void saveExternalIssue(SensorContext context, InputFile inputFile, AdaLangAnalyzerFinding finding) {
    SoftwareQuality softwareQuality = sonarSoftwareQuality(finding.softwareQuality());
    org.sonar.api.issue.impact.Severity impactSeverity = sonarImpactSeverity(finding);
    NewExternalIssue issue = context.newExternalIssue()
      .engineId(ENGINE_ID)
      .ruleId(finding.ruleId())
      .type(sonarRuleType(softwareQuality))
      .cleanCodeAttribute(CleanCodeAttribute.CONVENTIONAL)
      .severity(sonarSeverity(impactSeverity))
      .addImpact(softwareQuality, impactSeverity)
      .remediationEffortMinutes(10L);

    NewIssueLocation location = issue.newLocation().on(inputFile).message(finding.sonarMessage());
    int line = Math.max(1, Math.min(inputFile.lines(), finding.line()));
    try {
      int start = Math.max(0, finding.column() - 1);
      location.at(inputFile.newRange(line, start, line, start + 1));
    } catch (RuntimeException invalidRange) {
      location.at(inputFile.selectLine(line));
    }
    issue.at(location).save();
  }

  private static void saveExternalIssue(SensorContext context, InputFile inputFile, AdaLangAnalyzerIssue importedIssue) {
    org.sonar.api.issue.impact.Severity impactSeverity = "Error".equalsIgnoreCase(importedIssue.key())
      ? org.sonar.api.issue.impact.Severity.MEDIUM
      : org.sonar.api.issue.impact.Severity.LOW;
    NewExternalIssue issue = context.newExternalIssue()
      .engineId(ENGINE_ID)
      .ruleId(importedIssue.ruleId())
      .type(RuleType.CODE_SMELL)
      .cleanCodeAttribute(CleanCodeAttribute.CONVENTIONAL)
      .severity(sonarSeverity(impactSeverity))
      .addImpact(SoftwareQuality.MAINTAINABILITY, impactSeverity)
      .remediationEffortMinutes(10L);

    NewIssueLocation location = issue.newLocation().on(inputFile).message(importedIssue.sonarMessage());
    int line = Math.max(1, Math.min(inputFile.lines(), importedIssue.line()));
    try {
      int start = Math.max(0, importedIssue.column() - 1);
      location.at(inputFile.newRange(line, start, line, start + 1));
    } catch (RuntimeException invalidRange) {
      location.at(inputFile.selectLine(line));
    }
    issue.at(location).save();
  }

  private static SoftwareQuality sonarSoftwareQuality(String quality) {
    if ("security".equalsIgnoreCase(quality)) {
      return SoftwareQuality.SECURITY;
    }
    if ("reliability".equalsIgnoreCase(quality)) {
      return SoftwareQuality.RELIABILITY;
    }
    return SoftwareQuality.MAINTAINABILITY;
  }

  private static org.sonar.api.issue.impact.Severity sonarImpactSeverity(AdaLangAnalyzerFinding finding) {
    if ("high".equalsIgnoreCase(finding.qualitySeverity())) {
      return org.sonar.api.issue.impact.Severity.HIGH;
    }
    if ("medium".equalsIgnoreCase(finding.qualitySeverity())
      || "error".equalsIgnoreCase(finding.severity())) {
      return org.sonar.api.issue.impact.Severity.MEDIUM;
    }
    return org.sonar.api.issue.impact.Severity.LOW;
  }

  private static Severity sonarSeverity(org.sonar.api.issue.impact.Severity severity) {
    return switch (severity) {
      case BLOCKER -> Severity.BLOCKER;
      case HIGH -> Severity.CRITICAL;
      case MEDIUM -> Severity.MAJOR;
      case LOW -> Severity.MINOR;
      case INFO -> Severity.INFO;
    };
  }

  private static RuleType sonarRuleType(SoftwareQuality quality) {
    return switch (quality) {
      case SECURITY -> RuleType.VULNERABILITY;
      case RELIABILITY -> RuleType.BUG;
      case MAINTAINABILITY -> RuleType.CODE_SMELL;
    };
  }

  private static void handleError(AdaLangAnalyzerConfiguration configuration, String message) {
    if (configuration.failOnError()) {
      throw new IllegalStateException(message);
    }
    LOG.warn(message);
  }
}
