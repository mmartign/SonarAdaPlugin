package com.spazioit.sonarada.analysis;

public record AdaMetrics(
  int lines,
  int ncloc,
  int commentLines,
  int statements,
  int functions,
  int complexity
) {
}
