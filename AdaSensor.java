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
package com.spazioit.sonarada;

import com.spazioit.sonarada.analysis.AdaAnalysis;
import com.spazioit.sonarada.analysis.AdaAnalysisConfig;
import com.spazioit.sonarada.analysis.AdaCpdToken;
import com.spazioit.sonarada.analysis.AdaHighlight;
import com.spazioit.sonarada.analysis.AdaHighlightKind;
import com.spazioit.sonarada.analysis.AdaIssue;
import com.spazioit.sonarada.analysis.AdaMetrics;
import com.spazioit.sonarada.analysis.LibadalangAnalyzer;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.cpd.NewCpdTokens;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.rule.RuleKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AdaSensor implements Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(AdaSensor.class);

  private final LibadalangAnalyzer analyzer = new LibadalangAnalyzer();

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .name("Ada Sensor")
      .onlyOnLanguage(AdaLanguage.KEY)
      .onlyOnFileType(InputFile.Type.MAIN)
      .createIssuesForRuleRepository(AdaRulesDefinition.REPOSITORY_KEY)
      .processesFilesIndependently();
  }

  @Override
  public void execute(SensorContext context) {
    FilePredicates predicates = context.fileSystem().predicates();
    Iterable<InputFile> inputFiles = context.fileSystem().inputFiles(
      predicates.and(
        predicates.hasLanguage(AdaLanguage.KEY),
        predicates.hasType(InputFile.Type.MAIN)
      )
    );
    AdaAnalysisConfig config = analysisConfig(context);

    int count = 0;
    for (InputFile inputFile : inputFiles) {
      if (context.isCancelled()) {
        break;
      }
      analyzeFile(context, inputFile, config);
      count++;
    }

    LOG.info("Ada analysis completed for {} file(s)", count);
  }

  private void analyzeFile(SensorContext context, InputFile inputFile, AdaAnalysisConfig config) {
    try {
      AdaAnalysis analysis = analyzer.analyze(inputFile.contents(), config);
      saveMetrics(context, inputFile, analysis.metrics());
      saveIssues(context, inputFile, analysis);
      saveHighlighting(context, inputFile, analysis);
      saveCpdTokens(context, inputFile, analysis);
    } catch (IOException e) {
      LOG.warn("Unable to read Ada file '{}': {}", inputFile, e.getMessage());
    }
  }

  private static AdaAnalysisConfig analysisConfig(SensorContext context) {
    Set<String> activeRuleKeys = new HashSet<>();
    Map<String, String> parameters = new HashMap<>();

    for (AdaRule rule : AdaRule.values()) {
      ActiveRule activeRule = context.activeRules().find(RuleKey.of(AdaRulesDefinition.REPOSITORY_KEY, rule.key()));
      if (activeRule != null) {
        activeRuleKeys.add(rule.key());
        for (AdaRuleParam param : rule.params()) {
          String value = activeRule.param(param.key());
          if (value != null && !value.isBlank()) {
            parameters.put(AdaAnalysisConfig.parameterKey(rule, param.key()), value);
          }
        }
      }
    }

    return new AdaAnalysisConfig(activeRuleKeys, parameters);
  }

  private static void saveMetrics(SensorContext context, InputFile inputFile, AdaMetrics metrics) {
    context.<Integer>newMeasure().on(inputFile).forMetric(CoreMetrics.LINES).withValue(metrics.lines()).save();
    context.<Integer>newMeasure().on(inputFile).forMetric(CoreMetrics.NCLOC).withValue(metrics.ncloc()).save();
    context.<Integer>newMeasure().on(inputFile).forMetric(CoreMetrics.COMMENT_LINES).withValue(metrics.commentLines()).save();
    context.<Integer>newMeasure().on(inputFile).forMetric(CoreMetrics.STATEMENTS).withValue(metrics.statements()).save();
    context.<Integer>newMeasure().on(inputFile).forMetric(CoreMetrics.FUNCTIONS).withValue(metrics.functions()).save();
    context.<Integer>newMeasure().on(inputFile).forMetric(CoreMetrics.COMPLEXITY).withValue(metrics.complexity()).save();
  }

  private static void saveIssues(SensorContext context, InputFile inputFile, AdaAnalysis analysis) {
    for (AdaIssue issue : analysis.issues()) {
      NewIssue newIssue = context.newIssue()
        .forRule(RuleKey.of(AdaRulesDefinition.REPOSITORY_KEY, issue.ruleKey()));
      NewIssueLocation location = newIssue.newLocation()
        .on(inputFile)
        .message(issue.message());

      try {
        int line = clampLine(inputFile, issue.line());
        int start = Math.max(0, issue.startOffset());
        int end = Math.max(start + 1, issue.endOffset());
        location.at(inputFile.newRange(line, start, line, end));
      } catch (RuntimeException invalidRange) {
        location.at(inputFile.selectLine(clampLine(inputFile, issue.line())));
      }

      newIssue.at(location).save();
    }
  }

  private static void saveHighlighting(SensorContext context, InputFile inputFile, AdaAnalysis analysis) {
    NewHighlighting highlighting = context.newHighlighting().onFile(inputFile);
    for (AdaHighlight highlight : analysis.highlights()) {
      try {
        highlighting.highlight(
          highlight.line(),
          highlight.startOffset(),
          highlight.line(),
          highlight.endOffset(),
          toTypeOfText(highlight.kind())
        );
      } catch (RuntimeException invalidRange) {
        LOG.debug("Ignoring invalid Ada highlighting range in {} at line {}", inputFile, highlight.line());
      }
    }
    highlighting.save();
  }

  private static void saveCpdTokens(SensorContext context, InputFile inputFile, AdaAnalysis analysis) {
    NewCpdTokens cpdTokens = context.newCpdTokens().onFile(inputFile);
    for (AdaCpdToken token : analysis.cpdTokens()) {
      try {
        cpdTokens.addToken(token.line(), token.startOffset(), token.line(), token.endOffset(), token.image());
      } catch (RuntimeException invalidRange) {
        LOG.debug("Ignoring invalid Ada CPD token in {} at line {}", inputFile, token.line());
      }
    }
    cpdTokens.save();
  }

  private static TypeOfText toTypeOfText(AdaHighlightKind kind) {
    return switch (kind) {
      case COMMENT -> TypeOfText.COMMENT;
      case STRING -> TypeOfText.STRING;
      case KEYWORD -> TypeOfText.KEYWORD;
      case CONSTANT -> TypeOfText.CONSTANT;
      case PRAGMA -> TypeOfText.ANNOTATION;
    };
  }

  private static int clampLine(InputFile inputFile, int line) {
    return Math.max(1, Math.min(Math.max(1, inputFile.lines()), line));
  }
}
