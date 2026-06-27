package com.spazioit.sonarada.adacontrol;

import com.spazioit.sonarada.AdaProperties;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.config.Configuration;

final class AdaControlConfiguration {

  private final Configuration configuration;
  private final FileSystem fileSystem;
  private final AdaControlArgumentParser argumentParser = new AdaControlArgumentParser();

  AdaControlConfiguration(Configuration configuration, FileSystem fileSystem) {
    this.configuration = configuration;
    this.fileSystem = fileSystem;
  }

  boolean enabled() {
    return configuration.getBoolean(AdaProperties.ADACONTROL_ENABLED_KEY).orElse(false);
  }

  String executable() {
    return configuration.get(AdaProperties.ADACONTROL_EXECUTABLE_KEY).filter(value -> !value.isBlank()).orElse("adactl");
  }

  java.util.Optional<Path> rulesFile() {
    return optionalPath(AdaProperties.ADACONTROL_RULES_FILE_KEY);
  }

  java.util.Optional<Path> projectFile() {
    return optionalPath(AdaProperties.ADACONTROL_PROJECT_FILE_KEY);
  }

  List<String> extraArguments() {
    return argumentParser.parse(configuration.get(AdaProperties.ADACONTROL_EXTRA_ARGS_KEY).orElse(""));
  }

  List<Path> reportPaths() {
    return splitCommaSeparated(configuration.get(AdaProperties.ADACONTROL_REPORT_PATHS_KEY).orElse(""))
      .stream()
      .map(this::resolvePath)
      .toList();
  }

  int timeoutSeconds() {
    return Math.max(1, configuration.getInt(AdaProperties.ADACONTROL_TIMEOUT_SECONDS_KEY).orElse(300));
  }

  boolean failOnError() {
    return configuration.getBoolean(AdaProperties.ADACONTROL_FAIL_ON_ERROR_KEY).orElse(true);
  }

  private java.util.Optional<Path> optionalPath(String key) {
    return configuration.get(key)
      .map(String::trim)
      .filter(value -> !value.isEmpty())
      .map(this::resolvePath);
  }

  private Path resolvePath(String path) {
    Path candidate = Path.of(path);
    if (candidate.isAbsolute()) {
      return candidate.normalize();
    }
    return fileSystem.baseDir().toPath().resolve(candidate).normalize();
  }

  private static List<String> splitCommaSeparated(String value) {
    if (value.isBlank()) {
      return List.of();
    }
    return Arrays.stream(value.split(","))
      .map(String::trim)
      .filter(item -> !item.isEmpty())
      .toList();
  }
}
