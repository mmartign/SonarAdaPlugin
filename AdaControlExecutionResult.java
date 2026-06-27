package com.spazioit.sonarada.adacontrol;

import java.nio.file.Path;

public record AdaControlExecutionResult(
  int exitCode,
  Path reportPath,
  String consoleOutput,
  boolean timedOut
) {
}
