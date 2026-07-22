/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.spazioit.sonarada.gnattest;

import com.spazioit.sonarada.AdaLanguage;
import com.spazioit.sonarada.AdaProperties;
import java.io.IOException;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.measures.CoreMetrics;

public final class GnattestSensor implements Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(GnattestSensor.class);
  private final GnattestReportParser parser = new GnattestReportParser();

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .name("GNATtest Sensor")
      .onlyOnLanguage(AdaLanguage.KEY)
      .onlyWhenConfiguration(configuration -> configuration.hasKey(AdaProperties.GNATTEST_REPORT_PATHS_KEY));
  }

  @Override
  public void execute(SensorContext context) {
    GnattestConfiguration configuration = new GnattestConfiguration(context.config(), context.fileSystem());
    GnattestResults total = GnattestResults.empty();
    int importedReports = 0;

    for (Path report : configuration.reportPaths()) {
      try {
        total = total.plus(parser.parse(report));
        importedReports++;
      } catch (IOException | ArithmeticException e) {
        String message = "Unable to import GNATtest report '" + report + "': " + e.getMessage();
        if (configuration.failOnError()) {
          throw new IllegalStateException(message, e);
        }
        LOG.warn(message);
      }
    }

    if (importedReports == 0) {
      return;
    }

    context.<Integer>newMeasure().on(context.project()).forMetric(CoreMetrics.TESTS).withValue(total.tests()).save();
    context.<Integer>newMeasure().on(context.project()).forMetric(CoreMetrics.TEST_FAILURES).withValue(total.failures()).save();
    context.<Integer>newMeasure().on(context.project()).forMetric(CoreMetrics.TEST_ERRORS).withValue(total.errors()).save();
    context.<Integer>newMeasure().on(context.project()).forMetric(CoreMetrics.SKIPPED_TESTS).withValue(total.skipped()).save();
    context.<Long>newMeasure().on(context.project()).forMetric(CoreMetrics.TEST_EXECUTION_TIME).withValue(total.durationMillis()).save();

    LOG.info(
      "GNATtest import completed: {} report(s), {} test(s), {} failure(s), {} error(s), {} skipped",
      importedReports, total.tests(), total.failures(), total.errors(), total.skipped());
  }
}
